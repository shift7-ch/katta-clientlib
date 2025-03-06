/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.text.ParseException;
import java.util.HashMap;

import cloud.katta.client.ApiException;
import cloud.katta.client.model.TrustedUserDto;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.nimbusds.jose.JOSEException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

class CachingWoTServiceTest {

    @Test
    void testGetTrustLevelsPerUserId() throws AccessException, SecurityFailure, ApiException {
        final WoTService proxyMock = Mockito.mock(WoTService.class);
        final HashMap<String, Integer> trustLevels = new HashMap<String, Integer>() {{
            put("alkdajf", 5);
            put("lakdjfa", 42);
        }};
        Mockito.when(proxyMock.getTrustLevelsPerUserId(any())).thenReturn(trustLevels);
        final CachingWoTService service = new CachingWoTService(proxyMock);
        assertEquals(trustLevels, service.getTrustLevelsPerUserId(null));
        Mockito.verify(proxyMock, times(1)).getTrustLevelsPerUserId(any());
        assertEquals(trustLevels, service.getTrustLevelsPerUserId(null));
        Mockito.verify(proxyMock, times(1)).getTrustLevelsPerUserId(any());
    }

    @Test
    void testVerify() throws AccessException, SecurityFailure, ApiException {
        final WoTService proxyMock = Mockito.mock(WoTService.class);
        final CachingWoTService service = new CachingWoTService(proxyMock);
        service.verify(null, null, null);
        Mockito.verify(proxyMock, times(1)).verify(any(), any(), any());
        service.verify(null, null, null);
        Mockito.verify(proxyMock, times(2)).verify(any(), any(), any());
    }

    @Test
    void testSign() throws AccessException, SecurityFailure, ParseException, JOSEException, ApiException {
        final WoTService proxyMock = Mockito.mock(WoTService.class);
        final TrustedUserDto trustedUser = new TrustedUserDto();
        Mockito.when(proxyMock.sign(any(), any())).thenReturn(trustedUser);
        final CachingWoTService service = new CachingWoTService(proxyMock);
        assertEquals(trustedUser, service.sign(null, null));
        Mockito.verify(proxyMock, times(1)).sign(any(), any());
        assertEquals(trustedUser, service.sign(null, null));
        Mockito.verify(proxyMock, times(2)).sign(any(), any());
    }
}
