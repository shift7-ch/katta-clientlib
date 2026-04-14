/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.VaultDto;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.HubVaultKeys;
import cloud.katta.crypto.uvf.UVFAccessTokenPayload;
import cloud.katta.workflows.exceptions.SecurityFailure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class VaultServiceImplTest {

    @Test
    void testGetVaultOwnerAccessTokenJWE() throws Exception {
        final VaultResourceApi vaultResourceMock = Mockito.mock(VaultResourceApi.class);
        final VaultService service = new VaultServiceImpl(vaultResourceMock);

        final UserKeys userKeys = UserKeys.create();
        final HubVaultKeys jwks = HubVaultKeys.create();
        final String accessToken = new UVFAccessTokenPayload(jwks.memberKey(), jwks.recoveryKey()).encryptForUser(userKeys.ecdhKeyPair().getPublic());

        final UUID vaultId = UUID.randomUUID();
        when(vaultResourceMock.apiVaultsVaultIdGet(vaultId)).thenReturn(new VaultDto().id(vaultId));
        when(vaultResourceMock.apiVaultsVaultIdAccessTokenGet(eq(vaultId), any())).thenReturn(accessToken);

        final UVFAccessTokenPayload payload = service.getVaultAccessToken(vaultId, userKeys);
        assertEquals(new UVFAccessTokenPayload(jwks.memberKey(), jwks.recoveryKey()), payload);
    }

    @Test
    void testGetVaultAccessToken() throws Exception {
        final VaultResourceApi vaultResourceMock = Mockito.mock(VaultResourceApi.class);
        final VaultService service = new VaultServiceImpl(vaultResourceMock);

        final UserKeys userKeys = UserKeys.create();
        final HubVaultKeys jwks = HubVaultKeys.create();
        final String accessToken = new UVFAccessTokenPayload(jwks.memberKey()).encryptForUser(userKeys.ecdhKeyPair().getPublic());

        final UUID vaultId = UUID.randomUUID();
        when(vaultResourceMock.apiVaultsVaultIdGet(vaultId)).thenReturn(new VaultDto().id(vaultId));
        when(vaultResourceMock.apiVaultsVaultIdAccessTokenGet(eq(vaultId), any())).thenReturn(accessToken);

        final UVFAccessTokenPayload payload = service.getVaultAccessToken(vaultId, userKeys);
        assertEquals(new UVFAccessTokenPayload(jwks.memberKey()), payload);
    }

    @Test
    void testGetVaultWrongAccessTokenJWE() throws Exception {
        final VaultResourceApi vaultResourceMock = Mockito.mock(VaultResourceApi.class);
        final VaultService service = new VaultServiceImpl(vaultResourceMock);

        final UserKeys userKeys = UserKeys.create();
        final HubVaultKeys jwks = HubVaultKeys.create();
        new UVFAccessTokenPayload(jwks.memberKey()).encryptForUser(userKeys.ecdhKeyPair().getPublic());

        final UUID vaultId = UUID.randomUUID();
        when(vaultResourceMock.apiVaultsVaultIdGet(vaultId)).thenReturn(new VaultDto().id(vaultId));
        when(vaultResourceMock.apiVaultsVaultIdAccessTokenGet(eq(vaultId), any())).thenReturn("lkajsdflkadsj");

        assertThrows(SecurityFailure.class, () -> service.getVaultAccessToken(vaultId, userKeys));
    }
}
