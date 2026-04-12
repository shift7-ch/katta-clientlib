/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;
import java.util.UUID;

import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.MemberDto;
import cloud.katta.client.model.VaultDto;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.HubVaultKeys;
import cloud.katta.crypto.uvf.UVFAccessTokenPayload;
import cloud.katta.crypto.uvf.UVFMetadataPayload;
import cloud.katta.crypto.uvf.VaultMetadataAutomaticAccessGrantDto;
import cloud.katta.protocols.hub.HubVaultMetadataUVFProvider;
import com.nimbusds.jose.JWEObjectJSON;

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
        final VaultService vaultServiceMock = mock(VaultService.class);
        final WoTService wotServiceMock = mock(WoTService.class);
        final UUID vaultId = UUID.randomUUID();

        final UserKeys aliceKeys = UserKeys.create();
        final UserKeys bobKeys = UserKeys.create();
        final MemberDto bob = new MemberDto()
                .id(UUID.randomUUID().toString())
                .ecdhPublicKey(bobKeys.encodedEcdhPublicKey())
                .ecdsaPublicKey(bobKeys.encodedEcdsaPublicKey());

        when(vaults.apiVaultsVaultIdGet(vaultId)).thenReturn(new VaultDto().id(vaultId));
        when(vaults.apiVaultsVaultIdUsersRequiringAccessGrantGet(vaultId)).thenReturn(Collections.singletonList(bob));
        final HubVaultKeys vaultKeys = HubVaultKeys.create();
        when(vaultServiceMock.getVaultAccessToken(vaultId, aliceKeys)).thenReturn(new UVFAccessTokenPayload(vaultKeys.memberKey()));
        when(vaultServiceMock.getVaultMetadata(vaultId)).thenReturn(
                JWEObjectJSON.parse(new HubVaultMetadataUVFProvider(new UVFMetadataPayload()
                        .withAutomaticAccessGrant(new VaultMetadataAutomaticAccessGrantDto().enabled(automaticAccessGrantEnabled).maxWotDepth(maxWotDepth)),
                        "apiUrl", vaultId, vaultKeys.serialize()).encrypt()));
        when(wotServiceMock.getTrustLevelsPerUserId(aliceKeys)).thenReturn(Collections.singletonMap(bob.getId(), bobTrustLevel));

        final GrantAccessServiceImpl grantAccessService = new GrantAccessServiceImpl(vaults, vaultServiceMock, wotServiceMock);
        grantAccessService.grantAccessToUsersRequiringAccessGrant(vaultId, aliceKeys);
        verify(vaults, times(expectedNumberOfUploads)).apiVaultsVaultIdAccessTokensPost(eq(vaultId), any());
    }
}
