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

public interface UserKeysService {

    /**
     * Get user keys from hub and decrypt with device keys
     *
     * @throws ApiException    Server error response
     * @throws SecurityFailure Failure decoding keys
     * @throws AccessException Failure accessing device keys
     */
    UserKeys getUserKeys(Host hub, UserDto me, DeviceKeys deviceKeyPair) throws ApiException, AccessException, SecurityFailure;

    /**
     * Get or create user keys and decrypt with existing or newly created device keys
     *
     * @throws ApiException    Server error response
     * @throws SecurityFailure Failure decoding keys
     * @throws AccessException Failure accessing or saving device keys
     */
    UserKeys getOrCreateUserKeys(Host hub, UserDto me, DeviceKeys deviceKeyPair, FirstLoginDeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure;
}
