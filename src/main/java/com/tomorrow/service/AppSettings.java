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
        if (!quietHoursEnabled || now == null) return false;
        LocalTime start = quietHoursStart != null ? quietHoursStart : LocalTime.of(22, 0);
        LocalTime end = quietHoursEnd != null ? quietHoursEnd : LocalTime.of(8, 0);
        if (start.equals(end)) return true;
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
        m.put("emailHost", emailHost != null ? emailHost : "");
        m.put("emailPort", emailPort);
        m.put("emailUsername", emailUsername != null ? emailUsername : "");
        m.put("emailFrom", emailFrom != null ? emailFrom : "");
        m.put("emailRecipient", emailRecipient != null ? emailRecipient : "");
        m.put("emailAuth", emailAuth);
        m.put("emailStarttls", emailStarttls);
        m.put("quietHoursEnabled", quietHoursEnabled);
        m.put("quietHoursStart", quietHoursStart != null ? quietHoursStart.toString() : "22:00");
        m.put("quietHoursEnd", quietHoursEnd != null ? quietHoursEnd.toString() : "08:00");
        m.put("reminderCheckIntervalMinutes", reminderCheckIntervalMinutes);
        return m;
    }

    public static AppSettings fromMap(Map<String, Object> m) {
        AppSettings s = new AppSettings();
        if (m == null || m.isEmpty()) return s;
        if (m.containsKey("tone")) s.setTone(parseToneSafe(m.get("tone"), MessageProvider.Tone.HUMOROUS));
        if (m.containsKey("inAppEnabled")) s.setInAppEnabled(parseBooleanSafe(m.get("inAppEnabled"), true));
        if (m.containsKey("macOSEnabled")) s.setMacOSEnabled(parseBooleanSafe(m.get("macOSEnabled"), false));
        if (m.containsKey("emailEnabled")) s.setEmailEnabled(parseBooleanSafe(m.get("emailEnabled"), false));
        if (m.containsKey("emailHost")) s.setEmailHost(String.valueOf(m.get("emailHost")));
        if (m.containsKey("emailPort")) s.setEmailPort(parseIntSafe(m.get("emailPort"), 587));
        if (m.containsKey("emailUsername")) s.setEmailUsername(String.valueOf(m.get("emailUsername")));
        if (m.containsKey("emailFrom")) s.setEmailFrom(String.valueOf(m.get("emailFrom")));
        if (m.containsKey("emailRecipient")) s.setEmailRecipient(String.valueOf(m.get("emailRecipient")));
        if (m.containsKey("emailAuth")) s.setEmailAuth(parseBooleanSafe(m.get("emailAuth"), true));
        if (m.containsKey("emailStarttls")) s.setEmailStarttls(parseBooleanSafe(m.get("emailStarttls"), true));
        if (m.containsKey("quietHoursEnabled")) s.setQuietHoursEnabled(parseBooleanSafe(m.get("quietHoursEnabled"), false));
        if (m.containsKey("quietHoursStart")) s.setQuietHoursStart(parseTimeSafe(m.get("quietHoursStart"), LocalTime.of(22, 0)));
        if (m.containsKey("quietHoursEnd")) s.setQuietHoursEnd(parseTimeSafe(m.get("quietHoursEnd"), LocalTime.of(8, 0)));
        if (m.containsKey("reminderCheckIntervalMinutes")) {
            s.setReminderCheckIntervalMinutes(parseIntSafe(m.get("reminderCheckIntervalMinutes"), 15));
        }
        return s;
    }

    private static int parseIntSafe(Object v, int defaultValue) {
        if (v == null) return defaultValue;
        if (v instanceof Number n) return n.intValue();
        try {
            return (int) Double.parseDouble(String.valueOf(v).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static boolean parseBooleanSafe(Object v, boolean defaultValue) {
        if (v == null) return defaultValue;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(v).trim());
    }

    private static LocalTime parseTimeSafe(Object v, LocalTime defaultValue) {
        if (v == null) return defaultValue;
        try {
            return LocalTime.parse(String.valueOf(v).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static MessageProvider.Tone parseToneSafe(Object v, MessageProvider.Tone defaultValue) {
        if (v == null) return defaultValue;
        try {
            return MessageProvider.Tone.valueOf(String.valueOf(v).trim().toUpperCase());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
