package com.tomorrow.service;

import java.time.LocalTime;
import java.util.*;

/**
 * Application settings. Secrets (SMTP password) are intentionally not stored here.
 */
public class AppSettings {
    private MessageProvider.Tone tone = MessageProvider.Tone.HUMOROUS;
    private boolean inAppEnabled = true;
    private boolean macOSEnabled = false;
    private boolean emailEnabled = false;
    private String emailHost;
    private int emailPort = 587;
    private String emailUsername;
    private String emailFrom;
    private String emailRecipient;
    private boolean emailAuth = true;
    private boolean emailStarttls = true;
    private boolean quietHoursEnabled = false;
    private LocalTime quietHoursStart = LocalTime.of(22, 0);
    private LocalTime quietHoursEnd = LocalTime.of(8, 0);
    private int reminderCheckIntervalMinutes = 15;

    public MessageProvider.Tone getTone() { return tone; }
    public void setTone(MessageProvider.Tone tone) { this.tone = tone; }

    public boolean isInAppEnabled() { return inAppEnabled; }
    public void setInAppEnabled(boolean inAppEnabled) { this.inAppEnabled = inAppEnabled; }

    public boolean isMacOSEnabled() { return macOSEnabled; }
    public void setMacOSEnabled(boolean macOSEnabled) { this.macOSEnabled = macOSEnabled; }

    public boolean isEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }

    public String getEmailHost() { return emailHost; }
    public void setEmailHost(String emailHost) { this.emailHost = emailHost; }

    public int getEmailPort() { return emailPort; }
    public void setEmailPort(int emailPort) { this.emailPort = emailPort; }

    public String getEmailUsername() { return emailUsername; }
    public void setEmailUsername(String emailUsername) { this.emailUsername = emailUsername; }

    public String getEmailFrom() { return emailFrom; }
    public void setEmailFrom(String emailFrom) { this.emailFrom = emailFrom; }

    public String getEmailRecipient() { return emailRecipient; }
    public void setEmailRecipient(String emailRecipient) { this.emailRecipient = emailRecipient; }

    public boolean isEmailAuth() { return emailAuth; }
    public void setEmailAuth(boolean emailAuth) { this.emailAuth = emailAuth; }

    public boolean isEmailStarttls() { return emailStarttls; }
    public void setEmailStarttls(boolean emailStarttls) { this.emailStarttls = emailStarttls; }

    public boolean isQuietHoursEnabled() { return quietHoursEnabled; }
    public void setQuietHoursEnabled(boolean quietHoursEnabled) { this.quietHoursEnabled = quietHoursEnabled; }

    public LocalTime getQuietHoursStart() { return quietHoursStart; }
    public void setQuietHoursStart(LocalTime quietHoursStart) { this.quietHoursStart = quietHoursStart; }

    public LocalTime getQuietHoursEnd() { return quietHoursEnd; }
    public void setQuietHoursEnd(LocalTime quietHoursEnd) { this.quietHoursEnd = quietHoursEnd; }

    public int getReminderCheckIntervalMinutes() { return reminderCheckIntervalMinutes; }
    public void setReminderCheckIntervalMinutes(int reminderCheckIntervalMinutes) {
        this.reminderCheckIntervalMinutes = reminderCheckIntervalMinutes;
    }

    public boolean inQuietHours(LocalTime now) {
        if (!quietHoursEnabled) return false;
        LocalTime start = quietHoursStart;
        LocalTime end = quietHoursEnd;
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        // overnight span, e.g. 22:00-08:00
        return !now.isBefore(start) || now.isBefore(end);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tone", tone.name());
        m.put("inAppEnabled", inAppEnabled);
        m.put("macOSEnabled", macOSEnabled);
        m.put("emailEnabled", emailEnabled);
        m.put("emailHost", emailHost);
        m.put("emailPort", emailPort);
        m.put("emailUsername", emailUsername);
        m.put("emailFrom", emailFrom);
        m.put("emailRecipient", emailRecipient);
        m.put("emailAuth", emailAuth);
        m.put("emailStarttls", emailStarttls);
        m.put("quietHoursEnabled", quietHoursEnabled);
        m.put("quietHoursStart", quietHoursStart.toString());
        m.put("quietHoursEnd", quietHoursEnd.toString());
        m.put("reminderCheckIntervalMinutes", reminderCheckIntervalMinutes);
        return m;
    }

    public static AppSettings fromMap(Map<String, Object> m) {
        AppSettings s = new AppSettings();
        if (m == null) return s;
        Object v;
        if ((v = m.get("tone")) != null) s.setTone(MessageProvider.Tone.valueOf(String.valueOf(v)));
        if ((v = m.get("inAppEnabled")) != null) s.setInAppEnabled(Boolean.parseBoolean(String.valueOf(v)));
        if ((v = m.get("macOSEnabled")) != null) s.setMacOSEnabled(Boolean.parseBoolean(String.valueOf(v)));
        if ((v = m.get("emailEnabled")) != null) s.setEmailEnabled(Boolean.parseBoolean(String.valueOf(v)));
        if ((v = m.get("emailHost")) != null) s.setEmailHost(String.valueOf(v));
        if ((v = m.get("emailPort")) != null) s.setEmailPort(Integer.parseInt(String.valueOf(v)));
        if ((v = m.get("emailUsername")) != null) s.setEmailUsername(String.valueOf(v));
        if ((v = m.get("emailFrom")) != null) s.setEmailFrom(String.valueOf(v));
        if ((v = m.get("emailRecipient")) != null) s.setEmailRecipient(String.valueOf(v));
        if ((v = m.get("emailAuth")) != null) s.setEmailAuth(Boolean.parseBoolean(String.valueOf(v)));
        if ((v = m.get("emailStarttls")) != null) s.setEmailStarttls(Boolean.parseBoolean(String.valueOf(v)));
        if ((v = m.get("quietHoursEnabled")) != null) s.setQuietHoursEnabled(Boolean.parseBoolean(String.valueOf(v)));
        if ((v = m.get("quietHoursStart")) != null) s.setQuietHoursStart(LocalTime.parse(String.valueOf(v)));
        if ((v = m.get("quietHoursEnd")) != null) s.setQuietHoursEnd(LocalTime.parse(String.valueOf(v)));
        if ((v = m.get("reminderCheckIntervalMinutes")) != null) {
            s.setReminderCheckIntervalMinutes(Integer.parseInt(String.valueOf(v)));
        }
        return s;
    }
}
