/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.core.util;

import ch.cyberduck.core.Host;

import ch.iterate.hub.core.FirstLoginDeviceSetupCallback;
import ch.iterate.hub.model.AccountKeyAndDeviceName;
import ch.iterate.hub.workflows.exceptions.AccessException;

public class MockableFirstLoginDeviceSetupCallback implements FirstLoginDeviceSetupCallback {
    public static void setProxy(final FirstLoginDeviceSetupCallback proxy) {
        MockableFirstLoginDeviceSetupCallback.proxy = proxy;
    }

    private static FirstLoginDeviceSetupCallback proxy = null;

    @Override
    public String displayAccountKeyAndAskDeviceName(final Host bookmark, final AccountKeyAndDeviceName accountKeyAndDeviceName) throws AccessException {
        return proxy.displayAccountKeyAndAskDeviceName(bookmark, accountKeyAndDeviceName);
    }

    @Override
    public AccountKeyAndDeviceName askForAccountKeyAndDeviceName(final Host bookmark, final String initialDeviceName) throws AccessException {
        return proxy.askForAccountKeyAndDeviceName(bookmark, initialDeviceName);
    }

    @Override
    public String generateAccountKey() {
        return proxy.generateAccountKey();
    }
}
