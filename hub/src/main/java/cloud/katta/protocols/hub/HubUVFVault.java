/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostUrlProvider;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.cryptomator.ContentWriter;
import ch.cyberduck.core.cryptomator.UVFVault;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.exception.UnsupportedException;
import ch.cyberduck.core.features.AttributesFinder;
import ch.cyberduck.core.features.Directory;
import ch.cyberduck.core.features.Write;
import ch.cyberduck.core.preferences.HostPreferencesFactory;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.proxy.ProxyFactory;
import ch.cyberduck.core.s3.S3Session;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.vault.VaultCredentials;
import ch.cyberduck.core.vault.VaultUnlockCancelException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.UserDto;
import cloud.katta.client.model.VaultDto;
import cloud.katta.core.DeviceSetupCallback;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.UvfMetadataPayload;
import cloud.katta.crypto.uvf.UvfMetadataPayloadPasswordCallback;
import cloud.katta.protocols.hub.exceptions.HubExceptionMappingService;
import cloud.katta.protocols.s3.S3AssumeRoleProtocol;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;

/**
 * Unified vault format (UVF) implementation for Katta
 */
public class HubUVFVault extends UVFVault {
    private static final Logger log = LogManager.getLogger(HubUVFVault.class);

    private final UUID vaultId;
    private final UvfMetadataPayload vaultMetadata;

    /**
     * Storage connection only available after loading vault
     */
    private final Session<?> storage;
    private final LoginCallback login;

    /**
     *
     * @param storage       Storage connection
     * @param vaultId       Vault Id
     * @param vaultMetadata Vault UVF metadata
     * @param prompt        Login prompt to access storage
     */
    public HubUVFVault(final Session<?> storage, final UUID vaultId, final UvfMetadataPayload vaultMetadata, final LoginCallback prompt) {
        super(new Path(vaultMetadata.storage().getDefaultPath(), EnumSet.of(Path.Type.directory, Path.Type.volume),
                new PathAttributes().setDisplayname(vaultMetadata.storage().getNickname())));
        this.storage = storage;
        this.vaultId = vaultId;
        this.vaultMetadata = vaultMetadata;
        this.login = prompt;
    }

    /**
     *
     * @return Storage provider configuration
     */
    public Session<?> getStorage() {
        return storage;
    }

    @Override
    public <T> T getFeature(final Session<?> hub, final Class<T> type, final T delegate) throws UnsupportedException {
        log.debug("Delegate to {} for feature {}", storage, type);
        // Ignore feature implementation but delegate to storage backend
        final T feature = storage._getFeature(type);
        if(null == feature) {
            log.warn("No feature {} available for {}", type, storage);
            throw new UnsupportedException();
        }
        return super.getFeature(storage, type, feature);
    }

    @Override
    public synchronized void close() {
        try {
            log.debug("Close connection {}", storage);
            storage.close();
        }
        catch(BackgroundException e) {
            //
        }
        super.close();
    }

