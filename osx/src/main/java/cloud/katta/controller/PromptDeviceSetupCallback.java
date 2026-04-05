/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.controller;

import ch.cyberduck.binding.ProxyController;
import ch.cyberduck.binding.SheetController;
import ch.cyberduck.binding.application.SheetCallback;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.exception.ConnectionCanceledException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cloud.katta.core.DeviceSetupCallback;
import cloud.katta.workflows.exceptions.AccessException;

public class PromptDeviceSetupCallback implements DeviceSetupCallback {
    private static final Logger log = LogManager.getLogger(PromptDeviceSetupCallback.class.getName());

    private final ProxyController controller;

    public PromptDeviceSetupCallback(final ProxyController controller) {
        this.controller = controller;
    }

    @Override
    public AccountKeyAndDeviceName displayAccountKeyAndAskDeviceName(final Host bookmark, final String accountKey) throws AccessException {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Display Account Key for %s", bookmark));
        }
        final AccountKeyAndDeviceName input = new AccountKeyAndDeviceName(accountKey, AccountKeyAndDeviceName.COMPUTER_NAME);
        final SheetController sheet = new FirstLoginController(input);
        switch(controller.alert(sheet)) {
            case SheetCallback.CANCEL_OPTION:
            case SheetCallback.ALTERNATE_OPTION:
                throw new AccessException(new ConnectionCanceledException());
        }
        return input;
    }

    @Override
    public AccountKeyAndDeviceName askForAccountKeyAndDeviceName(final Host bookmark) throws AccessException {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Ask for Account Key for %s", bookmark));
        }
        final AccountKeyAndDeviceName accountKeyAndDeviceName = new AccountKeyAndDeviceName(StringUtils.EMPTY, AccountKeyAndDeviceName.COMPUTER_NAME);
        final DeviceSetupController sheet = new DeviceSetupController(accountKeyAndDeviceName);
        switch(controller.alert(sheet)) {
            case SheetCallback.CANCEL_OPTION:
            case SheetCallback.ALTERNATE_OPTION:
                throw new AccessException(new ConnectionCanceledException());
        }
        return accountKeyAndDeviceName;
    }
}
