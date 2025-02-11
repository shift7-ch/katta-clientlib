/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.cyberduck.core.Host;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallback;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;

public interface GrantAccessService {
    void grantAccessToUsersRequiringAccessGrant(Host hub, FirstLoginDeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure;
}
