// Copyright (c) 2026 shift7 GmbH. All rights reserved.

namespace Cloud.Katta.ViewModels
{
    /// <summary>
    /// Display the recovery key of a new vault.
    /// </summary>
    public class RecoveryKeyViewModel : ViewModelBase
    {
        private bool confirmed;

        public RecoveryKeyViewModel(string recoveryKey)
        {
            RecoveryKey = recoveryKey ?? string.Empty;
        }

        public string RecoveryKey { get; }

        /// <summary>
        /// The user understands the consequences of losing the recovery key.
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

        public bool CanFinish => Confirmed;
    }
}
