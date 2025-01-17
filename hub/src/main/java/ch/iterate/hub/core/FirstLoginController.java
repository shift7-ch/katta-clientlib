/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core;

import ch.cyberduck.binding.Action;
import ch.cyberduck.binding.Outlet;
import ch.cyberduck.binding.SheetController;
import ch.cyberduck.binding.application.NSButton;
import ch.cyberduck.binding.application.NSCell;
import ch.cyberduck.binding.application.NSControl;
import ch.cyberduck.binding.application.NSImage;
import ch.cyberduck.binding.application.NSImageView;
import ch.cyberduck.binding.application.NSTextField;
import ch.cyberduck.binding.foundation.NSNotification;
import ch.cyberduck.binding.foundation.NSNotificationCenter;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.StringAppender;
import ch.cyberduck.core.exception.LocalAccessDeniedException;
import ch.cyberduck.core.resources.IconCacheFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rococoa.Foundation;
import org.rococoa.ID;

import ch.iterate.hub.model.AccountKeyAndDeviceName;
import ch.iterate.mountainduck.service.PasteboardService;
import ch.iterate.mountainduck.service.PasteboardServiceFactory;


public class FirstLoginController extends SheetController {
    private static final Logger log = LogManager.getLogger(FirstLoginController.class.getName());
    protected final NSNotificationCenter notificationCenter
            = NSNotificationCenter.defaultCenter();
    private final String title;
    private final String reason;
    private final String icon;

    private final String setupCodeHint_;
    private final String setupCodeHintIcon_;
    private final String setupCodeHint2_;
    private final String setupCodeHint2Icon_;
    private final String deviceNameHint_;
    private final String deviceNameHintIcon_;
    private final AccountKeyAndDeviceName accountKeyAndDeviceName;


    @Outlet
    private NSImageView iconView;
    @Outlet
    private NSTextField titleField;
    @Outlet
    private NSTextField messageField;

    @Outlet
    protected NSTextField setupCodeField;
    @Outlet
    protected NSTextField setupCodeHint;
    @Outlet
    private NSImageView setupCodeHintIcon;

    @Outlet
    protected NSTextField setupCodeHint2;
    @Outlet
    private NSImageView setupCodeHint2Icon;

    @Outlet
    protected NSTextField deviceNameHint;
    @Outlet
    private NSImageView deviceNameHintIcon;

    @Outlet
    protected NSTextField deviceNameField;
    private NSButton accountKeyStoredSecurelyCheckbox;
    private NSButton finishSetupButton;

    public FirstLoginController(final AccountKeyAndDeviceName accountKeyAndDeviceName) {
        this.icon = "cryptomator.tiff";
        this.title = LocaleFactory.localizedString("Welcome to Cipherduck", "Cipherduck");
        this.reason = LocaleFactory.localizedString("On first login, every user gets a unique Account Key.", "Cipherduck");
        this.setupCodeHint_ = LocaleFactory.localizedString("Your Account Key is required to login from other apps or browsers.", "Cipherduck");
        this.setupCodeHint2_ = LocaleFactory.localizedString("You can see a list of authorized apps on your profile page in Cipherduck Hub.", "Cipherduck");
        this.deviceNameHint_ = LocaleFactory.localizedString("This device will be added to this list as:", "Cipherduck");
        this.accountKeyAndDeviceName = accountKeyAndDeviceName;
        this.setupCodeHintIcon_ = "KeyIcon.tiff";
        this.setupCodeHint2Icon_ = "ListBulletIcon.tiff";
        this.deviceNameHintIcon_ = "ComputerDesktopIcon.tiff";
    }

    @Override
    public void awakeFromNib() {
        super.awakeFromNib();
        window.makeFirstResponder(titleField);
    }

    @Override
    protected String getBundleName() {
        return "FirstLogin";
    }

    public void setIconView(NSImageView iconView) {
        this.iconView = iconView;
        this.iconView.setImage(IconCacheFactory.<NSImage>get().iconNamed(this.icon, 64));
    }

