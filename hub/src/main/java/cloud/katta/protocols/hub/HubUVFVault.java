/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.cryptomator.AbstractVault;
import ch.cyberduck.core.cryptomator.impl.uvf.CryptoVault;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.exception.UnsupportedException;
import ch.cyberduck.core.preferences.HostPreferencesFactory;
import ch.cyberduck.core.proxy.ProxyFactory;
import ch.cyberduck.core.s3.S3Session;
import ch.cyberduck.core.vault.VaultMetadataProvider;
import ch.cyberduck.core.vault.VaultUnlockCancelException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.UserDto;
import cloud.katta.client.model.VaultDto;
import cloud.katta.core.DeviceSetupCallback;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.VaultIdMetadataUVFProvider;
import cloud.katta.protocols.hub.exceptions.HubExceptionMappingService;
import cloud.katta.protocols.s3.S3AssumeRoleProtocol;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;

/**
 * Unified vault format (UVF) implementation for Katta
 */
public class HubUVFVault extends CryptoVault {
    private static final Logger log = LogManager.getLogger(HubUVFVault.class);

    /**
     * Storage connection only available after loading vault
     */
    private final Session<?> storage;
    private final LoginCallback login;

    /**
     *
     * @param storage Storage connection
     * @param bucket  Vault UVF metadata
     * @param prompt  Login prompt to access storage
     */
    public HubUVFVault(final Session<?> storage, final Path bucket, final LoginCallback prompt) {
        super(bucket);
        this.storage = storage;
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
    public AbstractVault create(final Session<?> session, final String region, final VaultMetadataProvider metadata) throws BackgroundException {
        try {
            final HubSession hub = HubSession.coerce(session);
            final Path home = this.getHome();
            final UUID vaultId = VaultIdMetadataUVFProvider.cast(metadata).getVaultId();
            final VaultDto vaultDto = new VaultDto()
                    .id(vaultId)
                    .name(home.attributes().getDisplayname())
                    .description(null)
                    .archived(false)
                    .creationTime(DateTime.now())
                    .uvfMetadataFile(new String(VaultIdMetadataUVFProvider.cast(metadata).getMetadata(), StandardCharsets.US_ASCII))
                    .uvfKeySet(VaultIdMetadataUVFProvider.cast(metadata).getJwks().serializePublicRecoveryKey());
            // Create vault in Hub
            final VaultResourceApi vaultResourceApi = new VaultResourceApi(hub.getClient());
            log.debug("Create vault {}", vaultDto);
            vaultResourceApi.apiVaultsVaultIdPut(vaultId, vaultDto,
                    storage.getHost().getProtocol().isRoleConfigurable() && !S3Session.isAwsHostname(storage.getHost().getHostname()),
                    storage.getHost().getProtocol().isRoleConfigurable() && S3Session.isAwsHostname(storage.getHost().getHostname()));
            // Upload JWE
            log.debug("Grant access to vault {}", vaultDto);
            final UserDto userDto = hub.getMe();
            final DeviceSetupCallback setup = login.getFeature(DeviceSetupCallback.class);
            final UserKeys userKeys = hub.getUserKeys(setup);
            vaultResourceApi.apiVaultsVaultIdAccessTokensPost(vaultId,
                    Collections.singletonMap(userDto.getId(), VaultIdMetadataUVFProvider.cast(metadata).getJwks().toOwnerAccessToken().encryptForUser(userKeys.ecdhKeyPair().getPublic())));
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
            log.debug("Upload vault template to {}", storage);
            return super.create(storage, HubStorageLocationService.StorageLocation.fromIdentifier(region).getRegion(), metadata);
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
     * @param metadata  metadata
     * @return Vault configuration with storage connection
     */
    @Override
    public HubUVFVault load(final Session<?> session, final PasswordCallback prompt, final VaultMetadataProvider metadata) throws BackgroundException {
        log.debug("Connect to {}", storage);
        try {
            storage.open(ProxyFactory.get(), new DisabledHostKeyCallback(), login, new DisabledCancelCallback());
        }
        catch(BackgroundException e) {
            log.warn("Skip loading vault with failure {} connecting to storage", e.toString());
            throw new VaultUnlockCancelException(this, e);
        }
        log.debug("Initialize vault {}", this);
        // Initialize cryptors
        super.load(storage, prompt, metadata);
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HubUVFVault{");
        sb.append("storage=").append(storage);
        sb.append('}');
        return sb.toString();
    }
}
