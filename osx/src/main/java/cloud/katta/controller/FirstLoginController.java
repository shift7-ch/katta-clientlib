/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.controller;

import ch.cyberduck.binding.Action;
import ch.cyberduck.binding.AlertController;
import ch.cyberduck.binding.Outlet;
import ch.cyberduck.binding.application.NSAlert;
import ch.cyberduck.binding.application.NSCell;
import ch.cyberduck.binding.application.NSControl;
import ch.cyberduck.binding.application.NSTextField;
import ch.cyberduck.binding.application.NSView;
import ch.cyberduck.binding.application.SheetCallback;
import ch.cyberduck.binding.foundation.NSNotification;
import ch.cyberduck.binding.foundation.NSNotificationCenter;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.StringAppender;
import ch.cyberduck.core.preferences.PreferencesFactory;

import org.apache.commons.lang3.StringUtils;
import org.rococoa.Foundation;

import cloud.katta.model.AccountKeyAndDeviceName;

public class FirstLoginController extends AlertController {

    private final AccountKeyAndDeviceName accountKeyAndDeviceName;

    @Outlet
    private final NSTextField accountKeyField = NSTextField.textFieldWithString(StringUtils.EMPTY);

    @Outlet
    private final NSTextField deviceNameField = NSTextField.textFieldWithString(StringUtils.EMPTY);

    public FirstLoginController(final AccountKeyAndDeviceName accountKeyAndDeviceName) {
        this.accountKeyAndDeviceName = accountKeyAndDeviceName;
    }

    @Override
    public NSAlert loadAlert() {
        final NSAlert alert = NSAlert.alert();
        alert.setAlertStyle(NSAlert.NSInformationalAlertStyle);
        alert.setMessageText(LocaleFactory.localizedString("Account Key", "Hub"));
        alert.setInformativeText(new StringAppender()
                .append(LocaleFactory.localizedString("On first login, every user gets a unique Account Key", "Hub"))
                .append(LocaleFactory.localizedString("Your Account Key is required to login from other apps or browsers", "Hub"))
                .append(LocaleFactory.localizedString("You can see a list of authorized apps on your profile page", "Hub")).toString());
        alert.addButtonWithTitle(LocaleFactory.localizedString("Finish Setup", "Hub"));
        alert.addButtonWithTitle(LocaleFactory.localizedString("Cancel", "Alert"));
        alert.setShowsSuppressionButton(true);
        alert.suppressionButton().setTitle(LocaleFactory.localizedString("Add to Keychain", "Login"));
        alert.suppressionButton().setState(PreferencesFactory.get().getBoolean("cryptomator.vault.keychain") ? NSCell.NSOnState : NSCell.NSOffState);
        return alert;
    }

    @Override
    public NSView getAccessoryView(final NSAlert alert) {
        final NSView accessoryView = NSView.create();
        {
            accountKeyField.setEditable(false);
            accountKeyField.setSelectable(true);
            accountKeyField.cell().setWraps(false);
            this.updateField(accountKeyField, accountKeyAndDeviceName.accountKey(), TRUNCATE_MIDDLE_ATTRIBUTES);
            this.addAccessorySubview(accessoryView, accountKeyField);
        }

        {
            this.updateField(deviceNameField, accountKeyAndDeviceName.deviceName());
            deviceNameField.cell().setPlaceholderString(LocaleFactory.localizedString("Device Name", "Hub"));
            deviceNameField.setToolTip(LocaleFactory.localizedString("Name this device for easy identification in your authorized devices list.", "Hub"));
            NSNotificationCenter.defaultCenter().addObserver(this.id(),
                    Foundation.selector("deviceNameFieldTextDidChange:"),
                    NSControl.NSControlTextDidChangeNotification,
                    deviceNameField.id());
            this.addAccessorySubview(accessoryView, deviceNameField);
        }

        return accessoryView;
    }

    @Override
    protected void focus(final NSAlert alert) {
        super.focus(alert);
        window.makeFirstResponder(deviceNameField);
    }

    @Override
    public boolean validate(final int option) {
        if(SheetCallback.DEFAULT_OPTION == option) {
            return StringUtils.isNotBlank(deviceNameField.stringValue());
        }
        return true;
    }

    @Override
    public void callback(final int returncode) {
        if(SheetCallback.DEFAULT_OPTION == returncode) {
            accountKeyAndDeviceName.withAddToKeychain(this.isSuppressed());
        }
    }

    @Action
    public void deviceNameFieldTextDidChange(final NSNotification sender) {
        accountKeyAndDeviceName.withDeviceName(StringUtils.trim(deviceNameField.stringValue()));
    }
}

