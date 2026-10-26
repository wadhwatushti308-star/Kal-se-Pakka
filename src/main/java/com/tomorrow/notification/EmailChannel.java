package com.tomorrow.notification;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.*;

/**
 * Email channel over SMTP. Credentials are passed at runtime (never stored in JSON).
 * Sends are meant to be performed off the Swing EDT by the caller/scheduler.
 */
public class EmailChannel implements NotificationChannel {
    private final Properties props = new Properties();
    private String host;
    private int port = 587;
    private String username;
    private char[] password;
    private String from;
    private String recipient;
    private boolean enabled = false;
    private boolean auth = true;
    private boolean starttls = true;

    public void setSmtp(String host, int port, String username, char[] password,
                        String from, String recipient, boolean auth, boolean starttls) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password == null ? new char[0] : password.clone();
        this.from = from;
        this.recipient = recipient;
        this.auth = auth;
        this.starttls = starttls;
        props.clear();
        if (host != null && !host.isBlank()) {
            props.put("mail.smtp.host", host);
        }
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
    }

    public void setPassword(char[] password) {
        this.password = password == null ? new char[0] : password.clone();
    }

    public void clearPassword() {
        if (password != null) {
            Arrays.fill(password, ' ');
        }
    }

    @Override
    public String name() {
        return "email";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isConfigured() {
        return host != null && !host.isBlank()
                && recipient != null && !recipient.isBlank()
                && from != null && !from.isBlank();
    }

    public boolean sendTestEmail(String targetRecipient) {
        Notification testNotif = new Notification();
        testNotif.setTitle("Test Email");
        testNotif.setMessage("This is a test notification from Tomorrow. Your SMTP settings are working properly!");
        String origRecipient = this.recipient;
        boolean wasEnabled = this.enabled;
        try {
            if (targetRecipient != null && !targetRecipient.isBlank()) {
                this.recipient = targetRecipient.trim();
            }
            this.enabled = true;
            return send(testNotif);
        } finally {
            this.recipient = origRecipient;
            this.enabled = wasEnabled;
        }
    }

    public boolean send(Notification notification) {
        if (!enabled || !isConfigured()) {
            notification.setChannelStatus(name(), false);
            return false;
        }
        Session session = auth
                ? Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, new String(password));
            }
        })
                : Session.getInstance(props);
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            msg.setSubject("[Tomorrow] " + (notification.getTitle() != null ? notification.getTitle() : "Notification"));
            msg.setText(notification.getMessage() != null ? notification.getMessage() : "");
            msg.setSentDate(new Date());
            Transport.send(msg);
            notification.setChannelStatus(name(), true);
            return true;
        } catch (MessagingException e) {
            notification.setChannelStatus(name(), false);
            return false;
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getFrom() {
        return from;
    }

    public String getRecipient() {
        return recipient;
    }

    public boolean isAuth() {
        return auth;
    }

    public boolean isStarttls() {
        return starttls;
    }
}
