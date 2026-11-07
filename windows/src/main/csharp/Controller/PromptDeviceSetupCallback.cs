// Copyright (c) 2026 shift7 GmbH. All rights reserved.

using System;
using System.Windows;
using ch.cyberduck.core;
using ch.cyberduck.core.exception;
using cloud.katta.core;
using cloud.katta.crypto;
using cloud.katta.workflows.exceptions;
using Cloud.Katta.ViewModels;
using Cloud.Katta.Views;
using org.apache.logging.log4j;
using org.cryptomator.cryptolib.common;

namespace Cloud.Katta.Controller
{
    /// <summary>
    /// Prompt user with WPF dialogs for device setup. Instantiated by <c>DeviceSetupCallbackFactory</c> using the
    /// class name configured in <c>factory.devicesetupcallback.class</c>.
    /// </summary>
    public class PromptDeviceSetupCallback : DeviceSetupCallback
    {
        private static readonly Logger Log = LogManager.getLogger(nameof(PromptDeviceSetupCallback));

        /// <summary>
        /// Constructor looked up by the factory matching the type of the controller.
        /// </summary>
        public PromptDeviceSetupCallback(ch.cyberduck.core.Controller controller)
        {
        }

        public PromptDeviceSetupCallback()
        {
        }

        public DeviceSetupCallback.AccountKeyAndDeviceName displayAccountKeyAndAskDeviceName(Host bookmark, string accountKey)
        {
            Log.debug($"Display account key for {bookmark}");
            return OnDispatcher(() =>
            {
                var model = new FirstLoginViewModel(accountKey, DeviceSetupCallback.AccountKeyAndDeviceName.COMPUTER_NAME);
                Show(new FirstLoginWindow(model));
                return new DeviceSetupCallback.AccountKeyAndDeviceName(accountKey, model.TrimmedDeviceName);
            });
        }

        public DeviceSetupCallback.AccountKeyAndDeviceName askForAccountKeyAndDeviceName(Host bookmark)
        {
            Log.debug($"Ask for account key for {bookmark}");
            return OnDispatcher(() =>
            {
                var model = new DeviceSetupViewModel(DeviceSetupCallback.AccountKeyAndDeviceName.COMPUTER_NAME);
                var message = string.Format(
                                  LocaleFactory.localizedString("This is your first login to {0} on this device.", "Hub"),
                                  BookmarkNameProvider.toHostname(bookmark)) + " "
                              + Strings.AccountKeyRequired;
                Show(new DeviceSetupWindow(model, message));
                return new DeviceSetupCallback.AccountKeyAndDeviceName(model.TrimmedAccountKey, model.TrimmedDeviceName);
            });
        }

        public void displayRecoveryKey(Host bookmark, P384KeyPair recoveryKey)
        {
            Log.debug($"Display recovery key {bookmark}");
            OnDispatcher<object>(() =>
            {
                Show(new RecoveryKeyWindow(new RecoveryKeyViewModel(generateRecoveryKey(recoveryKey))));
                return null;
            });
        }

        public string generateAccountKey() => DeviceSetupCallback.__DefaultMethods.generateAccountKey(this);

        public DeviceKeys generateDeviceKey() => DeviceSetupCallback.__DefaultMethods.generateDeviceKey(this);

        public UserKeys generateUserKeys() => DeviceSetupCallback.__DefaultMethods.generateUserKeys(this);

        public string generateRecoveryKey(P384KeyPair recoveryKey) =>
            DeviceSetupCallback.__DefaultMethods.generateRecoveryKey(this, recoveryKey);

        /// <exception cref="AccessException">Dialog canceled</exception>
        private static void Show(Window window)
        {
            if (window.ShowDialog() != true)
            {
                throw new AccessException(new ConnectionCanceledException());
            }
        }

        private static T OnDispatcher<T>(Func<T> action)
        {
            var dispatcher = Application.Current.Dispatcher;
            return dispatcher.CheckAccess() ? action() : dispatcher.Invoke(action);
        }
    }
}
