/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.cyberduck.core.Host;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.UUID;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.UsersResourceApi;
import ch.iterate.hub.client.api.VaultResourceApi;
import ch.iterate.hub.client.model.MemberDto;
import ch.iterate.hub.core.DisabledFirstLoginDeviceSetupCallback;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.crypto.uvf.UvfAccessTokenPayload;
import ch.iterate.hub.crypto.uvf.UvfMetadataPayload;
import ch.iterate.hub.crypto.uvf.VaultMetadataJWEAutomaticAccessGrantDto;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

class GrantAccessServiceImplTest {

    @ParameterizedTest
    @CsvSource({
            "false,-1,2,0", // automatic access grant disabled -> no upload
            "true,-1,2,0",  // invalid maxWotDepth value -> no upload
            "true,3,2,1",   // maxWotDepth > bobTrustLevel -> 1 upload
            "true,2,2,1",   // maxWotDepth == bobTrustLevel -> 1 upload
            "true,1,2,0",   // maxWotDepth < bobTrustLevel -> no upload
    })
    public void grantAccessToUsersRequiringAccessGrant(final boolean automaticAccessGrantEnabled, final int maxWotDepth, final int bobTrustLevel, final int expectedNumberOfUploads) throws ApiException, AccessException, SecurityFailure {
        final VaultResourceApi vaults = Mockito.mock(VaultResourceApi.class);
        final UsersResourceApi users = Mockito.mock(UsersResourceApi.class);
        final UserKeysService userKeysServiceMock = Mockito.mock(UserKeysService.class);
        final WoTService wotServiceMock = Mockito.mock(WoTService.class);
        final UUID vaultId = UUID.randomUUID();
        final Host hub = Mockito.mock(Host.class);

        final UserKeys aliceKeys = UserKeys.create();
        final UserKeys bobKeys = UserKeys.create();
        final MemberDto bob = new MemberDto()
                .id(UUID.randomUUID().toString())
                .ecdhPublicKey(bobKeys.encodedEcdhPublicKey())
                .ecdsaPublicKey(bobKeys.encodedEcdsaPublicKey());

        Mockito.when(vaults.apiVaultsVaultIdUsersRequiringAccessGrantGet(vaultId)).thenReturn(Collections.singletonList(bob));
        Mockito.when(userKeysServiceMock.getUserKeys(hub, new DisabledFirstLoginDeviceSetupCallback())).thenReturn(aliceKeys);
        Mockito.when(userKeysServiceMock.getVaultMetadataJWE(hub, vaultId, new DisabledFirstLoginDeviceSetupCallback())).thenReturn(new UvfMetadataPayload().withAutomaticAccessGrant(new VaultMetadataJWEAutomaticAccessGrantDto().enabled(automaticAccessGrantEnabled).maxWotDepth(maxWotDepth)));
        Mockito.when(userKeysServiceMock.getVaultAccessTokenJWE(hub, vaultId, new DisabledFirstLoginDeviceSetupCallback())).thenReturn(new UvfAccessTokenPayload());
        Mockito.when(wotServiceMock.getTrustLevelsPerUserId(userKeysServiceMock.getUserKeys(hub, new DisabledFirstLoginDeviceSetupCallback()))).thenReturn(Collections.singletonMap(bob.getId(), bobTrustLevel));

        final GrantAccessServiceImpl grantAccessService = new GrantAccessServiceImpl(vaults, users, userKeysServiceMock, wotServiceMock);
        grantAccessService.grantAccessToUsersRequiringAccessGrant(hub, vaultId, new DisabledFirstLoginDeviceSetupCallback());
        Mockito.verify(vaults, times(expectedNumberOfUploads)).apiVaultsVaultIdAccessTokensPost(eq(vaultId), any());
    }
}