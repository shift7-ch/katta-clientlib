/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.testsetup.model;

public class HubTestConfig {
    public final HubTestSetupConfig hubTestSetupConfig;
    public final VaultSpec vaultSpec;

    public HubTestConfig(final HubTestSetupConfig hubTestSetupConfig, final VaultSpec vaultSpec) {
        this.hubTestSetupConfig = hubTestSetupConfig;
        this.vaultSpec = vaultSpec;
    }
}