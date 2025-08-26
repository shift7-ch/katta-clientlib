/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.DefaultPathContainerService;
import ch.cyberduck.core.DisabledPasswordCallback;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.features.Vault;
import ch.cyberduck.core.vault.DefaultVaultRegistry;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HubVaultRegistry extends DefaultVaultRegistry {
    private static final Logger log = LogManager.getLogger(HubSession.class);

    public HubVaultRegistry() {
        super(new DisabledPasswordCallback());
    }

    @Override
    public Vault find(final Session session, final Path file, final boolean unlock) {
        for(final Vault vault : this) {
            if(StringUtils.equals(new DefaultPathContainerService().getContainer(file).getName(),
                    new DefaultPathContainerService().getContainer(vault.getHome()).getName())) {
                // Return matching vault
                log.debug("Found vault {} for file {}", vault, file);
                return vault;
            }
        }
        log.warn("No vault found for file {}", file);
        return Vault.DISABLED;
    }

    @Override
    public <T> T getFeature(Session<?> session, Class<T> type, T proxy) {
        // Always forward to load feature from vault
        return this._getFeature(session, type, proxy);
    }
}
