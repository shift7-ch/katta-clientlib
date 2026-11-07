// Copyright (c) 2026 shift7 GmbH. All rights reserved.

using Cloud.Katta.ViewModels;
using Xunit;

namespace Cloud.Katta.Test
{
    public class ViewModelTest
    {
        [Fact]
        public void FirstLoginRequiresConfirmationAndDeviceName()
        {
            var model = new FirstLoginViewModel("key", "computer");
            Assert.False(model.CanFinish);
            model.Confirmed = true;
            Assert.True(model.CanFinish);
            model.DeviceName = "  ";
            Assert.False(model.CanFinish);
            model.DeviceName = " laptop ";
            Assert.Equal("laptop", model.TrimmedDeviceName);
        }

        [Fact]
        public void DeviceSetupRequiresAccountKey()
        {
            var model = new DeviceSetupViewModel("computer");
            Assert.False(model.CanFinish);
            model.AccountKey = " abc ";
            Assert.True(model.CanFinish);
            Assert.Equal("abc", model.TrimmedAccountKey);
        }

        [Fact]
        public void RecoveryKeyRequiresConfirmation()
        {
            var model = new RecoveryKeyViewModel("a b c");
            Assert.False(model.CanFinish);
            model.Confirmed = true;
            Assert.True(model.CanFinish);
        }

        [Fact]
        public void RaisesPropertyChanged()
        {
            var model = new DeviceSetupViewModel(null);
            var changed = string.Empty;
            model.PropertyChanged += (s, e) => changed += e.PropertyName + ";";
            model.AccountKey = "x";
            Assert.Contains("CanFinish", changed);
        }
    }
}