    public void setTitleField(NSTextField titleField) {
        this.titleField = titleField;
        this.updateField(this.titleField, LocaleFactory.localizedString(title, "Cipherduck"));
    }

    public void setMessageField(NSTextField messageField) {
        this.messageField = messageField;
        this.messageField.setSelectable(true);
        this.updateField(this.messageField, new StringAppender().append(reason).toString());
    }

    public void setSetupCodeField(final NSTextField field) {
        this.setupCodeField = field;
        this.setupCodeField.setStringValue(this.accountKeyAndDeviceName.accountKey());
    }

    public void setSetupCodeHint(NSTextField messageField) {
        this.setupCodeHint = messageField;
        this.updateField(this.setupCodeHint, LocaleFactory.localizedString(this.setupCodeHint_, "Cipherduck"));
    }

    public void setSetupCodeHintIcon(NSImageView iconView) {
        this.setupCodeHintIcon = iconView;
        this.setupCodeHintIcon.setImage(IconCacheFactory.<NSImage>get().iconNamed(this.setupCodeHintIcon_, 64));
    }

    public void setSetupCodeHint2(NSTextField messageField) {
        this.setupCodeHint2 = messageField;
        this.updateField(this.setupCodeHint2, LocaleFactory.localizedString(this.setupCodeHint2_, "Cipherduck"));
    }

    public void setSetupCodeHint2Icon(NSImageView iconView) {
        this.setupCodeHint2Icon = iconView;
        this.setupCodeHint2Icon.setImage(IconCacheFactory.<NSImage>get().iconNamed(this.setupCodeHint2Icon_, 64));
    }

    public void setDeviceNameHint(NSTextField messageField) {
        this.deviceNameHint = messageField;
        this.updateField(this.deviceNameHint, LocaleFactory.localizedString(this.deviceNameHint_, "Cipherduck"));
    }

    public void setDeviceNameHintIcon(NSImageView iconView) {
        this.deviceNameHintIcon = iconView;
        this.deviceNameHintIcon.setImage(IconCacheFactory.<NSImage>get().iconNamed(this.deviceNameHintIcon_, 64));
    }

    public void setDeviceNameField(final NSTextField field) {
        this.deviceNameField = field;
        this.deviceNameField.setStringValue(StringUtils.isNotBlank(this.accountKeyAndDeviceName.deviceName()) ? this.accountKeyAndDeviceName.deviceName() : StringUtils.EMPTY);
        this.notificationCenter.addObserver(this.id(),
                Foundation.selector("deviceNameInputDidChange:"),
                NSControl.NSControlTextDidChangeNotification,
                field.id());
    }

    @Action
    public void deviceNameInputDidChange(final NSNotification sender) {
        accountKeyAndDeviceName.withDeviceName(StringUtils.trim(deviceNameField.stringValue()));
    }

    @Override
    public void callback(final int returncode) {
        //
    }

    @Action
    public void copyToClipboard(final ID sender) {
        try {
            PasteboardServiceFactory.get().add(PasteboardService.Type.string, accountKeyAndDeviceName.accountKey());
        }
        catch(LocalAccessDeniedException e) {
            log.error("Could not copy Account Key to pasteboard.", e);
        }
    }

    public void setFinishSetupButton(final NSButton button) {
        this.finishSetupButton = button;
        this.finishSetupButton.setEnabled(false);
    }

    public void setAccountKeyStoredSecurelyCheckbox(final NSButton button) {
        this.accountKeyStoredSecurelyCheckbox = button;
        this.accountKeyStoredSecurelyCheckbox.setTarget(this.id());
        this.accountKeyStoredSecurelyCheckbox.setAction(Foundation.selector("accountKeyStoredSecurelyCheckboxClicked:"));
    }

    @Action
    public void accountKeyStoredSecurelyCheckboxClicked(final NSButton sender) {
        this.finishSetupButton.setEnabled(sender.state() == NSCell.NSOnState);
    }

}

