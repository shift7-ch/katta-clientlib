/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.shift7.katta.controller;

import ch.cyberduck.binding.Action;
import ch.cyberduck.binding.BundleController;
import ch.cyberduck.binding.Outlet;
import ch.cyberduck.binding.SheetController;
import ch.cyberduck.binding.application.NSControl;
import ch.cyberduck.binding.application.NSImage;
import ch.cyberduck.binding.application.NSImageView;
import ch.cyberduck.binding.application.NSSecureTextField;
import ch.cyberduck.binding.application.NSTextField;
import ch.cyberduck.binding.foundation.NSAttributedString;
import ch.cyberduck.binding.foundation.NSNotification;
import ch.cyberduck.binding.foundation.NSNotificationCenter;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.StringAppender;
import ch.cyberduck.core.resources.IconCacheFactory;

import org.apache.commons.lang3.StringUtils;
import org.rococoa.Foundation;

import ch.iterate.hub.model.AccountKeyAndDeviceName;


public class DeviceSetupWithAccountKeyController extends SheetController {
    protected final NSNotificationCenter notificationCenter
            = NSNotificationCenter.defaultCenter();
    private final String title;
    private final String reason;
    private final String icon;
    private final AccountKeyAndDeviceName accountKeyAndDeviceName;

    private final String setupCodeLabel_;
    private final String setupCodeHint_;
    private final String deviceNameLabel_;
    private final String deviceNameHint_;

    @Outlet
    private NSImageView iconView;
    @Outlet
    private NSTextField titleField;
    @Outlet
    private NSTextField messageField;

    @Outlet
    protected NSTextField setupCodeField;
    @Outlet
    protected NSTextField setupCodeLabel;
    @Outlet
    protected NSTextField setupCodeHint;

    @Outlet
    protected NSTextField deviceNameField;
    @Outlet
    protected NSTextField deviceNameLabel;
    @Outlet
    protected NSTextField deviceNameHint;

    public DeviceSetupWithAccountKeyController(final AccountKeyAndDeviceName accountKeyAndDeviceName) {
        this.accountKeyAndDeviceName = accountKeyAndDeviceName;
        this.title = LocaleFactory.localizedString("Authorization Required", "Cipherduck");
        this.reason = LocaleFactory.localizedString("This is your first login on this device. ", "Cipherduck");
        this.setupCodeLabel_ = LocaleFactory.localizedString("Account Key", "Cipherduck");
        this.setupCodeHint_ = LocaleFactory.localizedString("Your Account Key is required to authorize this device.", "Cipherduck");
        this.deviceNameLabel_ = LocaleFactory.localizedString("Device Name", "Cipherduck");
        this.deviceNameHint_ = LocaleFactory.localizedString("Name this device for easy identification in your authorized devices list.", "Cipherduck");
        this.icon = "cryptomator.tiff";
    }

    @Override
    public void awakeFromNib() {
        super.awakeFromNib();
        window.makeFirstResponder(titleField);
    }

    @Override
    protected String getBundleName() {
        return "DeviceSetupWithAccountKey";
    }

    public void setIconView(NSImageView iconView) {
        this.iconView = iconView;
        this.iconView.setImage(IconCacheFactory.<NSImage>get().iconNamed(this.icon, 64));
    }

    public void setTitleField(NSTextField titleField) {
        this.titleField = titleField;
        this.updateField(this.titleField, LocaleFactory.localizedString(title, "Credentials"));
    }

    public void setMessageField(NSTextField messageField) {
        this.messageField = messageField;
        this.messageField.setSelectable(true);
        this.updateField(this.messageField, new StringAppender().append(reason).toString());
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

    public void setDeviceNameLabel(final NSTextField deviceNameLabel) {
        this.deviceNameLabel = deviceNameLabel;
        deviceNameLabel.setAttributedStringValue(NSAttributedString.attributedStringWithAttributes(
                String.format("%s:", this.deviceNameLabel_),
                BundleController.TRUNCATE_TAIL_ATTRIBUTES
        ));
    }

    public void setDeviceNameHint(NSTextField messageField) {
        this.deviceNameHint = messageField;
        this.updateField(this.deviceNameHint, LocaleFactory.localizedString(this.deviceNameHint_, "Cipherduck"));
    }

    public void setSetupCodeLabel(final NSTextField setSetupCodeLabel) {
        this.setupCodeLabel = setSetupCodeLabel;
        setSetupCodeLabel.setAttributedStringValue(NSAttributedString.attributedStringWithAttributes(
                String.format("%s:", this.setupCodeLabel_),
                BundleController.TRUNCATE_TAIL_ATTRIBUTES
        ));
    }

    public void setSetupCodeField(NSSecureTextField field) {
        this.setupCodeField = field;
        this.notificationCenter.addObserver(this.id(),
                Foundation.selector("setupCodeInputDidChange:"),
                NSControl.NSControlTextDidChangeNotification,
                field.id());
    }

    @Action
    public void setupCodeInputDidChange(final NSNotification sender) {
        this.accountKeyAndDeviceName.withAccountKey(StringUtils.trim(setupCodeField.stringValue()));
    }

    public void setSetupCodeHint(NSTextField messageField) {
        this.setupCodeHint = messageField;
        this.updateField(this.setupCodeHint, LocaleFactory.localizedString(this.setupCodeHint_, "Cipherduck"));
    }


    @Override
    public void callback(final int returncode) {
        //
    }

}

