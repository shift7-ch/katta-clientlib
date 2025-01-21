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
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.nimbusds.jose.JOSEException;

public class WoTServiceImpl implements WoTService {

    protected final UsersResourceApi usersApi;

    public WoTServiceImpl(final HubSession hubSession) {
        this(new UsersResourceApi(hubSession.getClient()));
    }

    public WoTServiceImpl(final UsersResourceApi users) {
        this.usersApi = users;
    }

    @Override
    public Map<String, Integer> getTrustLevelsPerUserId(final UserKeys userKeys) throws ApiException, AccessException, SecurityFailure {
        return this.getTrustLevels(userKeys).entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getTrustedUserId(), Map.Entry::getValue));
    }

    protected Map<TrustedUserDto, Integer> getTrustLevels(final UserKeys userKeys) throws ApiException, AccessException, SecurityFailure {
        final ECPublicKey signerPublicKey = userKeys.ecdsaKeyPair().getPublic();

        // 1. From the perspective of the currently logged-in user, GET a list of trusted users from /api/users/trusted
        final List<TrustedUserDto> trusts = usersApi.apiUsersTrustedGet();
        final List<UserDto> users = usersApi.apiUsersGet();

        // 2. Verify all returned signature chains
        return WoT.verifyTrusts(trusts, users, signerPublicKey);
    }

    @Override
    public void verify(final UserKeys userKeys, final List<String> signatureChain, final SignedKeys allegedSignedKey) throws ApiException, AccessException, SecurityFailure {
        WoT.verifyRecursive(signatureChain, userKeys.ecdsaKeyPair().getPublic(), allegedSignedKey);
    }

    @Override
    public TrustedUserDto sign(final UserKeys userKeys, final UserDto user) throws ApiException, ParseException, JOSEException, AccessException, SecurityFailure {
        final String signature = WoT.sign(userKeys.ecdsaKeyPair().getPrivate(),
                usersApi.apiUsersMeGet(true).getId(), user);
        usersApi.apiUsersTrustedUserIdPut(user.getId(), signature);
        return usersApi.apiUsersTrustedUserIdGet(user.getId());
    }
}
