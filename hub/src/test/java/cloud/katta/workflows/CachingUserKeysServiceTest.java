/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import cloud.katta.client.ApiException;
import cloud.katta.crypto.UserKeys;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;

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
