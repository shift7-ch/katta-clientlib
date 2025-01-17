/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import java.util.Map;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.UsersResourceApi;
import ch.iterate.hub.client.model.TrustedUserDto;
import ch.iterate.hub.client.model.UserDto;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.FirstLoginDeviceSetupException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;

/**
 * Retrieve verified trusted user from hub upon first access and cache afterwards.
 * Counterpart of @see <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/wot.ts">wot.ts</a>.
 */
public class CachingWoTService extends WoTServiceImpl {

    private Map<TrustedUserDto, Integer> trustLevels;
    private UserKeys myUserKeys;
    private UserDto me;

    public CachingWoTService(final UsersResourceApi users, final UserKeysService userKeysService) {
        super(users, userKeysService);
    }

    @Override
    public Map<TrustedUserDto, Integer> getTrustLevels() throws ApiException, FirstLoginDeviceSetupException, AccessException, SecurityFailure {
        if(trustLevels == null) {
            trustLevels = super.getTrustLevels();
        }
        return trustLevels;
    }

    protected UserKeys getMyUserKeys() throws ApiException, AccessException, SecurityFailure, FirstLoginDeviceSetupException {
        if(myUserKeys == null) {
            myUserKeys = super.getMyUserKeys();
        }
        return myUserKeys;
    }

    protected UserDto getMe() throws ApiException {
        if(me == null) {
            me = super.getMe();
        }
        return me;
    }
}
