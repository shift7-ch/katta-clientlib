/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.Path;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.cryptomator.impl.uvf.UVFVault;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.UnsupportedException;
import ch.cyberduck.core.features.AttributesFinder;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.shared.DefaultAttributesFinderFeature;
import ch.cyberduck.core.shared.DefaultFindFeature;
import ch.cyberduck.core.vault.VaultException;
import ch.cyberduck.core.vault.VaultMetadataProvider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

/**
 * Unified vault format (UVF) implementation for Katta
 */
public class HubUVFVault extends UVFVault {
    private static final Logger log = LogManager.getLogger(HubUVFVault.class);

    /**
     * Storage connection only available after loading vault
     */
    private final Session<?> storage;

    /**
     * Contents of vault.uvf from Hub. Null when creating a vault
     */
    @Nullable
    private final String metadata;

    /**
     * Vault to create
     *
     * @param storage Storage connection
     * @param bucket  Vault UVF metadata
     */
    public HubUVFVault(final Session<?> storage, final Path bucket) {
        this(storage, bucket, null);
    }

    /**
     * Vault to load
     *
     * @param storage  Storage connection
     * @param bucket   Vault UVF metadata
     * @param metadata Contents of vault.uvf from Hub
     */
    public HubUVFVault(final Session<?> storage, final Path bucket, @Nullable final String metadata) {
        super(bucket);
        this.storage = storage;
        this.metadata = metadata;
    }

    @Override
    public <T> T getFeature(final Session<?> hub, final Class<T> type, final T delegate) throws UnsupportedException {
        log.debug("Delegate to storage backend for feature {} in vault {}", type, this.getHome());
        // Ignore feature implementation but delegate to storage backend
        T feature = null;
        if(type == AttributesFinder.class) {
            if(delegate instanceof DefaultAttributesFinderFeature) {
                feature = (T) new DefaultAttributesFinderFeature(storage);
            }
        }
        if(type == Find.class) {
            if(delegate instanceof DefaultFindFeature) {
                feature = (T) new DefaultFindFeature(storage);
            }
        }
        if(null == feature) {
            feature = storage._getFeature(type);
        }
        if(null == feature) {
            log.warn("No feature {} available for storage backend", type);
            throw new UnsupportedException();
        }
        final T impl = super.getFeature(storage, type, feature);
        if(type == Delete.class) {
            return (T) new HubVaultDeleteFeature((Delete) impl);
        }
        return impl;
    }

    @Override
    public synchronized void close() {
        try {
            log.debug("Close storage backend connection for vault {}", this.getHome());
            storage.close();
        }
        catch(BackgroundException e) {
            //
        }
        super.close();
    }

    @Override
    public void create(final Session<?> session, final String region, final VaultMetadataProvider metadata) throws BackgroundException {
        // Upload vault template to storage
        log.debug("Upload vault template to storage backend for vault {}", this.getHome());
        super.create(storage, region, metadata);
    }

    /**
     *
     * @param session  Hub Connection
     * @param metadata metadata Return user keys
     */
    @Override
    public void load(final Session<?> session, final VaultMetadataProvider metadata) throws BackgroundException {
        log.debug("Initialize vault {}", this.getHome());
        // Initialize cryptors
        super.load(storage, metadata);
    }

    /**
     * The canonical copy of vault.uvf is in Hub. The copy in the bucket is only written when the vault is created.
     *
     * @param session Storage connection
     * @return Contents of vault.uvf from Hub
     */
    @Override
    protected byte[] readMetadata(final Session<?> session) throws BackgroundException {
        if(null == metadata) {
            throw new VaultException(String.format("Missing vault metadata from Hub for vault %s", this.getHome().getName()));
        }
        return metadata.getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HubUVFVault{");
        sb.append("storage=").append(storage);
        sb.append('}');
        return sb.toString();
    }
}
