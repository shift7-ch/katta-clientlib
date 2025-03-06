/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import ch.cyberduck.core.Host;

import cloud.katta.client.ApiException;
import cloud.katta.client.model.UserDto;
import cloud.katta.core.DeviceSetupCallback;
import cloud.katta.crypto.DeviceKeys;
import cloud.katta.crypto.UserKeys;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;

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
    public UserKeys getOrCreateUserKeys(final Host hub, final UserDto me, final DeviceKeys deviceKeyPair, final DeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure {
        if(userKeys == null) {
            userKeys = proxy.getOrCreateUserKeys(hub, me, deviceKeyPair, prompt);
        }
        return userKeys;
    }
}
