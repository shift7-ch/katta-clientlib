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

import org.rococoa.ObjCClass;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.CGFloat;
import org.rococoa.cocoa.foundation.NSRect;
import org.rococoa.cocoa.foundation.NSSize;

public class RecoveryKeyController extends AlertController {

    /**
     * Visible height of the recovery key. Scroll for the remaining lines.
     */
    private static final double RECOVERY_KEY_FIELD_HEIGHT = 160;
    private static final double RECOVERY_KEY_FIELD_WIDTH = 300;

    @Outlet
    private final NSTextView recoveryKeyField = NSTextView.create();

    @Outlet
    private final NSScrollView scrollView = NSScrollView.create(new NSRect(0, RECOVERY_KEY_FIELD_HEIGHT));

    private final String recoveryKey;

    /**
     * Temporary binding until available in ch.cyberduck.binding.application
     */
    public static abstract class NSScrollView extends NSView {
        private static final _Class CLASS = Rococoa.createClass("NSScrollView", _Class.class);

        /**
         * NSBorderType
         */
        public static final int NSNoBorder = 0;

        public static NSScrollView create(final NSRect frameRect) {
            return CLASS.alloc().initWithFrame(frameRect);
        }

        public interface _Class extends ObjCClass {
            NSScrollView alloc();
        }

        public abstract NSScrollView initWithFrame(NSRect frameRect);

        public abstract void setDocumentView(NSView view);

        public abstract void setBorderType(int type);

        public abstract void setDrawsBackground(boolean flag);

        public abstract void setHasVerticalScroller(boolean flag);
    }

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
        recoveryKeyField.setEditable(false);
        recoveryKeyField.setSelectable(true);
        // Blend in with the alert in both light and dark appearance
        recoveryKeyField.setDrawsBackground(false);
        // Wrap words within the width of the scroll view set in awakeFromNib
        recoveryKeyField.setHorizontallyResizable(false);
        recoveryKeyField.setVerticallyResizable(true);
        recoveryKeyField.setString(recoveryKey);
        // Wrap the recovery key within the width of the clip view and scroll for lines not visible
        recoveryKeyField.setFrameSize(new NSSize(RECOVERY_KEY_FIELD_WIDTH, RECOVERY_KEY_FIELD_HEIGHT));
        scrollView.setBorderType(NSScrollView.NSNoBorder);
        scrollView.setDrawsBackground(false);
        scrollView.setHasVerticalScroller(true);
        scrollView.setDocumentView(recoveryKeyField);
        scrollView.setFrameSize(new NSSize(RECOVERY_KEY_FIELD_WIDTH, RECOVERY_KEY_FIELD_HEIGHT));
        accessoryView.addSubview(scrollView);
        return accessoryView;
    }

    @Override
    protected NSRect getFrame(final NSView accessory) {
        final NSRect frame = super.getFrame(accessory);
        frame.size.width = new CGFloat(RECOVERY_KEY_FIELD_WIDTH);
        return frame;
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

