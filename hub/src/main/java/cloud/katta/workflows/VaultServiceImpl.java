/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.ParseException;
import java.util.Base64;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.VaultDto;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.UvfAccessTokenPayload;
import cloud.katta.crypto.uvf.UvfMetadataPayload;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.OctetSequenceKey;

import static cloud.katta.crypto.uvf.UvfMetadataPayload.UniversalVaultFormatJWKS.memberKeyFromRawKey;

public class VaultServiceImpl implements VaultService {
    private static final Logger log = LogManager.getLogger(VaultServiceImpl.class);

    private final VaultResourceApi vaultResource;
    private final StorageProfileResourceApi storageProfileResourceApi;

    public VaultServiceImpl(final HubSession hubSession) {
        this(new VaultResourceApi(hubSession.getClient()), new StorageProfileResourceApi(hubSession.getClient()));
    }

    public VaultServiceImpl(final VaultResourceApi vaultResource, final StorageProfileResourceApi storageProfileResourceApi) {
        this.vaultResource = vaultResource;
        this.storageProfileResourceApi = storageProfileResourceApi;
    }

    @Override
    public UvfMetadataPayload getVaultMetadataJWE(final UUID vaultId, final UserKeys userKeys) throws ApiException, SecurityFailure, AccessException {
        final VaultDto vault = vaultResource.apiVaultsVaultIdGet(vaultId);
        // contains vault member key
        final UvfAccessTokenPayload accessToken = this.getVaultAccessTokenJWE(vaultId, userKeys);
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
    public UvfAccessTokenPayload getVaultAccessTokenJWE(final UUID vaultId, final UserKeys userKeys) throws ApiException, SecurityFailure {
        // Get the user-specific vault key with private user key
        final String userSpecificVaultJWE = vaultResource.apiVaultsVaultIdAccessTokenGet(vaultId, false);
        try {
            return userKeys.decryptAccessToken(userSpecificVaultJWE);
        }
        catch(ParseException | JOSEException | JsonProcessingException e) {
            throw new SecurityFailure(e);
        }
    }

    @Override
    public StorageProfileDtoWrapper getVaultStorageProfile(final UvfMetadataPayload metadataPayload) throws ApiException {
        log.debug("Load profile {}", metadataPayload.storage().getProvider());
        return StorageProfileDtoWrapper.coerce(storageProfileResourceApi
                .apiStorageprofileProfileIdGet(UUID.fromString(metadataPayload.storage().getProvider())));
    }
}
