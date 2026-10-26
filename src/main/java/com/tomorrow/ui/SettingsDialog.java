package com.tomorrow.ui;

import com.tomorrow.notification.EmailChannel;
import com.tomorrow.notification.InAppChannel;
import com.tomorrow.notification.MacOSChannel;
import com.tomorrow.service.AppSettings;
import com.tomorrow.service.MessageProvider;
import com.tomorrow.service.NotificationManager;
import com.tomorrow.storage.JsonStorage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.concurrent.CompletableFuture;

/**
 * Settings dialog for configuring notification tone, channels, email SMTP,
 * quiet hours, and reminder intervals.
 * <p>
 * Secrets such as SMTP passwords are kept in memory only and never written to settings.json.
 */
public class SettingsDialog extends JDialog {
    private final AppSettings settings;
    private final JsonStorage storage;
    private final NotificationManager notifMgr;
    private final EmailChannel emailChannel;
    private final Runnable onSaved;

    private JComboBox<MessageProvider.Tone> cmbTone;
    private JCheckBox chkInApp;
    private JCheckBox chkMacOS;
    private JCheckBox chkEmail;

    private JTextField txtEmailHost;
    private JSpinner spnEmailPort;
    private JTextField txtEmailUser;
    private JPasswordField txtEmailPass;
    private JTextField txtEmailFrom;
    private JTextField txtEmailRecipient;
    private JCheckBox chkEmailAuth;
    private JCheckBox chkEmailStarttls;
    private JButton btnTestEmail;

    private JCheckBox chkQuietHours;
    private JTextField txtQuietStart;
    private JTextField txtQuietEnd;
    private JSpinner spnInterval;

    public SettingsDialog(Window owner, AppSettings settings, JsonStorage storage,
                          NotificationManager notifMgr, EmailChannel emailChannel, Runnable onSaved) {
        super(owner, "Settings — Tomorrow", ModalityType.APPLICATION_MODAL);
        this.settings = settings;
        this.storage = storage;
        this.notifMgr = notifMgr;
        this.emailChannel = emailChannel;
        this.onSaved = onSaved;

        initializeUI();
        loadValues();
        pack();
        setMinimumSize(new Dimension(520, 600));
        setLocationRelativeTo(owner);
    }

