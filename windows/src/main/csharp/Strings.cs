// Copyright (c) 2026 shift7 GmbH. All rights reserved.

using ch.cyberduck.core;

namespace Cloud.Katta
{
    /// <summary>
    /// Localized strings, shared with the macOS dialogs.
    /// </summary>
    public static class Strings
    {
        private static string Hub(string key) => LocaleFactory.localizedString(key, "Hub");

        public static string AuthorizationRequired => Hub("Authorization Required");
        public static string AccountKey => Hub("Account Key");
        public static string AccountKeyRequired => Hub("Your Account Key is required to authorize this device.");
        public static string DeviceName => Hub("Device Name");
        public static string DeviceNameTooltip => Hub("Name this device for easy identification in your authorized devices list.");
        public static string FinishSetup => Hub("Finish Setup");
        public static string Cancel => LocaleFactory.localizedString("Cancel", "Alert");
        public static string FirstLoginInfo =>
            Hub("On first login, every user gets a unique Account Key") + " "
            + Hub("Your Account Key is required to login from other apps or browsers") + " "
            + Hub("You can see a list of authorized apps on your profile page");
        public static string StoredAccountKey => Hub("I stored my Account Key securely.");
        public static string RecoveryKey => Hub("Recovery Key");
        public static string RecoveryKeyInfo => Hub("The following recovery key can be used to restore access to the vault.");
        public static string RecoveryKeyConfirm =>
            Hub("I understand that I will lose access to the vault in the event of an emergency if I don't have the recovery key.");
        public static string CreateVault => LocaleFactory.localizedString("Create Vault", "Cryptomator");
        public static string Copy => LocaleFactory.localizedString("Copy");
    }
}
