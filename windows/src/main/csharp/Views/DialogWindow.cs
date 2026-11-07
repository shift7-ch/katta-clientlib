// Copyright (c) 2026 shift7 GmbH. All rights reserved.

using System.Windows;

namespace Cloud.Katta.Views
{
    /// <summary>
    /// Base for modal dialogs closing with a result.
    /// </summary>
    public abstract class DialogWindow : Window
    {
        protected DialogWindow()
        {
            ResizeMode = ResizeMode.NoResize;
            SizeToContent = SizeToContent.WidthAndHeight;
            WindowStartupLocation = WindowStartupLocation.CenterScreen;
            ShowInTaskbar = false;
            Topmost = true;
            Loaded += (s, e) => Activate();
        }

        protected void CloseWithResult(bool result)
        {
            DialogResult = result;
            Close();
        }

        protected void OnFinish(object sender, RoutedEventArgs e) => CloseWithResult(true);

        protected void OnCancel(object sender, RoutedEventArgs e) => CloseWithResult(false);
    }
}
