/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.*;
import ch.cyberduck.core.cryptomator.ContentWriter;
import ch.cyberduck.core.cryptomator.UVFVault;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.features.AttributesFinder;
import ch.cyberduck.core.features.Directory;
import ch.cyberduck.core.features.Write;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.proxy.ProxyFactory;
import ch.cyberduck.core.s3.S3Session;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.vault.VaultCredentials;
import ch.cyberduck.core.vault.VaultUnlockCancelException;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.UserDto;
import cloud.katta.client.model.VaultDto;
import cloud.katta.crypto.DeviceKeys;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.UvfMetadataPayload;
import cloud.katta.crypto.uvf.UvfMetadataPayloadPasswordCallback;
import cloud.katta.crypto.uvf.VaultMetadataJWEAutomaticAccessGrantDto;
import cloud.katta.crypto.uvf.VaultMetadataJWEBackendDto;
import cloud.katta.protocols.hub.exceptions.HubExceptionMappingService;
import cloud.katta.workflows.DeviceKeysServiceImpl;
import cloud.katta.workflows.UserKeysServiceImpl;
import cloud.katta.workflows.VaultServiceImpl;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;

import static cloud.katta.protocols.s3.S3AssumeRoleProtocol.OAUTH_TOKENEXCHANGE_VAULT;

/**
 * Unified vault format (UVF) implementation for Katta
 */
public class HubUVFVault extends UVFVault {
    private static final Logger log = LogManager.getLogger(HubUVFVault.class);

    private final UUID vaultId;

    /**
     * Storage connection only available after loading vault
     */
    private Session<?> storage;
    private Path home;

    public HubUVFVault(final Path home) {
        this(home, null, null, null);
    }

    /**
     * Constructor for factory creating new vault
     *
     * @param home Bucket
     */
    public HubUVFVault(final Path home, final String masterkey, final String config, final byte[] pepper) {
        super(home);
        this.home = home;
        this.vaultId = UUID.fromString(new UUIDRandomStringService().random());
    }

    /**
     * Open from existing metadata
     *
     * @param vaultId Vault ID Used to lookup profile
     * @param bucket  Bucket name
     */
    public HubUVFVault(final UUID vaultId, final Path bucket) {
        super(bucket);
        this.vaultId = vaultId;
    }

    public Session<?> getStorage() {
        return storage;
    }

