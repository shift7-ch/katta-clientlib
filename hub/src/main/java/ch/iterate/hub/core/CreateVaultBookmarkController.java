/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core;

import ch.cyberduck.binding.Action;
import ch.cyberduck.binding.Outlet;
import ch.cyberduck.binding.SheetController;
import ch.cyberduck.binding.application.NSButton;
import ch.cyberduck.binding.application.NSImage;
import ch.cyberduck.binding.application.NSImageView;
import ch.cyberduck.binding.application.NSPopUpButton;
import ch.cyberduck.binding.application.NSTextField;
import ch.cyberduck.binding.application.SheetCallback;
import ch.cyberduck.binding.foundation.NSAttributedString;
import ch.cyberduck.core.Controller;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.StringAppender;
import ch.cyberduck.core.resources.IconCacheFactory;
import ch.cyberduck.core.threading.AbstractBackgroundAction;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rococoa.Foundation;
import org.rococoa.ID;
import org.rococoa.cocoa.foundation.NSPoint;
import org.rococoa.cocoa.foundation.NSRect;
import org.rococoa.cocoa.foundation.NSSize;

import java.util.List;
import java.util.UUID;

import ch.iterate.hub.client.model.StorageProfileS3Dto;
import ch.iterate.hub.core.callback.CreateVaultModel;
import ch.iterate.hub.model.StorageProfileDtoWrapper;


/**
 * Fetch user input for vault bookmark creation.
 */
public class CreateVaultBookmarkController extends SheetController {

    private static final Logger log = LogManager.getLogger(CreateVaultBookmarkController.class.getName());


    private final String title_;
    private final String reason_;
    private final String icon_;
    private final String vaultNameLabel_;
    private final String vaultDescriptionLabel_;
    private final String backendLabel_;
    private final String regionLabel_;
    private final String bucketNameLabel_;
    private final String accessKeyIdLabel_;
    private final String secretKeyLabel_;
    private final String maxWotLevelLabel_;

    private final CreateVaultModel model;
    private final CreateVaultBookmarkAction.Callback callback;

    private final Controller controller;

    private final List<StorageProfileDtoWrapper> storageProfiles;

    @Outlet
    private NSImageView iconView;
    @Outlet
    private NSTextField titleField;
    @Outlet
    private NSTextField messageField;
    @Outlet
    private NSTextField vaultNameLabel;
    @Outlet
    private NSTextField vaultNameField;
    @Outlet
    private NSTextField vaultDescriptionLabel;
    @Outlet
    private NSTextField vaultDescriptionField;
    @Outlet
    private NSTextField backendLabel;
    @Outlet
    private NSPopUpButton backendCombobox;
    @Outlet
    private NSTextField regionLabel;
    @Outlet
    private NSPopUpButton regionCombobox;
    @Outlet
    private NSTextField bucketNameLabel;
    @Outlet
    private NSTextField bucketNameField;
    @Outlet
    private NSTextField accessKeyIdLabel;
    @Outlet
    private NSTextField accessKeyIdField;
    @Outlet
    private NSTextField secretKeyLabel;
    @Outlet
    private NSTextField secretKeyField;
    @Outlet
    private NSTextField automaticAccessGrantCheckboxLabel;
    @Outlet
    private NSButton automaticAccessGrantCheckbox;
    @Outlet
    private NSTextField maxWotLevelLabel;
    @Outlet
    private NSTextField maxWotLevel;
    @Outlet
    private NSButton helpButton;
    @Outlet
    private NSButton cancelButton;
    @Outlet
    private NSButton createVaultButton;

    public CreateVaultBookmarkController(final List<StorageProfileDtoWrapper> storageProfiles, final Controller controller, final CreateVaultModel model, final CreateVaultBookmarkAction.Callback callback) {
        this.model = model;
        this.callback = callback;
        this.title_ = LocaleFactory.localizedString("Create Vault", "Cipherduck");
        if(null != model.reason()) {
            this.reason_ = model.reason();
        }
        else {
            this.reason_ = LocaleFactory.localizedString("Enter a name and description for your new vault. You can change these later.", "Cipherduck");
        }
        this.vaultNameLabel_ = LocaleFactory.localizedString("Vault Name", "Cipherduck");
        this.vaultDescriptionLabel_ = LocaleFactory.localizedString("Description (optional)", "Cipherduck");
        this.backendLabel_ = LocaleFactory.localizedString("Vault storage location", "Cipherduck");
        this.regionLabel_ = LocaleFactory.localizedString("Region", "Cipherduck");
        this.icon_ = "cryptomator.tiff";
        this.storageProfiles = storageProfiles;
        this.bucketNameLabel_ = LocaleFactory.localizedString("Bucket Name", "Cipherduck");
        this.accessKeyIdLabel_ = LocaleFactory.localizedString("Access Key ID", "Cipherduck");
        this.secretKeyLabel_ = LocaleFactory.localizedString("Secret Key", "Cipherduck");
        this.maxWotLevelLabel_ = LocaleFactory.localizedString("Max WoT Level", "Cipherduck");
        this.controller = controller;
    }

