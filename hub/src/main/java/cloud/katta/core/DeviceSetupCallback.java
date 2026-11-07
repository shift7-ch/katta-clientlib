/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.core;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.UUIDRandomStringService;

import ch.cyberduck.core.nio.LocalProtocol;

import cloud.katta.crypto.DeviceKeys;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.HubVaultKeys;
import cloud.katta.crypto.uvf.WordEncoder;
import cloud.katta.workflows.exceptions.AccessException;

import org.cryptomator.cryptolib.common.P384KeyPair;

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

        @Override
        public void displayRecoveryKey(final Host bookmark, final P384KeyPair recoveryKey) throws AccessException {
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
     * Display the recovery key of a new vault to the vault owner prior to creating the vault. The recovery key
     * is not shown again and must be stored securely by the user to restore access to the vault.
     *
     * @param bookmark    Hub connection
     * @param recoveryKey Recovery key pair of the vault to be created. Use {@link #generateRecoveryKey(P384KeyPair)} for a human-readable representation of its private part
     * @throws AccessException Canceled prompt by user to abort vault creation
     */
    void displayRecoveryKey(Host bookmark, P384KeyPair recoveryKey) throws AccessException;

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

    /**
     * Encodes the private part of the recovery key in a human-readable representation to be displayed to the
     * vault owner. The encoding is interchangeable with the recovery key displayed in the web frontend.
     *
     * @return Private recovery key encoded as a list of words separated by {@value WordEncoder#DELIMITER}
     */
    default String generateRecoveryKey(final P384KeyPair recoveryKey) {
        return new WordEncoder().encodePadded(HubVaultKeys.createRecoveryKey(recoveryKey));
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
