/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.model.TrustedUserDto;
import ch.iterate.hub.client.model.UserDto;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.crypto.wot.SignedKeys;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.nimbusds.jose.JOSEException;

/**
 * Retrieve verified trusted user from hub upon first access and cache afterwards.
 * Counterpart of @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/wot.ts">wot.ts</a>.
 */
public class CachingWoTService implements WoTService {

    private final WoTService proxy;

    private Map<String, Integer> trustLevels;

    public CachingWoTService(final WoTService proxy) {
        this.proxy = proxy;
    }

    @Override
    public Map<String, Integer> getTrustLevelsPerUserId(final UserKeys userKeys) throws ApiException, AccessException, SecurityFailure {
        if(trustLevels == null) {
            trustLevels = proxy.getTrustLevelsPerUserId(userKeys);
        }
        return trustLevels;
    }

    @Override
    public void verify(final UserKeys userKeys, final List<String> signatureChain, final SignedKeys allegedSignedKey) throws ApiException, AccessException, SecurityFailure {
        proxy.verify(userKeys, signatureChain, allegedSignedKey);
    }

    @Override
    public TrustedUserDto sign(final UserKeys userKeys, final UserDto user) throws ApiException, ParseException, JOSEException, AccessException, SecurityFailure {
        return proxy.sign(userKeys, user);
    }
}