    @Override
    public void awakeFromNib() {
        super.awakeFromNib();
        window.makeFirstResponder(titleField);
        updateRegions();
    }

    @Override
    protected String getBundleName() {
        return "CreateVault";
    }

    public void setIconView(NSImageView iconView) {
        this.iconView = iconView;
        this.iconView.setImage(IconCacheFactory.<NSImage>get().iconNamed(this.icon_, 64));
    }

    public void setTitleField(NSTextField titleField) {
        this.titleField = titleField;
        this.updateField(this.titleField, LocaleFactory.localizedString(this.title_, "Cipherduck"));
    }

    public void setMessageField(NSTextField messageField) {
        this.messageField = messageField;
        this.messageField.setSelectable(true);
        this.updateField(this.messageField, new StringAppender().append(reason_).toString());
    }

    public void setVaultNameLabel(final NSTextField f) {
        this.vaultNameLabel = f;
        this.vaultNameLabel.setAttributedStringValue(NSAttributedString.attributedStringWithAttributes(
                String.format("%s:", this.vaultNameLabel_),
                TRUNCATE_TAIL_ATTRIBUTES
        ));
    }

    public void setVaultNameField(final NSTextField f) {
        this.vaultNameField = f;
        this.vaultNameField.setStringValue(this.model.vaultName());
    }

    public void setVaultDescriptionLabel(final NSTextField f) {
        this.vaultDescriptionLabel = f;
        vaultDescriptionLabel.setAttributedStringValue(NSAttributedString.attributedStringWithAttributes(
                String.format("%s:", this.vaultDescriptionLabel_),
                TRUNCATE_TAIL_ATTRIBUTES
        ));
    }

    public void setVaultDescriptionField(final NSTextField f) {
        this.vaultDescriptionField = f;
    }

    public void setBackendLabel(final NSTextField f) {
        this.backendLabel = f;
        backendLabel.setAttributedStringValue(NSAttributedString.attributedStringWithAttributes(
                String.format("%s:", this.backendLabel_),
                TRUNCATE_TAIL_ATTRIBUTES
        ));
    }

    public void setBackendCombobox(final NSPopUpButton b) {
        this.backendCombobox = b;
        this.backendCombobox.removeAllItems();
        this.backendCombobox.setTarget(this.id());
        this.backendCombobox.setAction(Foundation.selector("backendComboboxClicked:"));
        for(final StorageProfileDtoWrapper backend : storageProfiles) {
            this.backendCombobox.addItemWithTitle(backend.getName());
            this.backendCombobox.lastItem().setRepresentedObject(backend.getId().toString());
        }
        if(StringUtils.isNotBlank(this.model.backend())) {
            this.backendCombobox.selectItemAtIndex(this.backendCombobox.indexOfItemWithRepresentedObject(this.model.backend()));
        }
    }

    @Action
    public void backendComboboxClicked(NSPopUpButton sender) {
        LocaleFactory.get().setDefault(sender.selectedItem().representedObject());
        updateRegions();
    }

