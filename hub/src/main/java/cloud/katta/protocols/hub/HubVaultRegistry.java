/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.DisabledPasswordCallback;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.vault.DefaultVaultRegistry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HubVaultRegistry extends DefaultVaultRegistry {
    private static final Logger log = LogManager.getLogger(HubVaultRegistry.class);

    public HubVaultRegistry() {
        super(new DisabledPasswordCallback());
    }

    @Override
    public <T> T getFeature(Session<?> session, Class<T> type, T proxy) {
        // Always forward to load feature from vault
        return this._getFeature(session, type, proxy);
    }
}
