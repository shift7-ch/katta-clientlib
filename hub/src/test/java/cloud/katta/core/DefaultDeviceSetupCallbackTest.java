/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.core;

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.LoginCallback;
import ch.cyberduck.core.LoginOptions;

import org.junit.jupiter.api.Test;

import cloud.katta.core.DeviceSetupCallback.AccountKeyAndDeviceName;
import cloud.katta.protocols.hub.HubProtocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultDeviceSetupCallbackTest {

    private static final String ACCOUNT_KEY = "ABCD-1234-EFGH-5678";
    private static final String DEVICE_NAME = "MacBook Pro";

    /**
     * Stubs the login prompt so that the value entered into the field labelled "Account Key" is always
     * {@link #ACCOUNT_KEY} and the value entered into the field labelled "Device Name" is always
     * {@link #DEVICE_NAME}, regardless of which credential field (username/password) carries which label.
     */
    private static LoginCallback promptMappingByPlaceholder() throws Exception {
        final String accountKeyLabel = LocaleFactory.localizedString("Account Key", "Hub");
        final LoginCallback prompt = mock(LoginCallback.class);
        when(prompt.prompt(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            final LoginOptions options = invocation.getArgument(4);
            final String username = accountKeyLabel.equals(options.getUsernamePlaceholder()) ? ACCOUNT_KEY : DEVICE_NAME;
            final String password = accountKeyLabel.equals(options.getPasswordPlaceholder()) ? ACCOUNT_KEY : DEVICE_NAME;
            return new Credentials(username, password);
        });
        return prompt;
    }

    @Test
    void testAskForAccountKeyAndDeviceNameMapping() throws Exception {
        final DefaultDeviceSetupCallback callback = new DefaultDeviceSetupCallback(promptMappingByPlaceholder());
        final AccountKeyAndDeviceName result = callback.askForAccountKeyAndDeviceName(new Host(new HubProtocol()));
        assertEquals(ACCOUNT_KEY, result.accountKey());
        assertEquals(DEVICE_NAME, result.deviceName());
    }

    @Test
    void testDisplayAccountKeyAndAskDeviceNameMapping() throws Exception {
        final DefaultDeviceSetupCallback callback = new DefaultDeviceSetupCallback(promptMappingByPlaceholder());
        final AccountKeyAndDeviceName result = callback.displayAccountKeyAndAskDeviceName(new Host(new HubProtocol()), ACCOUNT_KEY);
        assertEquals(ACCOUNT_KEY, result.accountKey());
        assertEquals(DEVICE_NAME, result.deviceName());
    }
}
