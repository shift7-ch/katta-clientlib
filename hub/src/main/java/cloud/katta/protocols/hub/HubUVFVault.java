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
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.shared.DefaultAttributesFinderFeature;
import ch.cyberduck.core.shared.DefaultFindFeature;
import ch.cyberduck.core.vault.VaultMetadataProvider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
     *
     * @param storage Storage connection
     * @param bucket  Vault UVF metadata
     */
    public HubUVFVault(final Session<?> storage, final Path bucket) {
        super(bucket);
        this.storage = storage;
    }

    @Override
    public <T> T getFeature(final Session<?> hub, final Class<T> type, final T delegate) throws UnsupportedException {
        log.debug("Delegate to {} for feature {}", storage, type);
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
    public void create(final Session<?> session, final String region, final VaultMetadataProvider metadata) throws BackgroundException {
        // Upload vault template to storage
        log.debug("Upload vault template to {}", storage);
        super.create(storage, region, metadata);
    }

    /**
     *
     * @param session  Hub Connection
     * @param metadata metadata Return user keys
     */
    @Override
    public void load(final Session<?> session, final VaultMetadataProvider metadata) throws BackgroundException {
        log.debug("Initialize vault {}", this);
        // Initialize cryptors
        super.load(storage, metadata);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HubUVFVault{");
        sb.append("storage=").append(storage);
        sb.append('}');
        return sb.toString();
    }
}
