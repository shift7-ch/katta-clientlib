/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import ch.cyberduck.core.Host;

import cloud.katta.client.model.UserDto;
import cloud.katta.core.DeviceSetupCallback;
import cloud.katta.crypto.DeviceKeys;
import cloud.katta.workflows.exceptions.SecurityFailure;

public interface DeviceKeysService {

    /**
     * Retrieve saved device keys or create and save when missing
     *
     * @param hub Identification for server instance
     * @param me
     * @return Device keys
     * @throws SecurityFailure Failure decoding device keys retrieved from storage
     */
    DeviceKeys getOrCreateDeviceKeys(Host hub, final UserDto me, DeviceSetupCallback setup) throws SecurityFailure;

    /**
     * Retrieve saved device keys
     *
     * @param hub Identification for server instance
     * @param me
     * @return Device keys
     * @throws SecurityFailure Failure decoding device keys retrieved from storage
     */
    DeviceKeys getDeviceKeys(Host hub, final UserDto me) throws SecurityFailure;
}
