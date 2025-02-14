/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.cyberduck.core.Host;

import ch.iterate.hub.core.FirstLoginDeviceSetupCallback;
import ch.iterate.hub.crypto.DeviceKeys;
import ch.iterate.hub.workflows.exceptions.AccessException;

public interface DeviceKeysService {

    /**
     * Retrieve saved device keys or create and save when missing
     *
     * @param hub Identification for server instance
     * @return Device keys
     * @throws AccessException   Failure accessing storage or not found
     * @throws SecurityException Failure decoding device keys retrieved from storage
     */
    DeviceKeys getOrCreateDeviceKeys(Host hub, FirstLoginDeviceSetupCallback setup) throws AccessException;

    /**
     * Retrieve saved device keys
     *
     * @param hub Identification for server instance
     * @return Device keys
     * @throws AccessException   Failure accessing storage or not found
     * @throws SecurityException Failure decoding device keys retrieved from storage
     */
    DeviceKeys getDeviceKeys(Host hub) throws AccessException, SecurityException;
}
