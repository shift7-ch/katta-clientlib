// Copyright (c) 2026 shift7 GmbH. All rights reserved.

using System.Windows;
using Cloud.Katta.ViewModels;

namespace Cloud.Katta.Views
{
    public partial class DeviceSetupWindow : DialogWindow
    {
        public DeviceSetupWindow(DeviceSetupViewModel viewModel, string message)
        {
            InitializeComponent();
            DataContext = viewModel;
            Message.Text = message;
            Loaded += (s, e) => AccountKeyBox.Focus();
        }

        private void OnAccountKeyChanged(object sender, RoutedEventArgs e) =>
            ((DeviceSetupViewModel)DataContext).AccountKey = AccountKeyBox.Password;
    }
}
