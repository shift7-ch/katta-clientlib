/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.core;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.UUIDRandomStringService;

import ch.cyberduck.core.nio.LocalProtocol;

import cloud.katta.crypto.DeviceKeys;
import cloud.katta.crypto.UserKeys;
import cloud.katta.workflows.exceptions.AccessException;

public interface DeviceSetupCallback {

    DeviceSetupCallback disabled = new DeviceSetupCallback() {
        @Override
        public AccountKeyAndDeviceName displayAccountKeyAndAskDeviceName(final Host bookmark, String accountKey) throws AccessException {
            throw new AccessException("Disabled");
        }

        @Override
        public AccountKeyAndDeviceName askForAccountKeyAndDeviceName(final Host bookmark) throws AccessException {
            throw new AccessException("Disabled");
        }
    };

    /**
     * Prompt user for device name
     *
     * @return Account key and device name
     * @throws AccessException Canceled prompt by user
     */
    AccountKeyAndDeviceName displayAccountKeyAndAskDeviceName(Host bookmark, String accountKey) throws AccessException;

    /**
     * Prompt user for existing account key
     *
     * @return Account key and device name
     * @throws AccessException Canceled prompt by user
     */
    AccountKeyAndDeviceName askForAccountKeyAndDeviceName(Host bookmark) throws AccessException;

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

    final class AccountKeyAndDeviceName {
        public static final String COMPUTER_NAME = new LocalProtocol().getName();

        private String accountKey;
        private String deviceName;

        public AccountKeyAndDeviceName(final String accountKey, final String deviceName) {
            this.accountKey = accountKey;
            this.deviceName = deviceName;
        }

        public String accountKey() {
            return accountKey;
        }

        public String deviceName() {
            return deviceName;
        }

        public AccountKeyAndDeviceName setAccountKey(final String accountKey) {
            this.accountKey = accountKey;
            return this;
        }

        public AccountKeyAndDeviceName setDeviceName(final String deviceName) {
            this.deviceName = deviceName;
            return this;
        }
    }
}
