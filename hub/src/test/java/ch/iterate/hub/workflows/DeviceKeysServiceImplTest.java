/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.PasswordStore;
import ch.cyberduck.core.exception.LocalAccessDeniedException;
import ch.iterate.hub.client.model.UserDto;
import ch.iterate.hub.crypto.DeviceKeys;
import ch.iterate.hub.protocols.hub.HubProtocol;
import ch.iterate.hub.testsetup.AbstractHubTest;
import ch.iterate.hub.workflows.exceptions.AccessException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Base64;
import java.util.UUID;

import static ch.iterate.hub.workflows.DeviceKeysServiceImpl.KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME;
import static ch.iterate.hub.workflows.DeviceKeysServiceImpl.KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DeviceKeysServiceImplTest extends AbstractHubTest {

    @Test
    void testGetDeviceKeys() throws LocalAccessDeniedException, AccessException {
        final PasswordStore storeMock = Mockito.mock(PasswordStore.class);
        when(storeMock.getPassword(eq(KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME), any())).thenReturn("v");
        final DeviceKeysServiceImpl service = new DeviceKeysServiceImpl(storeMock);
        final Host hubId = new Host(new HubProtocol());
        hubId.setUuid(UUID.randomUUID().toString());
        final UserDto userId = new UserDto();
        userId.setId(UUID.randomUUID().toString());
        service.getDeviceKeys(hubId);
        verify(storeMock, times(1)).getPassword(eq(KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME), any());
        verify(storeMock, times(1)).getPassword(eq(KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME), any());
    }

    @Test
    void testStoreDeviceKeys() throws LocalAccessDeniedException, AccessException {
        final PasswordStore storeMock = Mockito.mock(PasswordStore.class);
        when(storeMock.getPassword(eq(KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME), any())).thenReturn("v");
        final DeviceKeysServiceImpl service = new DeviceKeysServiceImpl(storeMock);
        final UserDto userId = new UserDto();
        userId.setId(UUID.randomUUID().toString());
        final Host hubId = new Host(new HubProtocol());
        hubId.setUuid(UUID.randomUUID().toString());
        final DeviceKeys deviceKeys = DeviceKeys.create();
        service.storeDeviceKeys(hubId, deviceKeys);
        verify(storeMock, times(1)).addPassword(eq(KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME), any(),
                eq(Base64.getEncoder().encodeToString(deviceKeys.getEcKeyPair().getPublic().getEncoded())));
        verify(storeMock, times(1)).addPassword(eq(KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME), any(),
                eq(Base64.getEncoder().encodeToString(deviceKeys.getEcKeyPair().getPrivate().getEncoded())));
    }

    @Test
    void testStoreAndRetrieveDeviceKeys() throws AccessException {
        final UserDto userId = new UserDto();
        userId.setId(UUID.randomUUID().toString());
        final Host hubId = new Host(new HubProtocol());
        hubId.setUuid(UUID.randomUUID().toString());
        assertEquals(DeviceKeys.notfound, new DeviceKeysServiceImpl().getDeviceKeys(hubId));
        final DeviceKeys deviceKeys = DeviceKeys.create();
        new DeviceKeysServiceImpl().storeDeviceKeys(hubId, deviceKeys);
        assertEquals(deviceKeys, new DeviceKeysServiceImpl().getDeviceKeys(hubId));
    }
}
