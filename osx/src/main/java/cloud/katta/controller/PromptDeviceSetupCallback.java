/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.controller;

import ch.cyberduck.binding.ProxyController;
import ch.cyberduck.binding.SheetController;
import ch.cyberduck.binding.application.SheetCallback;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.exception.ConnectionCanceledException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cloud.katta.core.DeviceSetupCallback;
import cloud.katta.model.AccountKeyAndDeviceName;
import cloud.katta.workflows.exceptions.AccessException;

public class PromptDeviceSetupCallback implements DeviceSetupCallback {
    private static final Logger log = LogManager.getLogger(PromptDeviceSetupCallback.class.getName());

    private final ProxyController controller;

    public PromptDeviceSetupCallback(final ProxyController controller) {
        this.controller = controller;
    }

    @Override
    public AccountKeyAndDeviceName displayAccountKeyAndAskDeviceName(final Host bookmark, final AccountKeyAndDeviceName accountKeyAndDeviceName) throws AccessException {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Display Account Key for %s", bookmark));
        }
        final SheetController sheet = new FirstLoginController(accountKeyAndDeviceName);
        switch(controller.alert(sheet)) {
            case SheetCallback.CANCEL_OPTION:
            case SheetCallback.ALTERNATE_OPTION:
                throw new AccessException(new ConnectionCanceledException());
        }
        return accountKeyAndDeviceName;
    }

    @Override
    public AccountKeyAndDeviceName askForAccountKeyAndDeviceName(final Host bookmark, final String initialDeviceName) throws AccessException {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Ask for Account Key for %s", bookmark));
        }
        final AccountKeyAndDeviceName accountKeyAndDeviceName = new AccountKeyAndDeviceName().withDeviceName(initialDeviceName);
        final DeviceSetupController sheet = new DeviceSetupController(accountKeyAndDeviceName);
        switch(controller.alert(sheet)) {
            case SheetCallback.CANCEL_OPTION:
            case SheetCallback.ALTERNATE_OPTION:
                throw new AccessException(new ConnectionCanceledException());
        }
        return accountKeyAndDeviceName;
    }
}
