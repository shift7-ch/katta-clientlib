/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.cyberduck.core.Host;

import java.util.UUID;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallback;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.crypto.uvf.UvfAccessTokenPayload;
import ch.iterate.hub.crypto.uvf.UvfMetadataPayload;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;

/**
 * Retrieve user keys from hub upon first access and cache in memory during service's lifetime.
 */
public class CachingUserKeysService implements UserKeysService {

    private final UserKeysService proxy;
    private UserKeys userKeys;

    public CachingUserKeysService(final UserKeysService proxy) {
        this.proxy = proxy;
    }

    /**
     * Get user key from hub and decrypt with device-keys
     */
    public UserKeys getUserKeys(final Host hub, final FirstLoginDeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure {
        // Get user key from hub and decrypt with device-keys
        if(this.userKeys == null) {
            this.userKeys = proxy.getUserKeys(hub, prompt);
        }
        return this.userKeys;
    }

    @Override
    public UvfMetadataPayload getVaultMetadataJWE(final Host hub, final UUID vaultId, final FirstLoginDeviceSetupCallback prompt) throws ApiException, SecurityFailure, AccessException {
        return proxy.getVaultMetadataJWE(hub, vaultId, prompt);
    }

    @Override
    public UvfAccessTokenPayload getVaultAccessTokenJWE(final Host hub, final UUID vaultId, final FirstLoginDeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure {
        return proxy.getVaultAccessTokenJWE(hub, vaultId, prompt);
    }
}
