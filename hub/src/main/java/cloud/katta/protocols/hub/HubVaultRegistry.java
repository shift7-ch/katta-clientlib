/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.DisabledPasswordCallback;
import ch.cyberduck.core.PasswordCallback;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.features.Vault;
import ch.cyberduck.core.vault.DefaultVaultRegistry;

public class HubVaultRegistry extends DefaultVaultRegistry {

    public HubVaultRegistry() {
        this(new DisabledPasswordCallback());
    }

    public HubVaultRegistry(final PasswordCallback prompt) {
        super(prompt);
    }

    public HubVaultRegistry(final PasswordCallback prompt, final Vault... vaults) {
        super(prompt, vaults);
    }

    @Override
    public <T> T getFeature(final Session<?> session, final Class<T> type, final T proxy) {
        // Always forward to load feature from vault
        return this._getFeature(session, type, proxy);
    }
}
