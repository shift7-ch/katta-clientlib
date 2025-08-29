/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.DisabledPasswordCallback;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.vault.DefaultVaultRegistry;

public class HubVaultRegistry extends DefaultVaultRegistry {

    public HubVaultRegistry() {
        super(new DisabledPasswordCallback());
    }

    @Override
    public <T> T getFeature(final Session<?> session, final Class<T> type, final T proxy) {
        // Always forward to load feature from vault
        return this._getFeature(session, type, proxy);
    }
}