    @Override
    public Path create(final Session<?> session, final String region, final VaultCredentials noop) throws BackgroundException {
        try {
            final HubSession hub = HubSession.coerce(session);
            log.debug("Created metadata JWE {}", vaultMetadata);
            final UvfMetadataPayload.UniversalVaultFormatJWKS jwks = UvfMetadataPayload.createKeys();
            final VaultDto vaultDto = new VaultDto()
                    .id(vaultId)
                    .name(vaultMetadata.storage().getNickname())
                    .description(null)
                    .archived(false)
                    .creationTime(DateTime.now())
                    .uvfMetadataFile(vaultMetadata.encrypt(
                            String.format("%s/api", new HostUrlProvider(false, true).get(session.getHost())),
                            vaultId,
                            jwks.toJWKSet()
                    ))
                    .uvfKeySet(jwks.serializePublicRecoverykey());
            // Create vault in Hub
            final VaultResourceApi vaultResourceApi = new VaultResourceApi(hub.getClient());
            log.debug("Create vault {}", vaultDto);
            vaultResourceApi.apiVaultsVaultIdPut(vaultDto.getId(), vaultDto,
                    storage.getHost().getProtocol().isRoleConfigurable() && !S3Session.isAwsHostname(storage.getHost().getHostname()),
                    storage.getHost().getProtocol().isRoleConfigurable() && S3Session.isAwsHostname(storage.getHost().getHostname()));
            // Upload JWE
            log.debug("Grant access to vault {}", vaultDto);
            final UserDto userDto = hub.getMe();
            final DeviceSetupCallback setup = login.getFeature(DeviceSetupCallback.class);
            final UserKeys userKeys = hub.getUserKeys(setup);
            vaultResourceApi.apiVaultsVaultIdAccessTokensPost(vaultDto.getId(),
                    Collections.singletonMap(userDto.getId(), jwks.toOwnerAccessToken().encryptForUser(userKeys.ecdhKeyPair().getPublic())));
            // Upload vault template to storage
            log.debug("Connect to {}", storage);
            final Host configuration = storage.getHost();
            // No token exchange with Katta Server
            configuration.setProperty(S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE, null);
            // Assume role with policy attached to create vault
            configuration.setProperty(S3AssumeRoleProtocol.S3_ASSUMEROLE_ROLEARN_WEBIDENTITY,
                    HostPreferencesFactory.get(storage.getHost()).getProperty(S3AssumeRoleProtocol.S3_ASSUMEROLE_ROLEARN_CREATE_BUCKET));
            // No role chaining when creating vault
            configuration.setProperty(S3AssumeRoleProtocol.S3_ASSUMEROLE_ROLEARN_TAG, null);
            storage.open(ProxyFactory.get(), new DisabledHostKeyCallback(), login, new DisabledCancelCallback());
            final Path vault;
            if(false) {
                log.debug("Upload vault template to {}", storage);
                return super.create(storage,
                        HubStorageLocationService.StorageLocation.fromIdentifier(region).getRegion(), noop);
            }
            else { // Obsolete when implemented in super
                final Directory<?> directory = (Directory<?>) storage._getFeature(Directory.class);
                final Path home = this.getHome();
                log.debug("Create vault root directory at {}", home);
                final TransferStatus status = (new TransferStatus()).setRegion(HubStorageLocationService.StorageLocation.fromIdentifier(region).getRegion());
                vault = directory.mkdir(storage._getFeature(Write.class), home, status);

                final String hashedRootDirId = vaultMetadata.computeRootDirIdHash();
                final Path dataDir = new Path(vault, "d", EnumSet.of(Path.Type.directory));
                final Path firstLevel = new Path(dataDir, hashedRootDirId.substring(0, 2), EnumSet.of(Path.Type.directory));
                final Path secondLevel = new Path(firstLevel, hashedRootDirId.substring(2), EnumSet.of(Path.Type.directory));

                directory.mkdir(storage._getFeature(Write.class), dataDir, status);
                directory.mkdir(storage._getFeature(Write.class), firstLevel, status);
                directory.mkdir(storage._getFeature(Write.class), secondLevel, status);

                // vault.uvf
                new ContentWriter(storage).write(new Path(home, PreferencesFactory.get().getProperty("cryptomator.vault.config.filename"),
                        EnumSet.of(Path.Type.file, Path.Type.vault)), vaultDto.getUvfMetadataFile().getBytes(StandardCharsets.US_ASCII));
                // dir.uvf
                new ContentWriter(storage).write(new Path(secondLevel, "dir.uvf", EnumSet.of(Path.Type.file)),
                        vaultMetadata.computeRootDirUvf());
            }
            return vault;
        }
        catch(JOSEException | JsonProcessingException e) {
            throw new InteroperabilityException(e.getMessage(), e);
        }
        catch(ApiException e) {
            throw new HubExceptionMappingService().map(e);
        }
    }

    /**
     *
     * @param session Hub Connection
     * @param prompt  Return user keys
     * @return Vault configuration with storage connection
     */
    @Override
    public HubUVFVault load(final Session<?> session, final PasswordCallback prompt) throws BackgroundException {
        try {
            log.debug("Connect to {}", storage);
            try {
                storage.open(ProxyFactory.get(), new DisabledHostKeyCallback(), login, new DisabledCancelCallback());
            }
            catch(BackgroundException e) {
                log.warn("Skip loading vault with failure {} connecting to storage", e.toString());
                throw new VaultUnlockCancelException(this, e);
            }
            final Path home = this.getHome();
            home.setAttributes(storage.getFeature(AttributesFinder.class).find(home)
                    .setDisplayname(vaultMetadata.storage().getNickname()));
            log.debug("Initialize vault {} with metadata {}", this, vaultMetadata);
            // Initialize cryptors
            super.load(storage, new UvfMetadataPayloadPasswordCallback(vaultMetadata.toJSON()));
            return this;
        }
        catch(JsonProcessingException e) {
            throw new InteroperabilityException(e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HubUVFVault{");
        sb.append("vaultId=").append(vaultId);
        sb.append(", vaultMetadata=").append(vaultMetadata);
        sb.append(", storage=").append(storage);
        sb.append('}');
        return sb.toString();
    }
}
