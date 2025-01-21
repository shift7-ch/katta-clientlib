/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.cyberduck.core.Host;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallback;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;

public interface UserKeysService {
    /**
     * Get user key from hub and decrypt with device-keys
     */
    UserKeys getUserKeys(Host hub, FirstLoginDeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure;
}
