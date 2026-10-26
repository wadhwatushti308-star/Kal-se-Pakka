package com.tomorrow.ui;

import com.tomorrow.service.TaskManager;
import com.tomorrow.service.NotificationManager;
import com.tomorrow.service.AppSettings;
import com.tomorrow.service.MessageProvider;
import com.tomorrow.storage.JsonStorage;
import com.tomorrow.notification.EmailChannel;
import com.tomorrow.notification.InAppChannel;
import com.tomorrow.notification.MacOSChannel;
import java.nio.file.Path;

import javax.swing.*;
import java.awt.*;

/**
 * Minimal main entry point. Opens the main frame on the EDT.
 */
public class Main {
    public static void main(String[] args) {
        // Setup Nimbus/FlatLaf L&F early
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarwinLookAndFeel");
        } catch (Exception e) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception e2) { /* ignore */ }
        }

        SwingUtilities.invokeLater(() -> {
try {
                Path dataDir = Path.of(System.getProperty("user.home"), ".local", "share", "tomorrow");
                JsonStorage storage = new JsonStorage(dataDir);
                TaskManager taskManager = new TaskManager(storage);
                AppSettings settings = AppSettings.fromMap(storage.loadSettings());
                if (settings == null) {
                    settings = new AppSettings();
                }
                NotificationManager notifMgr = new NotificationManager(storage,
                        new MessageProvider(settings.getTone()));
                // register channels from settings
                if (settings.isInAppEnabled()) notifMgr.register(new InAppChannel());
                if (settings.isMacOSEnabled()) notifMgr.register(new MacOSChannel());
                if (settings.isEmailEnabled()) {
                    EmailChannel email = new EmailChannel();
                    email.setSmtp(settings.getEmailHost(), settings.getEmailPort(),
                            settings.getEmailUsername(), new char[0],
                            settings.getEmailFrom(), settings.getEmailRecipient(),
                            settings.isEmailAuth(), settings.isEmailStarttls());
                    notifMgr.register(email);
                }

                MainFrame frame = new MainFrame(taskManager, notifMgr, settings);
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Fatal error starting Tomorrow: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}