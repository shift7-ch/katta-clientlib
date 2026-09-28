/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import java.util.Set;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.crypto.UserKeys;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;

public interface GrantAccessService {
    /**
     * Grant access to all users requiring an access grant on any vault the current user holds an access token for, i.e.
     * can decrypt and therefore re-share. Does not require the {@link cloud.katta.client.model.Role#OWNER} role.
     * <p>
     * Failures for a single vault are logged and skipped, processing continues with the next vault.
     */
    void grantAccessToUsersRequiringAccessGrant(UserKeys userKeys) throws ApiException, AccessException, SecurityFailure;

    /**
     * Grant access to all users requiring an access grant on the given vault.
     */
    void grantAccessToUsersRequiringAccessGrant(UUID vaultId, UserKeys userKeys) throws ApiException, AccessException, SecurityFailure;

    /**
     * Grant access to the given candidates on the given vault, subject to the vault's own automatic access grant policy
     * and the Web of Trust.
     *
     * @param candidateUserIds IDs of users awaiting an access grant for this vault
     */
    void grantAccessToUsersRequiringAccessGrant(UUID vaultId, Set<String> candidateUserIds, UserKeys userKeys) throws ApiException, AccessException, SecurityFailure;
}
