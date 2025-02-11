/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.cyberduck.core.PasswordStore;
import ch.cyberduck.core.PasswordStoreFactory;
import ch.cyberduck.core.exception.LocalAccessDeniedException;
import ch.cyberduck.core.nio.LocalProtocol;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cryptomator.cryptolib.common.ECKeyPair;

import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import static ch.iterate.hub.crypto.KeyHelper.decodeKeyPair;

public class DeviceKeysService {
    private static final Logger log = LogManager.getLogger(DeviceKeysService.class.getName());

    public static final String KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME = "Cipherduck Public Device Key";
    public static final String KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME = "Cipherduck Private Device Key";

    public static final String COMPUTER_NAME = new LocalProtocol().getName();
    private final PasswordStore store;

    public DeviceKeysService(final PasswordStore store) {
        this.store = store;
    }

    public DeviceKeysService() {
        this(PasswordStoreFactory.get());
    }

    private static String toAccountName(final String userId, final String hubUUID) {
        return String.format("%s@%s", userId, hubUUID);
    }

    public ECKeyPair getDeviceKeysFromPasswordStore(final String userId, final String hubUUID) throws LocalAccessDeniedException, InvalidKeySpecException {
        final String accountName = toAccountName(userId, hubUUID);
        final String encodedPublicDeviceKey = store.getPassword(KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME, accountName);
        final String encodedPrivateDeviceKey = store.getPassword(KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME, accountName);
        if (null == encodedPublicDeviceKey || null == encodedPrivateDeviceKey) {
            return null;
        }
        log.debug("Retrieved device key pair for {} from keychain", accountName);
        return decodeKeyPair(encodedPublicDeviceKey, encodedPrivateDeviceKey);
    }

    public void storeDeviceKeysInPasswordStore(final ECKeyPair deviceKeys, final String userId, final String hubUUID) throws LocalAccessDeniedException {
        final String accountName = toAccountName(userId, hubUUID);
        store.addPassword(KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME, accountName,
                Base64.getEncoder().encodeToString(deviceKeys.getPublic().getEncoded())
        );
        store.addPassword(KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME, accountName,
                Base64.getEncoder().encodeToString(deviceKeys.getPrivate().getEncoded())
        );
        log.debug("Saved device key pair for {} in keychain", accountName);
    }

    public static boolean validateDeviceKeys(final ECKeyPair deviceKeyPairFromKeychain) {
        return deviceKeyPairFromKeychain != null;
    }
}
