/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
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

// TODO https://github.com/shift7-ch/katta-server/issues/4 merge with FirstLoginDeviceSetupService ?
public class UserKeysServiceImpl implements UserKeysService {
    protected final HubSession hubSession;

    public UserKeysServiceImpl(final HubSession hubSession) {
        this.hubSession = hubSession;
    }

    @Override
    public UserKeys getUserKeys() throws ApiException, AccessException, SecurityFailure {
        // Get user key from hub and decrypt with device-keys
        return new FirstLoginDeviceSetupService(hubSession).getUserKeysWithDeviceKeys();
    }


    @Override
    public UvfMetadataPayload getVaultMetadataJWE(final UUID vaultId) throws ApiException, SecurityFailure, AccessException {
        // contains vault member key
        final UvfAccessTokenPayload accessToken;
        accessToken = getVaultAccessTokenJWE(vaultId);
        final VaultResourceApi vaultResource = new VaultResourceApi(this.hubSession.getClient());
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
    public UvfAccessTokenPayload getVaultAccessTokenJWE(final UUID vaultId) throws ApiException, AccessException, SecurityFailure {
        // Get the user-specific vault key with private user key
        return getVaultAccessTokenJWE(vaultId, getUserKeys());
    }

    @Override
    public UvfAccessTokenPayload getVaultAccessTokenJWE(final UUID vaultId, final UserKeys privateUserKey) throws ApiException, SecurityFailure {
        final VaultResourceApi vaultResource = new VaultResourceApi(this.hubSession.getClient());
        final VaultDto vault = vaultResource.apiVaultsVaultIdGet(vaultId);
        final String userSpecificVaultJWE = vaultResource.apiVaultsVaultIdAccessTokenGet(vault.getId(), false);
        try {
            return privateUserKey.decryptAccessToken(userSpecificVaultJWE);
        }
        catch(ParseException | JOSEException | JsonProcessingException e) {
            throw new SecurityFailure(e);
        }
    }
}
