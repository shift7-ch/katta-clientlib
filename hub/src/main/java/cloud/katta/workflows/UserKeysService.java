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
    UserKeys getOrCreateUserKeys(Host hub, UserDto me, DeviceKeys deviceKeyPair, DeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure;
}
