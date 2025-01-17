/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.exception.ConnectionCanceledException;

import ch.iterate.hub.model.AccountKeyAndDeviceName;

public interface FirstLoginDeviceSetupCallback {

    String displayAccountKeyAndAskDeviceName(Host bookmark, final AccountKeyAndDeviceName accountKeyAndDeviceName) throws ConnectionCanceledException;

    String generateAccountKey();

    AccountKeyAndDeviceName askForAccountKeyAndDeviceName(Host bookmark, final String initialDeviceName) throws ConnectionCanceledException;

}
