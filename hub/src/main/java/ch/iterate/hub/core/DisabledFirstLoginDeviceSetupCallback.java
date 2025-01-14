/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.exception.LoginCanceledException;

import ch.iterate.hub.model.AccountKeyAndDeviceName;

public class DisabledFirstLoginDeviceSetupCallback implements FirstLoginDeviceSetupCallback {

    @Override
    public String displayAccountKeyAndAskDeviceName(final Host bookmark, final AccountKeyAndDeviceName accountKeyAndDeviceName) throws ConnectionCanceledException {
        throw new LoginCanceledException();
    }

    @Override
    public AccountKeyAndDeviceName askForAccountKeyAndDeviceName(final Host bookmark, final String initialDeviceName) throws ConnectionCanceledException {
        throw new LoginCanceledException();
    }
}
