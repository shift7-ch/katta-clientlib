/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import cloud.katta.client.ApiException;
import cloud.katta.client.api.AuthorityResourceApi;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.model.AuthorityDto;
import cloud.katta.client.model.TrustedUserDto;
import cloud.katta.client.model.UserDto;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.wot.SignedKeys;
import cloud.katta.crypto.wot.WoT;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.nimbusds.jose.JOSEException;

public class WoTServiceImpl implements WoTService {

    protected final UsersResourceApi usersApi;
    protected final AuthorityResourceApi authorityApi;

    public WoTServiceImpl(final HubSession hubSession) {
        this(new UsersResourceApi(hubSession.getClient()), new AuthorityResourceApi(hubSession.getClient()));
    }

    public WoTServiceImpl(final UsersResourceApi users) {
        this.usersApi = users;
        this.authorityApi = new AuthorityResourceApi(usersApi.getApiClient());
    }

    public WoTServiceImpl(final UsersResourceApi users, final AuthorityResourceApi authorities) {
        this.usersApi = users;
        this.authorityApi = authorities;
    }

    @Override
    public Map<String, Integer> getTrustLevelsPerUserId(final UserKeys userKeys) throws ApiException, AccessException, SecurityFailure {
        return this.getTrustLevels(userKeys).entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getTrustedUserId(), Map.Entry::getValue));
    }

    protected Map<TrustedUserDto, Integer> getTrustLevels(final UserKeys userKeys) throws ApiException, AccessException, SecurityFailure {
        final ECPublicKey signerPublicKey = userKeys.ecdsaKeyPair().getPublic();

        // 1. From the perspective of the currently logged-in user, GET a list of trusted users from /api/users/trusted
        final List<TrustedUserDto> trusts = usersApi.apiUsersTrustedGet();
        if(trusts.isEmpty()) {
            return Collections.emptyMap();
        }

        final List<UserDto> users = authorityApi.apiAuthoritiesGet(trusts.stream().map(TrustedUserDto::getTrustedUserId).filter(Objects::nonNull).collect(Collectors.toList())).stream().map(AuthorityDto::getUserDto).collect(Collectors.toList());

        // 2. Verify all returned signature chains
        return WoT.verifyTrusts(trusts, users, signerPublicKey);
    }

    @Override
    public void verify(final UserKeys userKeys, final List<String> signatureChain, final SignedKeys allegedSignedKey) throws ApiException, AccessException, SecurityFailure {
        WoT.verifyRecursive(signatureChain, userKeys.ecdsaKeyPair().getPublic(), allegedSignedKey);
    }

    @Override
    public TrustedUserDto sign(final UserKeys userKeys, final UserDto user) throws ApiException, ParseException, JOSEException, AccessException, SecurityFailure {
        final String signature = WoT.sign(userKeys.ecdsaKeyPair().getPrivate(), user.getId(), user);
        usersApi.apiUsersTrustedUserIdPut(user.getId(), signature);
        return usersApi.apiUsersTrustedUserIdGet(user.getId());
    }
}
