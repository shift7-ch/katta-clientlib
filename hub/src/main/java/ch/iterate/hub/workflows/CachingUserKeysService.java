/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;

/**
 * Retrieve user keys from hub upon first access and cache in memory during service's lifetime.
 */
public class CachingUserKeysService extends UserKeysServiceImpl {
    private UserKeys userKeys;

    public CachingUserKeysService(final HubSession hubSession) {
        super(hubSession);
    }

    /**
     * Get user key from hub and decrypt with device-keys
     */
    public UserKeys getUserKeys() throws ApiException, AccessException, SecurityFailure {
        // Get user key from hub and decrypt with device-keys
        if(this.userKeys == null) {
            this.userKeys = super.getUserKeys();
        }
        return this.userKeys;
    }

}
