/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.crypto.UserKeys;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;

public interface GrantAccessService {
    void grantAccessToUsersRequiringAccessGrant(UUID vaultId, UserKeys userKeys) throws ApiException, AccessException, SecurityFailure;
}
