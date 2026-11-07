// Copyright (c) 2026 shift7 GmbH. All rights reserved.

using System.Windows;
using Cloud.Katta.ViewModels;

namespace Cloud.Katta.Views
{
    public partial class FirstLoginWindow : DialogWindow
    {
        public FirstLoginWindow(FirstLoginViewModel viewModel)
        {
            InitializeComponent();
            DataContext = viewModel;
            Loaded += (s, e) => DeviceNameBox.Focus();
        }

        private void OnCopy(object sender, RoutedEventArgs e) => Clipboard.SetText(((FirstLoginViewModel)DataContext).AccountKey);
    }
}
