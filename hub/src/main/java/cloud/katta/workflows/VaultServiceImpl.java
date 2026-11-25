/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.UVFAccessTokenPayload;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.workflows.exceptions.SecurityFailure;

public class VaultServiceImpl implements VaultService {

    private final VaultResourceApi vaultResource;

    public VaultServiceImpl(final HubSession hubSession) {
        this(new VaultResourceApi(hubSession.getClient()));
    }

    public VaultServiceImpl(final VaultResourceApi vaultResource) {
        this.vaultResource = vaultResource;
    }

    @Override
    public UVFAccessTokenPayload getVaultAccessToken(final UUID vaultId, final UserKeys userKeys) throws ApiException, SecurityFailure {
        // Get the user-specific vault key with private user key
        return userKeys.decryptAccessToken(vaultResource.apiVaultsVaultIdAccessTokenGet(vaultId, false));
    }

    @Override
    public String getVaultMetadata(final UUID vaultId) throws ApiException {
        return vaultResource.apiVaultsVaultIdUvfVaultUvfGet(vaultId);
    }
}