    private void initializeUI() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(12, 16, 12, 16));

        // 1. General & Tone
        JPanel pnlGeneral = new JPanel(new GridLayout(2, 2, 8, 8));
        pnlGeneral.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "General & Procrastination Tone",
                TitledBorder.LEFT, TitledBorder.TOP));

        pnlGeneral.add(new JLabel("Tone:"));
        cmbTone = new JComboBox<>(MessageProvider.Tone.values());
        pnlGeneral.add(cmbTone);

        pnlGeneral.add(new JLabel("Check Interval (minutes):"));
        spnInterval = new JSpinner(new SpinnerNumberModel(15, 1, 1440, 5));
        pnlGeneral.add(spnInterval);

        content.add(pnlGeneral);
        content.add(Box.createVerticalStrut(10));

        // 2. Notification Channels
        JPanel pnlChannels = new JPanel(new GridLayout(3, 1, 4, 4));
        pnlChannels.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Notification Channels",
                TitledBorder.LEFT, TitledBorder.TOP));

        chkInApp = new JCheckBox("In-App Notification Center (Recommended)", true);
        chkMacOS = new JCheckBox("macOS Desktop Notifications (osascript)", false);
        chkEmail = new JCheckBox("Email Daily Digest (SMTP)", false);

        MacOSChannel macTester = new MacOSChannel();
        if (!macTester.isSupported()) {
            chkMacOS.setEnabled(false);
            chkMacOS.setText("macOS Desktop Notifications (Unsupported on this OS)");
        }

        pnlChannels.add(chkInApp);
        pnlChannels.add(chkMacOS);
        pnlChannels.add(chkEmail);

        content.add(pnlChannels);
        content.add(Box.createVerticalStrut(10));

        // 3. Email Settings
        JPanel pnlEmail = new JPanel(new GridBagLayout());
        pnlEmail.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Email Configuration (Opt-in)",
                TitledBorder.LEFT, TitledBorder.TOP));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        pnlEmail.add(new JLabel("SMTP Server:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtEmailHost = new JTextField(15);
        pnlEmail.add(txtEmailHost, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        pnlEmail.add(new JLabel("SMTP Port:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        spnEmailPort = new JSpinner(new SpinnerNumberModel(587, 1, 65535, 1));
        pnlEmail.add(spnEmailPort, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        pnlEmail.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtEmailUser = new JTextField(15);
        pnlEmail.add(txtEmailUser, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        pnlEmail.add(new JLabel("Password / App Token:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtEmailPass = new JPasswordField(15);
        txtEmailPass.setToolTipText("Runtime password only. Never saved to settings.json.");
        pnlEmail.add(txtEmailPass, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        pnlEmail.add(new JLabel("From Address:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtEmailFrom = new JTextField(15);
        pnlEmail.add(txtEmailFrom, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        pnlEmail.add(new JLabel("Recipient Address:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        txtEmailRecipient = new JTextField(15);
        pnlEmail.add(txtEmailRecipient, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JPanel pnlEmailFlags = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chkEmailAuth = new JCheckBox("SMTP Auth", true);
        chkEmailStarttls = new JCheckBox("STARTTLS", true);
        pnlEmailFlags.add(chkEmailAuth);
        pnlEmailFlags.add(Box.createHorizontalStrut(16));
        pnlEmailFlags.add(chkEmailStarttls);
        pnlEmail.add(pnlEmailFlags, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        btnTestEmail = new JButton("Send Test Email");
        btnTestEmail.addActionListener(e -> executeTestEmail());
        pnlEmail.add(btnTestEmail, gbc);

        content.add(pnlEmail);
        content.add(Box.createVerticalStrut(10));

        // 4. Quiet Hours
        JPanel pnlQuiet = new JPanel(new GridLayout(3, 2, 8, 6));
        pnlQuiet.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Quiet Hours (Desktop & Email)",
                TitledBorder.LEFT, TitledBorder.TOP));

        chkQuietHours = new JCheckBox("Enable Quiet Hours", false);
        pnlQuiet.add(chkQuietHours);
        pnlQuiet.add(new JLabel("(In-app notifications remain active)"));

        pnlQuiet.add(new JLabel("Start Time (HH:mm):"));
        txtQuietStart = new JTextField("22:00");
        pnlQuiet.add(txtQuietStart);

        pnlQuiet.add(new JLabel("End Time (HH:mm):"));
        txtQuietEnd = new JTextField("08:00");
        pnlQuiet.add(txtQuietEnd);

        content.add(pnlQuiet);
        content.add(Box.createVerticalStrut(12));

        // Bottom buttons (Save / Cancel)
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Save Settings");
        btnSave.addActionListener(e -> saveSettings());
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());
        pnlButtons.add(btnCancel);
        pnlButtons.add(btnSave);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(pnlButtons, BorderLayout.SOUTH);
    }

    private void loadValues() {
        cmbTone.setSelectedItem(settings.getTone());
        spnInterval.setValue(settings.getReminderCheckIntervalMinutes());

        chkInApp.setSelected(settings.isInAppEnabled());
        chkMacOS.setSelected(settings.isMacOSEnabled());
        chkEmail.setSelected(settings.isEmailEnabled());

        txtEmailHost.setText(settings.getEmailHost() != null ? settings.getEmailHost() : "");
        spnEmailPort.setValue(settings.getEmailPort() > 0 ? settings.getEmailPort() : 587);
        txtEmailUser.setText(settings.getEmailUsername() != null ? settings.getEmailUsername() : "");
        txtEmailFrom.setText(settings.getEmailFrom() != null ? settings.getEmailFrom() : "");
        txtEmailRecipient.setText(settings.getEmailRecipient() != null ? settings.getEmailRecipient() : "");
        chkEmailAuth.setSelected(settings.isEmailAuth());
        chkEmailStarttls.setSelected(settings.isEmailStarttls());

        chkQuietHours.setSelected(settings.isQuietHoursEnabled());
        txtQuietStart.setText(settings.getQuietHoursStart() != null ? settings.getQuietHoursStart().toString() : "22:00");
        txtQuietEnd.setText(settings.getQuietHoursEnd() != null ? settings.getQuietHoursEnd().toString() : "08:00");
    }

    private void executeTestEmail() {
        String host = txtEmailHost.getText().trim();
        int port = (Integer) spnEmailPort.getValue();
        String user = txtEmailUser.getText().trim();
        char[] pass = txtEmailPass.getPassword();
        String from = txtEmailFrom.getText().trim();
        String recipient = txtEmailRecipient.getText().trim();
        boolean auth = chkEmailAuth.isSelected();
        boolean starttls = chkEmailStarttls.isSelected();

        if (host.isEmpty() || from.isEmpty() || recipient.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in SMTP Host, From Address, and Recipient Address first.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnTestEmail.setEnabled(false);
        btnTestEmail.setText("Sending Test Email...");

        CompletableFuture.runAsync(() -> {
            boolean success = false;
            String errorMsg = null;
            try {
                EmailChannel testChannel = new EmailChannel();
                testChannel.setSmtp(host, port, user, pass, from, recipient, auth, starttls);
                success = testChannel.sendTestEmail(recipient);
            } catch (Exception ex) {
                errorMsg = ex.getMessage();
            }

            final boolean finalSuccess = success;
            final String finalError = errorMsg;
            SwingUtilities.invokeLater(() -> {
                btnTestEmail.setEnabled(true);
                btnTestEmail.setText("Send Test Email");
                if (finalSuccess) {
                    JOptionPane.showMessageDialog(SettingsDialog.this,
                            "Test email sent successfully to " + recipient + "!",
                            "Email Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(SettingsDialog.this,
                            "Failed to deliver test email.\n"
                                    + (finalError != null ? "Error: " + finalError : "Please check host, port, auth, or network connection."),
                            "Delivery Failed", JOptionPane.ERROR_MESSAGE);
                }
            });
        });
    }

    private void saveSettings() {
        // Validate quiet hours times
        LocalTime qStart;
        LocalTime qEnd;
        try {
            qStart = LocalTime.parse(txtQuietStart.getText().trim());
            qEnd = LocalTime.parse(txtQuietEnd.getText().trim());
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                    "Invalid time format for quiet hours. Please use HH:mm (e.g. 22:00).",
                    "Invalid Format", JOptionPane.ERROR_MESSAGE);
            return;
        }

        settings.setTone((MessageProvider.Tone) cmbTone.getSelectedItem());
        settings.setReminderCheckIntervalMinutes((Integer) spnInterval.getValue());

        settings.setInAppEnabled(chkInApp.isSelected());
        settings.setMacOSEnabled(chkMacOS.isSelected());
        settings.setEmailEnabled(chkEmail.isSelected());

        settings.setEmailHost(txtEmailHost.getText().trim());
        settings.setEmailPort((Integer) spnEmailPort.getValue());
        settings.setEmailUsername(txtEmailUser.getText().trim());
        settings.setEmailFrom(txtEmailFrom.getText().trim());
        settings.setEmailRecipient(txtEmailRecipient.getText().trim());
        settings.setEmailAuth(chkEmailAuth.isSelected());
        settings.setEmailStarttls(chkEmailStarttls.isSelected());

        settings.setQuietHoursEnabled(chkQuietHours.isSelected());
        settings.setQuietHoursStart(qStart);
        settings.setQuietHoursEnd(qEnd);

        // Update runtime email channel password if provided
        char[] pass = txtEmailPass.getPassword();
        if (pass.length > 0 && emailChannel != null) {
            emailChannel.setPassword(pass);
        }

        // Persist safe settings (no secrets in JSON)
        storage.saveSettings(settings.toMap());

        // Update MessageProvider tone
        if (notifMgr != null && notifMgr.getMessageProvider() != null) {
            notifMgr.getMessageProvider().setTone(settings.getTone());
        }

        // Reconfigure channels in notifMgr
        if (notifMgr != null) {
            notifMgr.clearChannels();
            if (settings.isInAppEnabled()) {
                notifMgr.register(new InAppChannel());
            }
            if (settings.isMacOSEnabled()) {
                MacOSChannel mac = new MacOSChannel();
                mac.setEnabled(true);
                notifMgr.register(mac);
            }
            if (settings.isEmailEnabled()) {
                EmailChannel em = emailChannel != null ? emailChannel : new EmailChannel();
                em.setSmtp(settings.getEmailHost(), settings.getEmailPort(),
                        settings.getEmailUsername(), pass,
                        settings.getEmailFrom(), settings.getEmailRecipient(),
                        settings.isEmailAuth(), settings.isEmailStarttls());
                em.setEnabled(true);
                notifMgr.register(em);
            }
        }

        if (onSaved != null) {
            onSaved.run();
        }

        dispose();
    }
}