    private void updateRegions() {
        synchronized(storageProfiles) {
            if(regionCombobox == null) {
                return;
            }
            if(backendCombobox == null) {
                return;
            }
            final String selectedStorageId = this.backendCombobox.selectedItem().representedObject();
            final StorageProfileDtoWrapper config = storageProfiles.stream().filter(c -> c.getId().toString().equals(selectedStorageId)).findFirst().get();

            final List<String> regions = config.getRegions();
            if(null != regions) {
                for(final String region : regions) {
                    this.regionCombobox.addItemWithTitle(LocaleFactory.localizedString(region, "S3"));
                    this.regionCombobox.lastItem().setRepresentedObject(region);
                    if(config.getRegion().equals(region)) {
                        regionCombobox.selectItem(this.regionCombobox.lastItem());
                    }
                }
            }
            final boolean isPermanent = config.getType().equals(StorageProfileS3Dto.class);
            final boolean hiddenIfSTS = !isPermanent;
            final boolean hiddenIfPermanent = isPermanent;
            bucketNameLabel.setHidden(hiddenIfSTS);
            bucketNameField.setHidden(hiddenIfSTS);
            accessKeyIdLabel.setHidden(hiddenIfSTS);
            accessKeyIdField.setHidden(hiddenIfSTS);
            secretKeyLabel.setHidden(hiddenIfSTS);
            secretKeyField.setHidden(hiddenIfSTS);
            regionCombobox.setHidden(hiddenIfPermanent);
            regionLabel.setHidden(hiddenIfPermanent);
            final NSRect frame = window.frame();
            double height = frame.size.height.doubleValue();
            final int ROW_HEIGHT = 25;
            // isPermanent -> hide region row (1)
            // STS -> hide Access Key ID/Secret Key/Bucket Name (3)
            height = !isPermanent ? 369.0 - 3 * ROW_HEIGHT : 369.0 - 1 * ROW_HEIGHT;
            double width = frame.size.width.doubleValue();
            window.setFrame_display_animate(new NSRect(frame.origin, new NSSize(width, height)), true, true);
            // set the bottom row elements relative to new window frame after resizing:
            bucketNameLabel.setFrameOrigin(new NSPoint(bucketNameLabel.frame().origin.x.doubleValue(), 62 + ROW_HEIGHT));
            bucketNameField.setFrameOrigin(new NSPoint(bucketNameField.frame().origin.x.doubleValue(), 60 + ROW_HEIGHT));
            secretKeyLabel.setFrameOrigin(new NSPoint(secretKeyLabel.frame().origin.x.doubleValue(), 87 + ROW_HEIGHT));
            secretKeyField.setFrameOrigin(new NSPoint(secretKeyField.frame().origin.x.doubleValue(), 85 + ROW_HEIGHT));
            accessKeyIdLabel.setFrameOrigin(new NSPoint(accessKeyIdLabel.frame().origin.x.doubleValue(), 112 + ROW_HEIGHT));
            accessKeyIdField.setFrameOrigin(new NSPoint(accessKeyIdField.frame().origin.x.doubleValue(), 110 + ROW_HEIGHT));
            automaticAccessGrantCheckbox.setFrameOrigin(new NSPoint(automaticAccessGrantCheckbox.frame().origin.x.doubleValue(), 35 + ROW_HEIGHT));
            maxWotLevel.setFrameOrigin(new NSPoint(maxWotLevel.frame().origin.x.doubleValue(), 35));
            maxWotLevelLabel.setFrameOrigin(new NSPoint(maxWotLevelLabel.frame().origin.x.doubleValue(), 35));
            helpButton.setFrameOrigin(new NSPoint(helpButton.frame().origin.x.doubleValue(), 5 + 4));
            cancelButton.setFrameOrigin(new NSPoint(cancelButton.frame().origin.x.doubleValue(), 5));
            createVaultButton.setFrameOrigin(new NSPoint(createVaultButton.frame().origin.x.doubleValue(), 5));
        }
    }

    public void setRegionLabel(final NSTextField f) {
        this.regionLabel = f;
        this.regionLabel.setAttributedStringValue(NSAttributedString.attributedStringWithAttributes(
                String.format("%s:", this.regionLabel_),
                TRUNCATE_TAIL_ATTRIBUTES
        ));
    }


    public void setRegionCombobox(final NSPopUpButton b) {
        this.regionCombobox = b;
        if(StringUtils.isNotBlank(this.model.region())) {
            this.regionCombobox.selectItemAtIndex(this.regionCombobox.indexOfItemWithRepresentedObject(this.model.region()));
        }
    }

    public void setBucketNameLabel(final NSTextField BucketNameLabel) {
        this.bucketNameLabel = BucketNameLabel;
        BucketNameLabel.setAttributedStringValue(NSAttributedString.attributedStringWithAttributes(
                String.format("%s:", this.bucketNameLabel_),
                TRUNCATE_TAIL_ATTRIBUTES
        ));
    }

