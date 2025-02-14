/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.cyberduck.core.Host;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.model.UserDto;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallback;
import ch.iterate.hub.crypto.DeviceKeys;
import ch.iterate.hub.crypto.UserKeys;
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
    public UserKeys getUserKeys(final Host hub, final UserDto me, final DeviceKeys deviceKeyPair) throws ApiException, AccessException, SecurityFailure {
        // Get user key from hub and decrypt with device-keys
        if(userKeys == null) {
            userKeys = proxy.getUserKeys(hub, me, deviceKeyPair);
        }
        return userKeys;
    }

    @Override
    public UserKeys getOrCreateUserKeys(final Host hub, final UserDto me, final DeviceKeys deviceKeyPair, final FirstLoginDeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure {
        if(userKeys == null) {
            userKeys = proxy.getOrCreateUserKeys(hub, me, deviceKeyPair, prompt);
        }
        return userKeys;
    }
}
