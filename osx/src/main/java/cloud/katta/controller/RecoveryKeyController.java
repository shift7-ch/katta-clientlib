/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.controller;

import ch.cyberduck.binding.AlertController;
import ch.cyberduck.binding.Outlet;
import ch.cyberduck.binding.application.NSAlert;
import ch.cyberduck.binding.application.NSCell;
import ch.cyberduck.binding.application.NSTextView;
import ch.cyberduck.binding.application.NSView;
import ch.cyberduck.binding.application.SheetCallback;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.StringAppender;

public class RecoveryKeyController extends AlertController {

    @Outlet
    private final NSTextView recoveryKeyField = NSTextView.create();

    private final String recoveryKey;

    public RecoveryKeyController(final String recoveryKey) {
        this.recoveryKey = recoveryKey;
    }

    @Override
    public NSAlert loadAlert() {
        final NSAlert alert = NSAlert.alert();
        alert.setAlertStyle(NSAlert.NSInformationalAlertStyle);
        alert.setMessageText(LocaleFactory.localizedString("Recovery Key", "Hub"));
        alert.setInformativeText(new StringAppender()
                .append(LocaleFactory.localizedString(
                        "The following recovery key can be used to restore access to the vault.", "Hub"))
                .toString());
        alert.addButtonWithTitle(LocaleFactory.localizedString("Create Vault", "Cryptomator"));
        alert.addButtonWithTitle(LocaleFactory.localizedString("Cancel", "Alert"));
        alert.setShowsSuppressionButton(true);
        alert.suppressionButton().setTitle(LocaleFactory.localizedString(
                "I understand that I will lose access to the vault in the event of an emergency if I don't have the recovery key.", "Hub"));
        alert.suppressionButton().setState(NSCell.NSOffState);
        return alert;
    }

    @Override
    public NSView getAccessoryView(final NSAlert alert) {
        final NSView accessoryView = NSView.create();
        {
            recoveryKeyField.setEditable(false);
            recoveryKeyField.setSelectable(true);
            recoveryKeyField.setString(recoveryKey);
            this.addAccessorySubview(accessoryView, recoveryKeyField);
        }
        return accessoryView;
    }

    @Override
    public void focus() {
        super.focus();
        window.makeFirstResponder(recoveryKeyField);
    }

    @Override
    public boolean validate(final int option) {
        if(SheetCallback.DEFAULT_OPTION == option) {
            if(!this.isSuppressed()) {
                return false;
            }
        }
        return true;
    }
}

