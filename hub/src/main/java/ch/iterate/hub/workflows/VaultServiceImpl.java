/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import java.text.ParseException;
import java.util.Base64;
import java.util.UUID;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.VaultResourceApi;
import ch.iterate.hub.client.model.VaultDto;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.crypto.uvf.UvfAccessTokenPayload;
import ch.iterate.hub.crypto.uvf.UvfMetadataPayload;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.OctetSequenceKey;

import static ch.iterate.hub.crypto.uvf.UvfMetadataPayload.UniversalVaultFormatJWKS.memberKeyFromRawKey;

public class VaultServiceImpl implements VaultService {

    private final VaultResourceApi vaultResource;

    public VaultServiceImpl(final HubSession hubSession) {
        this(new VaultResourceApi(hubSession.getClient()));
    }

    public VaultServiceImpl(final VaultResourceApi vaultResource) {
        this.vaultResource = vaultResource;
    }

    @Override
    public UvfMetadataPayload getVaultMetadataJWE(final UUID vaultId, final UserKeys userKeys) throws ApiException, SecurityFailure, AccessException {
        // contains vault member key
        final UvfAccessTokenPayload accessToken = this.getVaultAccessTokenJWE(vaultId, userKeys);
        final VaultDto vault = vaultResource.apiVaultsVaultIdGet(vaultId);
        // extract and decode vault key
        final OctetSequenceKey rawMemberKey = memberKeyFromRawKey(Base64.getDecoder().decode(accessToken.key()));
        // decode vault metadata (incl. key material)
        try {
            return UvfMetadataPayload.decryptWithJWK(vault.getUvfMetadataFile(), rawMemberKey);
        }
        catch(ParseException | JsonProcessingException | JOSEException e) {
            throw new SecurityFailure(e);
        }
    }

    @Override
    public UvfAccessTokenPayload getVaultAccessTokenJWE(final UUID vaultId, final UserKeys privateUserKey) throws ApiException, SecurityFailure {
        // Get the user-specific vault key with private user key
        final String userSpecificVaultJWE = vaultResource.apiVaultsVaultIdAccessTokenGet(vaultId, false);
        try {
            return privateUserKey.decryptAccessToken(userSpecificVaultJWE);
        }
        catch(ParseException | JOSEException | JsonProcessingException e) {
            throw new SecurityFailure(e);
        }
    }
}
