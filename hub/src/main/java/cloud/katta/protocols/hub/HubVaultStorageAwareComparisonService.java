/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.features.Vault;
import ch.cyberduck.core.synchronization.Comparison;
import ch.cyberduck.core.synchronization.ComparisonService;
import ch.cyberduck.core.vault.VaultUnlockCancelException;

public class HubVaultStorageAwareComparisonService implements ComparisonService {

    private final HubSession session;

    public HubVaultStorageAwareComparisonService(final HubSession session) {
        this.session = session;
    }

    @Override
    public Comparison compare(final Path.Type type, final PathAttributes local, final PathAttributes remote) {
        try {
            final ComparisonService feature = this.getFeature(remote.getVault());
            return feature.compare(type, local, remote);
        }
        catch(VaultUnlockCancelException e) {
            return Comparison.unknown;
        }
    }

    @Override
    public int hashCode(final Path.Type type, final PathAttributes attr) {
        try {
            final ComparisonService feature = this.getFeature(attr.getVault());
            return feature.hashCode(type, attr);
        }
        catch(VaultUnlockCancelException e) {
            return 0;
        }
    }

    private ComparisonService getFeature(final Path vault) throws VaultUnlockCancelException {
        if(null == vault) {
            return ComparisonService.disabled;
        }
        final Vault impl = session.getRegistry().find(session, vault);
        if(impl instanceof HubUVFVault) {
            final HubUVFVault cryptomator = (HubUVFVault) impl;
            return cryptomator.getStorage().getFeature(ComparisonService.class);
        }
        // Disabled
        return ComparisonService.disabled;
    }
}
