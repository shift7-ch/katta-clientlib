/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.text.ParseException;

import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.workflows.exceptions.AccessException;
import ch.iterate.hub.workflows.exceptions.SecurityFailure;
import com.nimbusds.jose.JOSEException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

class CachingWoTServiceTest {

    @Test
    void testGetTrustLevelsPerUserId() throws AccessException, SecurityFailure, ApiException {
        final WoTService proxyMock = Mockito.mock(WoTService.class);
        final CachingWoTService service = new CachingWoTService(proxyMock);
        service.getTrustLevelsPerUserId(null);
        Mockito.verify(proxyMock, times(1)).getTrustLevelsPerUserId(any());
        service.getTrustLevelsPerUserId(null);
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
        final CachingWoTService service = new CachingWoTService(proxyMock);
        service.sign(null, null);
        Mockito.verify(proxyMock, times(1)).sign(any(), any());
        service.sign(null, null);
        Mockito.verify(proxyMock, times(2)).sign(any(), any());
    }
}
