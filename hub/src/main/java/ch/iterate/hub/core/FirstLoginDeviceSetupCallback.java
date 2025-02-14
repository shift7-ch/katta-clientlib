/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.core;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.UUIDRandomStringService;

import ch.iterate.hub.crypto.DeviceKeys;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.model.AccountKeyAndDeviceName;
import ch.iterate.hub.workflows.exceptions.AccessException;

public interface FirstLoginDeviceSetupCallback {

    /**
     * Prompt user for device name
     *
     * @return Device name
     * @throws AccessException Canceled prompt by user
     */
    String displayAccountKeyAndAskDeviceName(Host bookmark, AccountKeyAndDeviceName accountKeyAndDeviceName) throws AccessException;

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

    FirstLoginDeviceSetupCallback disabled = new FirstLoginDeviceSetupCallback() {
        @Override
        public String displayAccountKeyAndAskDeviceName(final Host bookmark, final AccountKeyAndDeviceName accountKeyAndDeviceName) throws AccessException {
            throw new AccessException("Disabled");
        }

        @Override
        public AccountKeyAndDeviceName askForAccountKeyAndDeviceName(final Host bookmark, final String initialDeviceName) throws AccessException {
            throw new AccessException("Disabled");
        }
    };
}
