/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.cyberduck.core.Host;

import java.text.ParseException;
import java.util.Base64;
import java.util.UUID;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.DeviceResourceApi;
import ch.iterate.hub.client.api.UsersResourceApi;
import ch.iterate.hub.client.api.VaultResourceApi;
import ch.iterate.hub.client.model.VaultDto;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallback;
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

    private final VaultResourceApi vaultResource;
    private final UsersResourceApi usersResourceApi;
    private final DeviceResourceApi deviceResourceApi;

    public UserKeysServiceImpl(final HubSession hubSession) {
        this(new VaultResourceApi(hubSession.getClient()), new UsersResourceApi(hubSession.getClient()), new DeviceResourceApi(hubSession.getClient()));
    }

    public UserKeysServiceImpl(final VaultResourceApi vaultResource, final UsersResourceApi usersResourceApi, final DeviceResourceApi deviceResourceApi) {
        this.vaultResource = vaultResource;
        this.usersResourceApi = usersResourceApi;
        this.deviceResourceApi = deviceResourceApi;
    }

    @Override
    public UserKeys getUserKeys(final Host hub, final FirstLoginDeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure {
        // Get user key from hub and decrypt with device-keys
        return new FirstLoginDeviceSetupService(usersResourceApi, deviceResourceApi).getUserKeysWithDeviceKeys(hub, prompt);
    }

    @Override
    public UvfMetadataPayload getVaultMetadataJWE(final Host hub, final UUID vaultId, final FirstLoginDeviceSetupCallback prompt) throws ApiException, SecurityFailure, AccessException {
        // contains vault member key
        final UvfAccessTokenPayload accessToken = this.getVaultAccessTokenJWE(hub, vaultId, prompt);
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
    public UvfAccessTokenPayload getVaultAccessTokenJWE(final Host hub, final UUID vaultId, final FirstLoginDeviceSetupCallback prompt) throws ApiException, AccessException, SecurityFailure {
        // Get the user-specific vault key with private user key
        return this.getVaultAccessTokenJWE(vaultId, this.getUserKeys(hub, prompt));
    }

    /**
     * Get the user-specific vault key with private user key.
     *
     * @param vaultId        vault ID
     * @param privateUserKey private user key
     * @return uvf metadata
     */
    private UvfAccessTokenPayload getVaultAccessTokenJWE(final UUID vaultId, final UserKeys privateUserKey) throws ApiException, SecurityFailure {
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
