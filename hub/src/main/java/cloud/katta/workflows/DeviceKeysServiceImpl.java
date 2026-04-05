/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.PasswordStore;
import ch.cyberduck.core.PasswordStoreFactory;
import ch.cyberduck.core.exception.AccessDeniedException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Base64;

import cloud.katta.client.model.UserDto;
import cloud.katta.core.DeviceSetupCallback;
import cloud.katta.crypto.DeviceKeys;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;

import static cloud.katta.crypto.KeyHelper.decodeKeyPair;

public class DeviceKeysServiceImpl implements DeviceKeysService {
    private static final Logger log = LogManager.getLogger(DeviceKeysServiceImpl.class.getName());

    public static final String KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME = "Katta Public Device Key";
    public static final String KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME = "Katta Private Device Key";

    private final PasswordStore store;

    public DeviceKeysServiceImpl() {
        this(PasswordStoreFactory.get());
    }

    public DeviceKeysServiceImpl(final PasswordStore store) {
        this.store = store;
    }

    private static String toAccountName(final Host hub, final UserDto me) {
        return String.format("%s@%s", me.getName(), hub.getHostname());
    }

    @Override
    public DeviceKeys getOrCreateDeviceKeys(final Host hub, final UserDto me, final DeviceSetupCallback setup) throws AccessException, SecurityFailure {
        final DeviceKeys deviceKeys = this.getDeviceKeys(hub, me);
        if(DeviceKeys.validate(deviceKeys)) {
            return deviceKeys;
        }
        log.warn("Create new device key for {}", hub);
        return this.storeDeviceKeys(hub, me, setup.generateDeviceKey());
    }

    @Override
    public DeviceKeys getDeviceKeys(final Host hub, final UserDto me) throws AccessException, SecurityFailure {
        final String accountName = toAccountName(hub, me);
        try {
            final String encodedPublicDeviceKey = store.getPassword(KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME, accountName);
            if(null == encodedPublicDeviceKey) {
                log.warn("No public device key found in keychain for {}", accountName);
                return DeviceKeys.notfound;
            }
            final String encodedPrivateDeviceKey = store.getPassword(KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME, accountName);
            if(null == encodedPrivateDeviceKey) {
                log.warn("No private device key found in keychain for {}", accountName);
                return DeviceKeys.notfound;
            }
            log.debug("Retrieved device key pair for {} from keychain", accountName);
            return new DeviceKeys(decodeKeyPair(encodedPublicDeviceKey, encodedPrivateDeviceKey));
        }
        catch(AccessDeniedException e) {
            throw new AccessException(e);
        }
    }

    protected DeviceKeys storeDeviceKeys(final Host hub, final UserDto me, final DeviceKeys deviceKeys) throws AccessException {
        final String accountName = toAccountName(hub, me);
        try {
            store.addPassword(KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME, accountName,
                    Base64.getEncoder().encodeToString(deviceKeys.getEcKeyPair().getPublic().getEncoded())
            );
            store.addPassword(KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME, accountName,
                    Base64.getEncoder().encodeToString(deviceKeys.getEcKeyPair().getPrivate().getEncoded())
            );
            log.debug("Saved device key pair for {} in keychain", accountName);
            return deviceKeys;
        }
        catch(AccessDeniedException e) {
            throw new AccessException(e);
        }
    }
}
