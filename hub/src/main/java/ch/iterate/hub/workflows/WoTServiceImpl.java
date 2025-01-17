/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.UsersResourceApi;
import ch.iterate.hub.client.model.TrustedUserDto;
import ch.iterate.hub.client.model.UserDto;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.crypto.wot.SignedKeys;
import ch.iterate.hub.crypto.wot.WoT;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.FirstLoginDeviceSetupException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.nimbusds.jose.JOSEException;

public class WoTServiceImpl implements WoTService {
    protected final UsersResourceApi usersApi;
    protected final UserKeysService userKeysService;

    public WoTServiceImpl(final UsersResourceApi users, final UserKeysService userKeysService) {
        this.usersApi = users;
        this.userKeysService = userKeysService;
    }

    @Override
    public Map<TrustedUserDto, Integer> getTrustLevels() throws ApiException, FirstLoginDeviceSetupException, AccessException, SecurityFailure {
        ECPublicKey signerPublicKey = getMyUserKeys().ecdsaKeyPair().getPublic();

        // 1. From the perspective of the currently logged-in user, GET a list of trusted users from /api/users/trusted
        final List<TrustedUserDto> trusts = usersApi.apiUsersTrustedGet();
        final List<UserDto> users = usersApi.apiUsersGet();

        // 2. Verify all returned signature chains
        return WoT.verifyTrusts(trusts, users, signerPublicKey);
    }

    protected UserKeys getMyUserKeys() throws ApiException, AccessException, SecurityFailure, FirstLoginDeviceSetupException {
        return userKeysService.getUserKeys();
    }

    protected UserDto getMe() throws ApiException {
        return usersApi.apiUsersMeGet(true);
    }


    @Override
    public Map<String, Integer> getTrustLevelsPerUserId() throws ApiException, FirstLoginDeviceSetupException, AccessException, SecurityFailure {
        return getTrustLevels().entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getTrustedUserId(), Map.Entry::getValue));
    }


    @Override
    public void verify(final List<String> signatureChain, final SignedKeys allegedSignedKey) throws FirstLoginDeviceSetupException, ApiException, AccessException, SecurityFailure {
        WoT.verifyRecursive(signatureChain, getMyUserKeys().ecdsaKeyPair().getPublic(), allegedSignedKey);
    }


    @Override
    public TrustedUserDto sign(final UserDto user) throws ApiException, ParseException, JOSEException, FirstLoginDeviceSetupException, AccessException, SecurityFailure {
        final String signature = WoT.sign(getMyUserKeys().ecdsaKeyPair().getPrivate(), getMe().getId(), user);
        usersApi.apiUsersTrustedUserIdPut(user.getId(), signature);
        return usersApi.apiUsersTrustedUserIdGet(user.getId());
    }
}
