/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import java.util.UUID;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;

public interface GrantAccessService {
    void grantAccessToUsersRequiringAccessGrant(UUID vaultId, UserKeys userKeys) throws ApiException, AccessException, SecurityFailure;
}