    public void setBucketNameField(final NSTextField f) {
        this.bucketNameField = f;
        this.bucketNameField.setStringValue(this.model.bucketName());
    }

    public void setAccessKeyIdLabel(final NSTextField f) {
        this.accessKeyIdLabel = f;
        f.setAttributedStringValue(NSAttributedString.attributedStringWithAttributes(
                String.format("%s:", this.accessKeyIdLabel_),
                TRUNCATE_TAIL_ATTRIBUTES
        ));
        this.accessKeyIdField.setStringValue(this.model.accessKeyId());
    }

    public void setAccessKeyIdField(final NSTextField f) {
        this.accessKeyIdField = f;
    }

    public void setMaxWotLevelLabel(final NSTextField f) {
        this.maxWotLevelLabel = f;
        f.setAttributedStringValue(NSAttributedString.attributedStringWithAttributes(
                String.format("%s:", this.maxWotLevelLabel_),
                TRUNCATE_TAIL_ATTRIBUTES
        ));

    }

    public void setMaxWotLevelField(final NSTextField f) {
        this.maxWotLevel = f;
        this.maxWotLevel.setStringValue(String.valueOf(this.model.maxWotLevel()));
    }

    public void setAutomaticAccessGrantCheckbox(final NSButton b) {
        this.automaticAccessGrantCheckbox = b;
        this.automaticAccessGrantCheckbox.setState(this.model.automaticAccessGrant() ? 1 : 0);

    }

    public void setSecretKeyLabel(final NSTextField f) {
        this.secretKeyLabel = f;
        f.setAttributedStringValue(NSAttributedString.attributedStringWithAttributes(
                String.format("%s:", this.secretKeyLabel_),
                TRUNCATE_TAIL_ATTRIBUTES
        ));
        this.secretKeyField.setStringValue(this.model.secretKey());
    }

    public void setSecretKeyField(final NSTextField f) {
        this.secretKeyField = f;
    }

    public void setHelpButton(final NSButton b) {
        this.helpButton = b;
    }

    public void setCancelButton(final NSButton b) {
        this.cancelButton = b;
        this.cancelButton.setTarget(this.id());
        this.cancelButton.setAction(Foundation.selector("closeSheet:"));
    }

    public void setCreateVaultButton(final NSButton b) {
        this.createVaultButton = b;
        this.createVaultButton.setTarget(this.id());
        this.createVaultButton.setAction(Foundation.selector("closeSheet:"));
    }

    @Override
    public boolean validate(final int returncode) {
        if(StringUtils.isBlank(this.vaultNameField.stringValue())) {
            return false;
        }
        final String selectedStorageId = this.backendCombobox.selectedItem().representedObject();
        final StorageProfileDtoWrapper config = storageProfiles.stream().filter(c -> c.getId().toString().equals(selectedStorageId)).findFirst().get();
        final boolean isPermanent = config.getType().equals(StorageProfileS3Dto.class);
        if(isPermanent) {
            if(StringUtils.isBlank(this.accessKeyIdField.stringValue())) {
                return false;
            }
            if(StringUtils.isBlank(this.secretKeyField.stringValue())) {
                return false;
            }
            if(StringUtils.isBlank(this.bucketNameField.stringValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void cancel(final ID sender) {
        window.close();
    }

    @Override
    public void callback(final int returncode) {
        if(returncode != SheetCallback.DEFAULT_OPTION) {
            return;
        }
        controller.background(new AbstractBackgroundAction<Void>() {
            @Override
            public Void run() {
                final CreateVaultModel m = new CreateVaultModel(UUID.randomUUID(), null, vaultNameField.stringValue(),
                        vaultDescriptionField.stringValue(),
                        backendCombobox.selectedItem().representedObject(),
                        StringUtils.isNotBlank(accessKeyIdField.stringValue()) ? accessKeyIdField.stringValue() : null,
                        StringUtils.isNotBlank(secretKeyField.stringValue()) ? secretKeyField.stringValue() : null,
                        StringUtils.isNotBlank(bucketNameField.stringValue()) ? bucketNameField.stringValue() : null,
                        regionCombobox.selectedItem().representedObject(),
                        automaticAccessGrantCheckbox.integerValue() == 1,
                        StringUtils.isNotBlank(maxWotLevel.stringValue()) ? Integer.parseInt(maxWotLevel.stringValue()) : 0
                );
                callback.callback(m);
                return null;
            }
        });
    }
}

