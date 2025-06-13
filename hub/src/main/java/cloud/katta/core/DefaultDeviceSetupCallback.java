/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.core;

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.StringAppender;
import ch.cyberduck.core.exception.LoginCanceledException;

import cloud.katta.model.AccountKeyAndDeviceName;
import cloud.katta.workflows.exceptions.AccessException;

public class DefaultDeviceSetupCallback implements DeviceSetupCallback {

    private final LoginCallback prompt;

    public DefaultDeviceSetupCallback(final LoginCallback prompt) {
        this.prompt = prompt;
    }

    @Override
    public AccountKeyAndDeviceName displayAccountKeyAndAskDeviceName(final Host bookmark, final AccountKeyAndDeviceName accountKeyAndDeviceName) throws AccessException {
        try {
            final Credentials input = prompt.prompt(bookmark, accountKeyAndDeviceName.accountKey(),
                    LocaleFactory.localizedString("Account Key", "Hub"),
                    new StringAppender()
                            .append(LocaleFactory.localizedString("On first login, every user gets a unique Account Key", "Hub"))
                            .append(LocaleFactory.localizedString("Your Account Key is required to login from other apps or browsers", "Hub"))
                            .append(LocaleFactory.localizedString("You can see a list of authorized apps on your profile page", "Hub")).toString(),
                    new LoginOptions()
                            .usernamePlaceholder(LocaleFactory.localizedString("Account Key", "Hub"))
                            // Account key not editable
                            .user(false)
                            .passwordPlaceholder(accountKeyAndDeviceName.deviceName())
                            // Input device name
                            .password(true)
                            .keychain(true)
            );
            return new AccountKeyAndDeviceName()
                    .withAddToKeychain(input.isSaved())
                    .withDeviceName(input.getUsername())
                    .withAccountKey(input.getPassword());
        }
        catch(LoginCanceledException e) {
            throw new AccessException(e);
        }
    }

    @Override
    public AccountKeyAndDeviceName askForAccountKeyAndDeviceName(final Host bookmark, final String initialDeviceName) throws AccessException {
        try {
            final Credentials input = prompt.prompt(bookmark, initialDeviceName,
                    LocaleFactory.localizedString("Authorization Required", "Hub"),
                    new StringAppender()
                            .append(LocaleFactory.localizedString("This is your first login on this device.", "Hub"))
                            .append(LocaleFactory.localizedString("Your Account Key is required to link this browser to your account.", "Hub")).toString(),
                    new LoginOptions()
                            .usernamePlaceholder(LocaleFactory.localizedString("Device Name", "Hub"))
                            // Customize device name
                            .user(true)
                            .passwordPlaceholder(LocaleFactory.localizedString("Account Key", "Hub"))
                            // Input account key
                            .password(true)
                            .keychain(true)
            );
            return new AccountKeyAndDeviceName()
                    .withAddToKeychain(input.isSaved())
                    .withDeviceName(input.getUsername())
                    .withAccountKey(input.getPassword());
        }
        catch(LoginCanceledException e) {
            throw new AccessException(e);
        }
    }
}
