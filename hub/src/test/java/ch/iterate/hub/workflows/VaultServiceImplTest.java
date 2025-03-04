/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.VaultResourceApi;
import ch.iterate.hub.client.model.VaultDto;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.crypto.uvf.UvfAccessTokenPayload;
import ch.iterate.hub.crypto.uvf.UvfMetadataPayload;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class VaultServiceImplTest {
    @Test
    void testGetVaultOwnerAccessTokenJWE() throws JOSEException, JsonProcessingException, AccessException, SecurityFailure, ApiException {
        final VaultResourceApi vaultResourceMock = Mockito.mock(VaultResourceApi.class);
        final VaultService service = new VaultServiceImpl(vaultResourceMock);

        final UserKeys userKeys = UserKeys.create();
        final UvfMetadataPayload.UniversalVaultFormatJWKS jwks = UvfMetadataPayload.createKeys();
        final String accessToken = jwks.toOwnerAccessToken().encryptForUser(userKeys.ecdhKeyPair().getPublic());

        final UUID vaultId = UUID.randomUUID();
        when(vaultResourceMock.apiVaultsVaultIdGet(vaultId)).thenReturn(new VaultDto().id(vaultId));
        when(vaultResourceMock.apiVaultsVaultIdAccessTokenGet(eq(vaultId), any())).thenReturn(accessToken);

        final UvfAccessTokenPayload payload = service.getVaultAccessTokenJWE(vaultId, userKeys);
        assertEquals(jwks.toOwnerAccessToken(), payload);
    }

    @Test
    void testGetVaultAccessTokenJWE() throws JOSEException, JsonProcessingException, AccessException, SecurityFailure, ApiException {
        final VaultResourceApi vaultResourceMock = Mockito.mock(VaultResourceApi.class);
        final VaultService service = new VaultServiceImpl(vaultResourceMock);

        final UserKeys userKeys = UserKeys.create();
        final UvfMetadataPayload.UniversalVaultFormatJWKS jwks = UvfMetadataPayload.createKeys();
        final String accessToken = jwks.toAccessToken().encryptForUser(userKeys.ecdhKeyPair().getPublic());

        final UUID vaultId = UUID.randomUUID();
        when(vaultResourceMock.apiVaultsVaultIdGet(vaultId)).thenReturn(new VaultDto().id(vaultId));
        when(vaultResourceMock.apiVaultsVaultIdAccessTokenGet(eq(vaultId), any())).thenReturn(accessToken);

        final UvfAccessTokenPayload payload = service.getVaultAccessTokenJWE(vaultId, userKeys);
        assertEquals(jwks.toAccessToken(), payload);
    }

    @Test
    void testGetVaultWrongAccessTokenJWE() throws JOSEException, JsonProcessingException, ApiException {
        final VaultResourceApi vaultResourceMock = Mockito.mock(VaultResourceApi.class);
        final VaultService service = new VaultServiceImpl(vaultResourceMock);

        final UserKeys userKeys = UserKeys.create();
        final UvfMetadataPayload.UniversalVaultFormatJWKS jwks = UvfMetadataPayload.createKeys();
        jwks.toAccessToken().encryptForUser(userKeys.ecdhKeyPair().getPublic());

        final UUID vaultId = UUID.randomUUID();
        when(vaultResourceMock.apiVaultsVaultIdGet(vaultId)).thenReturn(new VaultDto().id(vaultId));
        when(vaultResourceMock.apiVaultsVaultIdAccessTokenGet(eq(vaultId), any())).thenReturn("lkajsdflkadsj");

        assertThrows(SecurityFailure.class, () -> service.getVaultAccessTokenJWE(vaultId, userKeys));
    }

    @Test
    void testGetVaultMetadataJWE() throws JOSEException, JsonProcessingException, ApiException, AccessException, SecurityFailure {
        final VaultResourceApi vaultResourceMock = Mockito.mock(VaultResourceApi.class);
        final VaultService service = new VaultServiceImpl(vaultResourceMock);

        final UserKeys userKeys = UserKeys.create();
        final UvfMetadataPayload.UniversalVaultFormatJWKS jwks = UvfMetadataPayload.createKeys();
        final String accessToken = jwks.toAccessToken().encryptForUser(userKeys.ecdhKeyPair().getPublic());

        final UUID vaultId = UUID.randomUUID();

        final UvfMetadataPayload metadataJWE = UvfMetadataPayload.create();
        final String uvfMetadataFile = metadataJWE.encrypt(
                "blabla",
                vaultId,
                jwks.toJWKSet()
        );
        when(vaultResourceMock.apiVaultsVaultIdGet(vaultId)).thenReturn(new VaultDto().id(vaultId).uvfMetadataFile(uvfMetadataFile));
        when(vaultResourceMock.apiVaultsVaultIdAccessTokenGet(eq(vaultId), any())).thenReturn(accessToken);
        when(vaultResourceMock.apiVaultsVaultIdAccessTokenGet(eq(vaultId), any())).thenReturn(accessToken);

        final UvfMetadataPayload payload = service.getVaultMetadataJWE(vaultId, userKeys);
        assertEquals(metadataJWE, payload);
    }

    @Test
    void testGetWrongVaultMetadataJWE() throws JOSEException, JsonProcessingException, ApiException {
        final VaultResourceApi vaultResourceMock = Mockito.mock(VaultResourceApi.class);
        final VaultService service = new VaultServiceImpl(vaultResourceMock);

        final UserKeys userKeys = UserKeys.create();
        final UvfMetadataPayload.UniversalVaultFormatJWKS jwks = UvfMetadataPayload.createKeys();
        final String accessToken = jwks.toAccessToken().encryptForUser(userKeys.ecdhKeyPair().getPublic());

        final UUID vaultId = UUID.randomUUID();

        when(vaultResourceMock.apiVaultsVaultIdGet(vaultId)).thenReturn(new VaultDto().id(vaultId).uvfMetadataFile("lkajsdflkjas"));
        when(vaultResourceMock.apiVaultsVaultIdAccessTokenGet(eq(vaultId), any())).thenReturn(accessToken);
        when(vaultResourceMock.apiVaultsVaultIdAccessTokenGet(eq(vaultId), any())).thenReturn(accessToken);

        assertThrows(SecurityFailure.class, () -> service.getVaultMetadataJWE(vaultId, userKeys));
    }
}
