package ch.iterate.hub.workflows;

import ch.cyberduck.core.PasswordStore;
import ch.cyberduck.core.exception.LocalAccessDeniedException;

import org.cryptomator.cryptolib.common.P384KeyPair;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.UUID;

import ch.iterate.hub.testsetup.HubTestUtilities;

import static ch.iterate.hub.workflows.DeviceKeysService.KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME;
import static ch.iterate.hub.workflows.DeviceKeysService.KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DeviceKeysServiceTest {
    @Test
    public void testGetDeviceKeysFromPasswordStore() throws InvalidKeySpecException, LocalAccessDeniedException {
        final PasswordStore storeMock = Mockito.mock(PasswordStore.class);
        final DeviceKeysService service = new DeviceKeysService(storeMock);
        final String userId = UUID.randomUUID().toString();
        final String hubId = UUID.randomUUID().toString();
        service.getDeviceKeysFromPasswordStore(userId, hubId);
        verify(storeMock, times(1)).getPassword(KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME, String.format("%s@%s", userId, hubId));
        verify(storeMock, times(1)).getPassword(KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME, String.format("%s@%s", userId, hubId));
    }

    @Test
    public void testStoreDeviceKeysInPasswordStore() throws LocalAccessDeniedException {
        final PasswordStore storeMock = Mockito.mock(PasswordStore.class);
        final DeviceKeysService service = new DeviceKeysService(storeMock);
        final String userId = UUID.randomUUID().toString();
        final String hubId = UUID.randomUUID().toString();
        final P384KeyPair deviceKeys = P384KeyPair.generate();
        service.storeDeviceKeysInPasswordStore(deviceKeys, userId, hubId);
        verify(storeMock, times(1)).addPassword(KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME, String.format("%s@%s", userId, hubId), Base64.getEncoder().encodeToString(deviceKeys.getPublic().getEncoded()));
        verify(storeMock, times(1)).addPassword(KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME, String.format("%s@%s", userId, hubId), Base64.getEncoder().encodeToString(deviceKeys.getPrivate().getEncoded()));
    }

    @Test
    public void testStoreAndRetrieveDeviceKeys() throws InvalidKeySpecException, LocalAccessDeniedException {
        HubTestUtilities.preferences();

        final String userId = UUID.randomUUID().toString();
        final String hubId = UUID.randomUUID().toString();
        assertNull(new DeviceKeysService().getDeviceKeysFromPasswordStore(userId, hubId));

        final P384KeyPair deviceKeys = P384KeyPair.generate();
        new DeviceKeysService().storeDeviceKeysInPasswordStore(deviceKeys, userId, hubId);
        assertEquals(deviceKeys, new DeviceKeysService().getDeviceKeysFromPasswordStore(userId, hubId));
    }
}
