// Copyright (c) 2026 shift7 GmbH. All rights reserved.

using System.Windows;
using Cloud.Katta.ViewModels;

namespace Cloud.Katta.Views
{
    public partial class RecoveryKeyWindow : DialogWindow
    {
        public RecoveryKeyWindow(RecoveryKeyViewModel viewModel)
        {
            InitializeComponent();
            DataContext = viewModel;
        }

        private void OnCopy(object sender, RoutedEventArgs e) => Clipboard.SetText(((RecoveryKeyViewModel)DataContext).RecoveryKey);
    }
}