    @Override
    public <T> T getFeature(final Session<?> hub, final Class<T> type, final T delegate) {
        log.debug("Delegate to {} for feature {}", storage, type);
        // Ignore feature implementation but delegate to storage backend
        final T feature = storage._getFeature(type);
        if(null == feature) {
            log.warn("No feature {} available for {}", type, storage);
            return null;
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
    public Path create(final Session<?> session, final String region, final VaultCredentials credentials) throws BackgroundException {
        try {
            final HubStorageLocationService.StorageLocation location = HubStorageLocationService.StorageLocation.fromIdentifier(region);
            final String storageProfileId = location.getProfile();
            final UvfMetadataPayload metadataPayload = UvfMetadataPayload.create()
                    .withStorage(new VaultMetadataJWEBackendDto()
                            .provider(storageProfileId)
                            .defaultPath(session.getFeature(PathContainerService.class).getContainer(home).getName())
                            .region(location.getRegion())
                            .nickname(null != home.attributes().getDisplayname() ? home.attributes().getDisplayname() : "Vault"))
                    .withAutomaticAccessGrant(new VaultMetadataJWEAutomaticAccessGrantDto()
                            .enabled(true)
                            .maxWotDepth(null));
            log.debug("Created metadata JWE {}", metadataPayload);
            final UvfMetadataPayload.UniversalVaultFormatJWKS jwks = UvfMetadataPayload.createKeys();
            final VaultDto vaultDto = new VaultDto()
                    .id(vaultId)
                    .name(metadataPayload.storage().getNickname())
                    .description(null)
                    .archived(false)
                    .creationTime(DateTime.now())
                    .uvfMetadataFile(metadataPayload.encrypt(
                            String.format("%s/api", new HostUrlProvider(false, true).get(session.getHost())),
                            vaultId,
                            jwks.toJWKSet()
                    ))
                    .uvfKeySet(jwks.serializePublicRecoverykey());
            final HubSession hub = (HubSession) session;
            // Create vault in Hub
            final VaultResourceApi vaultResourceApi = new VaultResourceApi(hub.getClient());
            log.debug("Create vault {}", vaultDto);
            vaultResourceApi.apiVaultsVaultIdPut(vaultDto.getId(), vaultDto,
                    !S3Session.isAwsHostname(session.getHost().getHostname()), S3Session.isAwsHostname(session.getHost().getHostname()));
            // Upload JWE
            log.debug("Grant access to vault {}", vaultDto);
            final UserDto userDto = new UsersResourceApi(hub.getClient()).apiUsersMeGet(false, false);
            final DeviceKeys deviceKeys = new DeviceKeysServiceImpl().getDeviceKeys(session.getHost());
            final UserKeys userKeys = new UserKeysServiceImpl(hub).getUserKeys(session.getHost(), hub.getMe(), deviceKeys);
            vaultResourceApi.apiVaultsVaultIdAccessTokensPost(vaultDto.getId(),
                    Collections.singletonMap(userDto.getId(), jwks.toOwnerAccessToken().encryptForUser(userKeys.ecdhKeyPair().getPublic())));
            // Upload vault template to storage
            final Protocol profile = ProtocolFactory.get().forName(storageProfileId);
            log.debug("Loaded profile {} for vault {}", profile, this);
            final Host bookmark = new Host(profile,
                    session.getFeature(CredentialsConfigurator.class).reload().configure(session.getHost()));
            bookmark.setProperty(OAUTH_TOKENEXCHANGE_VAULT, vaultId.toString());
            bookmark.setRegion(location.getRegion());
            log.debug("Configured {} for vault {}", bookmark, this);
            storage = SessionFactory.create(bookmark, session.getFeature(X509TrustManager.class), session.getFeature(X509KeyManager.class));
            log.debug("Connect to {}", storage);
            storage.open(ProxyFactory.get(), new DisabledHostKeyCallback(), new DisabledLoginCallback(), new DisabledCancelCallback());
            storage.login(new DisabledLoginCallback(), new DisabledCancelCallback());
            log.debug("Upload vault template to {}", storage);
            final Path vault;
            if(false) {
                return super.create(storage, region, credentials);
            }
            else { // Obsolete when implemented in super
                final Directory<?> directory = (Directory<?>) storage._getFeature(Directory.class);
                log.debug("Create vault root directory at {}", home);
                final TransferStatus status = (new TransferStatus()).setRegion(region);
                vault = directory.mkdir(storage._getFeature(Write.class), home, status);

                final String hashedRootDirId = metadataPayload.computeRootDirIdHash();
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
                        metadataPayload.computeRootDirUvf());
            }
            return vault;
        }
        catch(JOSEException | JsonProcessingException | AccessException | SecurityFailure e) {
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
            final HubSession hub = HubSession.coerce(session);
            // Find storage configuration in vault metadata
            final VaultServiceImpl vaultService = new VaultServiceImpl(hub);
            final UvfMetadataPayload vaultMetadata = vaultService.getVaultMetadataJWE(vaultId, hub.getUserKeys());
            final Protocol profile = ProtocolFactory.get().forName(vaultMetadata.storage().getProvider());
            log.debug("Loaded profile {} for vault {}", profile, this);
            final Credentials credentials =
                    session.getFeature(CredentialsConfigurator.class).reload().configure(session.getHost());
            log.debug("Copy credentials {}", credentials);
            final VaultMetadataJWEBackendDto vaultStorageMetadata = vaultMetadata.storage();
            if(vaultStorageMetadata.getUsername() != null) {
                credentials.setUsername(vaultStorageMetadata.getUsername());
            }
            if(vaultStorageMetadata.getPassword() != null) {
                credentials.setPassword(vaultStorageMetadata.getPassword());
            }
            final Host bookmark = new Host(profile, credentials);
            log.debug("Configure bookmark for vault {}", vaultStorageMetadata);
            bookmark.setNickname(vaultStorageMetadata.getNickname());
            bookmark.setDefaultPath(vaultStorageMetadata.getDefaultPath());
            bookmark.setProperty(OAUTH_TOKENEXCHANGE_VAULT, vaultId.toString());
            // region as chosen by user upon vault creation (STS) or as retrieved from bucket (permanent)
            bookmark.setRegion(vaultStorageMetadata.getRegion());
            log.debug("Configured {} for vault {}", bookmark, this);
            storage = SessionFactory.create(bookmark, session.getFeature(X509TrustManager.class), session.getFeature(X509KeyManager.class));
            log.debug("Connect to {}", storage);
            try {
                storage.open(ProxyFactory.get(), new DisabledHostKeyCallback(), new DisabledLoginCallback(), new DisabledCancelCallback());
                storage.login(new DisabledLoginCallback(), new DisabledCancelCallback());
            }
            catch(BackgroundException e) {
                log.warn("Skip loading vault with failure {} connecting to storage", e.toString());
                throw new VaultUnlockCancelException(this, e);
            }
            final PathAttributes attr = storage.getFeature(AttributesFinder.class).find(home);
            attr.setDisplayname(vaultMetadata.storage().getNickname());
            home.setAttributes(attr);
            log.debug("Initialize vault {} with metadata {}", this, vaultMetadata);
            // Initialize cryptors
            super.load(storage, new UvfMetadataPayloadPasswordCallback(vaultMetadata.toJSON()));
            return this;
        }
        catch(ApiException e) {
            if(HttpStatus.SC_FORBIDDEN == e.getCode()) {
                throw new VaultUnlockCancelException(this, e);
            }
            throw new HubExceptionMappingService().map(e);
        }
        catch(JsonProcessingException | SecurityFailure | AccessException e) {
            throw new InteroperabilityException(e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HubUVFVault{");
        sb.append("storage=").append(storage);
        sb.append(", vaultId=").append(vaultId);
        sb.append('}');
        return sb.toString();
    }
}
