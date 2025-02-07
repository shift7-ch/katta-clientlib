/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.UUIDRandomStringService;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.exception.LoginCanceledException;

import ch.iterate.hub.model.AccountKeyAndDeviceName;

public interface FirstLoginDeviceSetupCallback {

    /**
     * Prompt user for device name
     *
     * @return Device name
     * @throws ConnectionCanceledException Canceled prompt by user
     */
    String displayAccountKeyAndAskDeviceName(Host bookmark, AccountKeyAndDeviceName accountKeyAndDeviceName) throws ConnectionCanceledException;

    /**
     * Prompt user for existing account key
     *
     * @param initialDeviceName Default device name
     * @return Account key and device name
     * @throws ConnectionCanceledException Canceled prompt by user
     */
    AccountKeyAndDeviceName askForAccountKeyAndDeviceName(Host bookmark, String initialDeviceName) throws ConnectionCanceledException;

    /**
     * Generate initial account key
     *
     * @return Random UUID
     */
    default String generateAccountKey() {
        return new UUIDRandomStringService().random();
    }

    FirstLoginDeviceSetupCallback disabled = new FirstLoginDeviceSetupCallback() {
        @Override
        public String displayAccountKeyAndAskDeviceName(final Host bookmark, final AccountKeyAndDeviceName accountKeyAndDeviceName) throws ConnectionCanceledException {
            throw new LoginCanceledException();
        }

        @Override
        public AccountKeyAndDeviceName askForAccountKeyAndDeviceName(final Host bookmark, final String initialDeviceName) throws ConnectionCanceledException {
            throw new LoginCanceledException();
        }
    };
}
