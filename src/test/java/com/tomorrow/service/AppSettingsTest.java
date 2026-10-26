package com.tomorrow.service;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AppSettingsTest {

    @Test
    void defaultSettings() {
        AppSettings s = new AppSettings();
        assertEquals(MessageProvider.Tone.HUMOROUS, s.getTone());
        assertTrue(s.isInAppEnabled());
        assertFalse(s.isMacOSEnabled());
        assertFalse(s.isEmailEnabled());
        assertEquals(587, s.getEmailPort());
        assertFalse(s.isQuietHoursEnabled());
    }

    @Test
    void quietHoursSameDay() {
        AppSettings s = new AppSettings();
        s.setQuietHoursEnabled(true);
        s.setQuietHoursStart(LocalTime.of(9, 0));
        s.setQuietHoursEnd(LocalTime.of(17, 0));

        assertTrue(s.inQuietHours(LocalTime.of(10, 0)));
        assertTrue(s.inQuietHours(LocalTime.of(9, 0)));
        assertFalse(s.inQuietHours(LocalTime.of(8, 59)));
        assertFalse(s.inQuietHours(LocalTime.of(17, 0)));
        assertFalse(s.inQuietHours(LocalTime.of(20, 0)));
    }

    @Test
    void quietHoursOvernight() {
        AppSettings s = new AppSettings();
        s.setQuietHoursEnabled(true);
        s.setQuietHoursStart(LocalTime.of(22, 0));
        s.setQuietHoursEnd(LocalTime.of(8, 0));

        assertTrue(s.inQuietHours(LocalTime.of(23, 0)));
        assertTrue(s.inQuietHours(LocalTime.of(22, 0)));
        assertTrue(s.inQuietHours(LocalTime.of(3, 0)));
        assertTrue(s.inQuietHours(LocalTime.of(7, 59)));
        assertFalse(s.inQuietHours(LocalTime.of(8, 0)));
        assertFalse(s.inQuietHours(LocalTime.of(14, 0)));
    }

    @Test
    void fromMapHandlesGsonDoubleNumbers() {
        // Gson parses untyped numbers in maps as Double (e.g. 587.0)
        Map<String, Object> map = Map.of(
                "emailPort", 587.0,
                "reminderCheckIntervalMinutes", 30.0,
                "tone", "PROFESSIONAL",
                "inAppEnabled", "true"
        );
        AppSettings s = AppSettings.fromMap(map);
        assertEquals(587, s.getEmailPort());
        assertEquals(30, s.getReminderCheckIntervalMinutes());
        assertEquals(MessageProvider.Tone.PROFESSIONAL, s.getTone());
        assertTrue(s.isInAppEnabled());
    }
}
