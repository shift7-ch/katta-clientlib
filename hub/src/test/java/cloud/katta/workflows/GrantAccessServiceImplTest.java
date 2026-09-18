/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.AuthorityResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.AuthorityDto;
import cloud.katta.client.model.UserDto;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.HubVaultKeys;
import cloud.katta.crypto.uvf.UVFAccessTokenPayload;
import cloud.katta.crypto.uvf.UVFMetadataPayload;
import cloud.katta.crypto.uvf.VaultMetadataAutomaticAccessGrantDto;
import cloud.katta.protocols.hub.HubVaultMetadataUVFProvider;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.nimbusds.jose.JWEObjectJSON;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GrantAccessServiceImplTest {

    @ParameterizedTest
    @CsvSource({
            "false,-1,2,0", // automatic access grant disabled -> no upload
            "true,-1,2,1",  // negative maxWotDepth value -> 1 upload
            "true,3,2,1",   // maxWotDepth > bobTrustLevel -> 1 upload
            "true,2,2,1",   // maxWotDepth == bobTrustLevel -> 1 upload
            "true,1,2,0",   // maxWotDepth < bobTrustLevel -> no upload
    })
    void testGrantAccess(final boolean automaticAccessGrantEnabled, final int maxWotDepth, final int bobTrustLevel, final int expectedNumberOfUploads) throws Exception {
        final VaultResourceApi vaults = mock(VaultResourceApi.class);
        final AuthorityResourceApi authorities = mock(AuthorityResourceApi.class);
        final VaultService vaultServiceMock = mock(VaultService.class);
        final WoTService wotServiceMock = mock(WoTService.class);
        final UUID vaultId = UUID.randomUUID();

        final UserKeys aliceKeys = UserKeys.create();
        final UserKeys bobKeys = UserKeys.create();
        final UserDto bob = user(bobKeys);

        final HubVaultKeys vaultKeys = HubVaultKeys.create();
        when(vaults.apiVaultsUsersRequiringAccessGrantGet(0)).thenReturn(Collections.singletonMap(vaultId.toString(), Collections.singleton(bob.getId())));
        when(authorities.apiAuthoritiesGet(any())).thenReturn(Collections.singletonList(new AuthorityDto(bob)));
        when(vaultServiceMock.getVaultAccessToken(vaultId, aliceKeys)).thenReturn(new UVFAccessTokenPayload(vaultKeys.memberKey()));
        when(vaultServiceMock.getVaultMetadata(vaultId)).thenReturn(metadata(vaultId, vaultKeys, automaticAccessGrantEnabled, maxWotDepth));
        when(wotServiceMock.getTrustLevelsPerUserId(eq(aliceKeys), any())).thenReturn(Collections.singletonMap(bob.getId(), bobTrustLevel));

        final GrantAccessServiceImpl grantAccessService = new GrantAccessServiceImpl(vaults, authorities, vaultServiceMock, wotServiceMock);
        grantAccessService.grantAccessToUsersRequiringAccessGrant(vaultId, aliceKeys);
        verify(vaults, times(expectedNumberOfUploads)).apiVaultsVaultIdAccessTokensAutoPost(eq(vaultId), any());
        verify(vaults, never()).apiVaultsVaultIdAccessTokensPost(eq(vaultId), any());
    }

    @Test
    void testGrantAccessAllVaultsWithSingleRequest() throws Exception {
        final VaultResourceApi vaults = mock(VaultResourceApi.class);
        final AuthorityResourceApi authorities = mock(AuthorityResourceApi.class);
        final VaultService vaultServiceMock = mock(VaultService.class);
        final WoTService wotServiceMock = mock(WoTService.class);
        final UUID firstVaultId = UUID.randomUUID();
        final UUID secondVaultId = UUID.randomUUID();

        final UserKeys aliceKeys = UserKeys.create();
        final UserDto bob = user(UserKeys.create());

        final Map<String, Set<String>> pending = new HashMap<>();
        pending.put(firstVaultId.toString(), Collections.singleton(bob.getId()));
        pending.put(secondVaultId.toString(), Collections.singleton(bob.getId()));
        when(vaults.apiVaultsUsersRequiringAccessGrantGet(0)).thenReturn(pending);
        when(authorities.apiAuthoritiesGet(any())).thenReturn(Collections.singletonList(new AuthorityDto(bob)));
        for(final UUID vaultId : Arrays.asList(firstVaultId, secondVaultId)) {
            final HubVaultKeys vaultKeys = HubVaultKeys.create();
            when(vaultServiceMock.getVaultAccessToken(vaultId, aliceKeys)).thenReturn(new UVFAccessTokenPayload(vaultKeys.memberKey()));
            when(vaultServiceMock.getVaultMetadata(vaultId)).thenReturn(metadata(vaultId, vaultKeys, true, -1));
        }

        new GrantAccessServiceImpl(vaults, authorities, vaultServiceMock, wotServiceMock).grantAccessToUsersRequiringAccessGrant(aliceKeys);

        verify(vaults, times(1)).apiVaultsUsersRequiringAccessGrantGet(0);
        verify(vaults, never()).apiVaultsVaultIdUsersRequiringAccessGrantGet(any());
        verify(vaults).apiVaultsVaultIdAccessTokensAutoPost(eq(firstVaultId), any());
        verify(vaults).apiVaultsVaultIdAccessTokensAutoPost(eq(secondVaultId), any());
    }

    @Test
    void testGrantAccessContinuesAfterFailingVault() throws Exception {
        final VaultResourceApi vaults = mock(VaultResourceApi.class);
        final AuthorityResourceApi authorities = mock(AuthorityResourceApi.class);
        final VaultService vaultServiceMock = mock(VaultService.class);
        final WoTService wotServiceMock = mock(WoTService.class);
        final UUID failingVaultId = UUID.randomUUID();
        final UUID vaultId = UUID.randomUUID();

        final UserKeys aliceKeys = UserKeys.create();
        final UserDto bob = user(UserKeys.create());

        final Map<String, Set<String>> pending = new HashMap<>();
        pending.put(failingVaultId.toString(), Collections.singleton(bob.getId()));
        pending.put(vaultId.toString(), Collections.singleton(bob.getId()));
        when(vaults.apiVaultsUsersRequiringAccessGrantGet(0)).thenReturn(pending);
        when(authorities.apiAuthoritiesGet(any())).thenReturn(Collections.singletonList(new AuthorityDto(bob)));
        when(vaultServiceMock.getVaultAccessToken(failingVaultId, aliceKeys)).thenThrow(new ApiException(403, "not a vault member"));
        final HubVaultKeys vaultKeys = HubVaultKeys.create();
        when(vaultServiceMock.getVaultAccessToken(vaultId, aliceKeys)).thenReturn(new UVFAccessTokenPayload(vaultKeys.memberKey()));
        when(vaultServiceMock.getVaultMetadata(vaultId)).thenReturn(metadata(vaultId, vaultKeys, true, -1));

        new GrantAccessServiceImpl(vaults, authorities, vaultServiceMock, wotServiceMock).grantAccessToUsersRequiringAccessGrant(aliceKeys);

        verify(vaults, never()).apiVaultsVaultIdAccessTokensAutoPost(eq(failingVaultId), any());
        verify(vaults).apiVaultsVaultIdAccessTokensAutoPost(eq(vaultId), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGrantAccessNeverSharesRecoveryKey() throws Exception {
        final VaultResourceApi vaults = mock(VaultResourceApi.class);
        final AuthorityResourceApi authorities = mock(AuthorityResourceApi.class);
        final VaultService vaultServiceMock = mock(VaultService.class);
        final WoTService wotServiceMock = mock(WoTService.class);
        final UUID vaultId = UUID.randomUUID();

        final UserKeys aliceKeys = UserKeys.create();
        final UserKeys bobKeys = UserKeys.create();
        final UserDto bob = user(bobKeys);

        final HubVaultKeys vaultKeys = HubVaultKeys.create();
        when(vaults.apiVaultsUsersRequiringAccessGrantGet(0)).thenReturn(Collections.singletonMap(vaultId.toString(), Collections.singleton(bob.getId())));
        when(authorities.apiAuthoritiesGet(any())).thenReturn(Collections.singletonList(new AuthorityDto(bob)));
        // Alice is owner, i.e. her own access token carries the vault's private recovery key
        when(vaultServiceMock.getVaultAccessToken(vaultId, aliceKeys)).thenReturn(new UVFAccessTokenPayload(vaultKeys.memberKey(), vaultKeys.recoveryKey()));
        when(vaultServiceMock.getVaultMetadata(vaultId)).thenReturn(metadata(vaultId, vaultKeys, true, -1));

        new GrantAccessServiceImpl(vaults, authorities, vaultServiceMock, wotServiceMock).grantAccessToUsersRequiringAccessGrant(vaultId, aliceKeys);

        final ArgumentCaptor<Map<String, String>> tokens = ArgumentCaptor.forClass(Map.class);
        verify(vaults).apiVaultsVaultIdAccessTokensAutoPost(eq(vaultId), tokens.capture());
        final UVFAccessTokenPayload granted = bobKeys.decryptAccessToken(tokens.getValue().get(bob.getId()));
        assertEquals(new UVFAccessTokenPayload(vaultKeys.memberKey()).key(), granted.key());
        assertNull(granted.recoveryKey());
    }

    @Test
    void testGrantAccessEncryptsForVerifiedKey() throws Exception {
        final VaultResourceApi vaults = mock(VaultResourceApi.class);
        final AuthorityResourceApi authorities = mock(AuthorityResourceApi.class);
        final VaultService vaultServiceMock = mock(VaultService.class);
        final WoTService wotServiceMock = mock(WoTService.class);
        final UUID vaultId = UUID.randomUUID();

        final UserKeys aliceKeys = UserKeys.create();
        final UserKeys bobKeys = UserKeys.create();
        final UserDto bob = user(bobKeys);

        final HubVaultKeys vaultKeys = HubVaultKeys.create();
        when(vaults.apiVaultsUsersRequiringAccessGrantGet(0)).thenReturn(Collections.singletonMap(vaultId.toString(), Collections.singleton(bob.getId())));
        when(authorities.apiAuthoritiesGet(any())).thenReturn(Collections.singletonList(new AuthorityDto(bob)));
        when(vaultServiceMock.getVaultAccessToken(vaultId, aliceKeys)).thenReturn(new UVFAccessTokenPayload(vaultKeys.memberKey()));
        when(vaultServiceMock.getVaultMetadata(vaultId)).thenReturn(metadata(vaultId, vaultKeys, true, 1));
        when(wotServiceMock.getTrustLevelsPerUserId(eq(aliceKeys), any())).thenReturn(Collections.singletonMap(bob.getId(), 1));

        new GrantAccessServiceImpl(vaults, authorities, vaultServiceMock, wotServiceMock).grantAccessToUsersRequiringAccessGrant(vaultId, aliceKeys);

        // The Web of Trust is handed the very users the keys are taken from, so the server cannot substitute a key
        final ArgumentCaptor<java.util.List<UserDto>> verified = ArgumentCaptor.forClass(java.util.List.class);
        verify(wotServiceMock).getTrustLevelsPerUserId(eq(aliceKeys), verified.capture());
        assertEquals(Collections.singletonList(bob.getEcdhPublicKey()),
                verified.getValue().stream().map(UserDto::getEcdhPublicKey).collect(java.util.stream.Collectors.toList()));
    }

    private static UserDto user(final UserKeys keys) throws SecurityFailure, AccessException {
        return new UserDto()
                .id(UUID.randomUUID().toString())
                .ecdhPublicKey(keys.encodedEcdhPublicKey())
                .ecdsaPublicKey(keys.encodedEcdsaPublicKey());
    }

    private static JWEObjectJSON metadata(final UUID vaultId, final HubVaultKeys vaultKeys, final boolean enabled, final int trustThreshold) throws Exception {
        return JWEObjectJSON.parse(new HubVaultMetadataUVFProvider(new UVFMetadataPayload()
                .withAutomaticAccessGrant(new VaultMetadataAutomaticAccessGrantDto().enabled(enabled).trustThreshold(trustThreshold)),
                "apiUrl", vaultId, vaultKeys.serialize()).encrypt());
    }
}
