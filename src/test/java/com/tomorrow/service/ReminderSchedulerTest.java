package com.tomorrow.service;

import com.tomorrow.model.Task;
import com.tomorrow.storage.JsonStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class ReminderSchedulerTest {

    @TempDir
    Path dir;

    @Test
    void reconcileOnLaunchGeneratesReminders() {
        JsonStorage storage = new JsonStorage(dir);
        TaskManager tm = new TaskManager(storage);
        NotificationManager nm = new NotificationManager(storage, new MessageProvider());
        AppSettings settings = new AppSettings();

        LocalDate fixedToday = LocalDate.of(2026, 3, 15);
        ReminderScheduler.Clock clock = new ReminderScheduler.Clock() {
            @Override public LocalDate today() { return fixedToday; }
            @Override public LocalTime now() { return LocalTime.of(10, 0); }
        };

        // Add an overdue task and a due-today task
        Task overdue = tm.add("Overdue task", "desc", fixedToday.minusDays(2));
        Task dueToday = tm.add("Due today task", "desc", fixedToday);
        Task future = tm.add("Future task", "desc", fixedToday.plusDays(5));

        ReminderScheduler scheduler = new ReminderScheduler(tm, nm, null, settings, clock);
        scheduler.reconcileOnLaunch();

        // Overdue and dueToday must produce reminders; future should not
        assertEquals(2, nm.getNotifications().size());

        // Reconcile again — deduplication prevents duplicate bursts!
        scheduler.reconcileOnLaunch();
        assertEquals(2, nm.getNotifications().size());
    }
}
