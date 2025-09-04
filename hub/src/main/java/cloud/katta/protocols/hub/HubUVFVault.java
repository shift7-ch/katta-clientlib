/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DisabledCancelCallback;
import ch.cyberduck.core.DisabledHostKeyCallback;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.SessionFactory;
import ch.cyberduck.core.cryptomator.UVFVault;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.exception.UnsupportedException;
import ch.cyberduck.core.features.AttributesFinder;
import ch.cyberduck.core.proxy.ProxyFactory;
import ch.cyberduck.core.ssl.X509KeyManager;
import ch.cyberduck.core.ssl.X509TrustManager;
import ch.cyberduck.core.vault.VaultUnlockCancelException;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumSet;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.crypto.uvf.UvfMetadataPayload;
import cloud.katta.crypto.uvf.UvfMetadataPayloadPasswordCallback;
import cloud.katta.crypto.uvf.VaultMetadataJWEBackendDto;
import cloud.katta.protocols.hub.exceptions.HubExceptionMappingService;
import cloud.katta.workflows.VaultServiceImpl;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;

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

    /**
     * Constructor for factory creating new vault
     *
     * @param home Bucket
     */
    public HubUVFVault(final Path home) {
        super(home);
        this.vaultId = UUID.fromString(home.getName());
    }

    /**
     * Open from existing metadata
     *
     * @param vaultId Vault ID Used to lookup profile
     * @param bucket  Bucket name
     */
    public HubUVFVault(final UUID vaultId, final String bucket) {
        super(new Path(bucket, EnumSet.of(Path.Type.directory, Path.Type.volume)));
        this.vaultId = vaultId;
    }

    public Session<?> getStorage() {
        return storage;
    }

    @Override
    public <T> T getFeature(final Session<?> ignore, final Class<T> type, final T delegate) throws UnsupportedException {
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
            final Protocol profile = ProtocolFactory.get().forName(vaultId.toString());
            log.debug("Loaded profile {} for vault {}", profile, this);
            final Credentials credentials = new Credentials(hub.getHost().getCredentials());
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
            storage.open(ProxyFactory.get(), new DisabledHostKeyCallback(), new DisabledLoginCallback(), new DisabledCancelCallback());
            storage.login(new DisabledLoginCallback(), new DisabledCancelCallback());
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
