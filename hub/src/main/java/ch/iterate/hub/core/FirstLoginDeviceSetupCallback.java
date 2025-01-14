/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core;

import ch.cyberduck.core.Host;
import ch.cyberduck.core.exception.ConnectionCanceledException;

import ch.iterate.hub.model.AccountKeyAndDeviceName;

public interface FirstLoginDeviceSetupCallback {

    /**
     * @return Device name
     * @throws ConnectionCanceledException Canceled prompt by user
     */
    String displayAccountKeyAndAskDeviceName(Host bookmark, AccountKeyAndDeviceName accountKeyAndDeviceName) throws ConnectionCanceledException;

    AccountKeyAndDeviceName askForAccountKeyAndDeviceName(Host bookmark, String initialDeviceName) throws ConnectionCanceledException;

}
