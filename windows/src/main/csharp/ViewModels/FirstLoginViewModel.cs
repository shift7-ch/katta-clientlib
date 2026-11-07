// Copyright (c) 2026 shift7 GmbH. All rights reserved.

namespace Cloud.Katta.ViewModels
{
    /// <summary>
    /// Display the generated account key and ask for the device name.
    /// </summary>
    public class FirstLoginViewModel : ViewModelBase
    {
        private string deviceName;
        private bool confirmed;

        public FirstLoginViewModel(string accountKey, string deviceName)
        {
            AccountKey = accountKey ?? string.Empty;
            this.deviceName = deviceName ?? string.Empty;
        }

        public string AccountKey { get; }

        public string DeviceName
        {
            get => deviceName;
            set
            {
                if (Set(ref deviceName, value))
                {
                    Raise(nameof(CanFinish));
                }
            }
        }

        /// <summary>
        /// The user stored the account key securely.
        /// </summary>
        public bool Confirmed
        {
            get => confirmed;
            set
            {
                if (Set(ref confirmed, value))
                {
                    Raise(nameof(CanFinish));
                }
            }
        }

        public bool CanFinish => Confirmed && !string.IsNullOrWhiteSpace(DeviceName);

        public string TrimmedDeviceName => (DeviceName ?? string.Empty).Trim();
    }
}
