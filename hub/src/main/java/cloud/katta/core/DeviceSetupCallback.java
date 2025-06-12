/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.core;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.UUIDRandomStringService;

import cloud.katta.crypto.DeviceKeys;
import cloud.katta.crypto.UserKeys;
import cloud.katta.model.AccountKeyAndDeviceName;
import cloud.katta.workflows.exceptions.AccessException;

public interface DeviceSetupCallback {

    /**
     * Prompt user for device name
     *
     * @return Account key and device name
     * @throws AccessException Canceled prompt by user
     */
    AccountKeyAndDeviceName displayAccountKeyAndAskDeviceName(Host bookmark, AccountKeyAndDeviceName accountKeyAndDeviceName) throws AccessException;

    /**
     * Prompt user for existing account key
     *
     * @param initialDeviceName Default device name
     * @return Account key and device name
     * @throws AccessException Canceled prompt by user
     */
    AccountKeyAndDeviceName askForAccountKeyAndDeviceName(Host bookmark, String initialDeviceName) throws AccessException;

    /**
     * Generate initial account key
     *
     * @return Random UUID
     */
    default String generateAccountKey() {
        return new UUIDRandomStringService().random();
    }

    default DeviceKeys generateDeviceKey() {
        return DeviceKeys.create();
    }

    default UserKeys generateUserKeys() {
        return UserKeys.create();
    }

    DeviceSetupCallback disabled = new DeviceSetupCallback() {
        @Override
        public AccountKeyAndDeviceName displayAccountKeyAndAskDeviceName(final Host bookmark, final AccountKeyAndDeviceName accountKeyAndDeviceName) throws AccessException {
            throw new AccessException("Disabled");
        }

        @Override
        public AccountKeyAndDeviceName askForAccountKeyAndDeviceName(final Host bookmark, final String initialDeviceName) throws AccessException {
            throw new AccessException("Disabled");
        }
    };
}
