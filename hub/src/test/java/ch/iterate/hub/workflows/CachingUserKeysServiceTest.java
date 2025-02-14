/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

class CachingUserKeysServiceTest {

    @Test
    void testGetUserKeys() throws AccessException, SecurityFailure, ApiException {
        final UserKeysService proxyMock = Mockito.mock(UserKeysService.class);
        final UserKeys userKeys = UserKeys.create();
        Mockito.when(proxyMock.getUserKeys(any(), any(), any())).thenReturn(userKeys);
        final CachingUserKeysService service = new CachingUserKeysService(proxyMock);
        assertEquals(userKeys, service.getUserKeys(null, null, null));
        Mockito.verify(proxyMock, times(1)).getUserKeys(any(), any(), any());
        assertEquals(userKeys, service.getUserKeys(null, null, null));
        Mockito.verify(proxyMock, times(1)).getUserKeys(any(), any(), any());
    }
}
