// Copyright (c) 2026 shift7 GmbH. All rights reserved.

namespace Cloud.Katta.ViewModels
{
    /// <summary>
    /// Ask for the existing account key and the device name.
    /// </summary>
    public class DeviceSetupViewModel : ViewModelBase
    {
        private string accountKey;
        private string deviceName;

        public DeviceSetupViewModel(string deviceName)
        {
            accountKey = string.Empty;
            this.deviceName = deviceName ?? string.Empty;
        }

        public string AccountKey
        {
            get => accountKey;
            set
            {
                if (Set(ref accountKey, value))
                {
                    Raise(nameof(CanFinish));
                }
            }
        }

        public string DeviceName
        {
            get => deviceName;
            set => Set(ref deviceName, value);
        }

        public bool CanFinish => !string.IsNullOrWhiteSpace(AccountKey);

        public string TrimmedAccountKey => (AccountKey ?? string.Empty).Trim();

        public string TrimmedDeviceName => (DeviceName ?? string.Empty).Trim();
    }
}
