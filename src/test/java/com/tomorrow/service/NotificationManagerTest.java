package com.tomorrow.service;

import com.tomorrow.model.Task;
import com.tomorrow.notification.InAppChannel;
import com.tomorrow.notification.Notification;
import com.tomorrow.notification.NotificationChannel;
import com.tomorrow.storage.JsonStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class NotificationManagerTest {

    @TempDir
    Path dir;

    @Test
    void notifyTaskEventAndDeduplicate() {
        JsonStorage storage = new JsonStorage(dir);
        NotificationManager nm = new NotificationManager(storage, new MessageProvider());

        Task task = new Task("Finish BCA project");
        task.setDueDate(LocalDate.now());

        // First notification
        nm.notifyTaskEvent(task, Notification.Type.DUE_TODAY);
        assertEquals(1, nm.getNotifications().size());
        assertEquals(1, nm.unreadCount());

        // Repeated notification for same task, type, and date should be suppressed
        nm.notifyTaskEvent(task, Notification.Type.DUE_TODAY);
        assertEquals(1, nm.getNotifications().size());

        // Postponing task creates a new notification for new due date
        task.postpone();
        nm.notifyTaskEvent(task, Notification.Type.POSTPONED);
        assertEquals(2, nm.getNotifications().size());
    }

    @Test
    void markReadAndDismiss() {
        JsonStorage storage = new JsonStorage(dir);
        NotificationManager nm = new NotificationManager(storage, new MessageProvider());

        Task task = new Task("Submit assignment");
        nm.notifyTaskEvent(task, Notification.Type.CREATED);

        Notification n = nm.getNotifications().get(0);
        assertFalse(n.isRead());
        assertFalse(n.isDismissed());

        nm.markRead(n.getId());
        assertTrue(nm.getNotifications().get(0).isRead());
        assertEquals(0, nm.unreadCount());

        nm.dismiss(n.getId());
        assertTrue(nm.getNotifications().get(0).isDismissed());
    }

    @Test
    void quietHoursSuppressesDesktopAndEmailButKeepsInApp() {
        JsonStorage storage = new JsonStorage(dir);
        NotificationManager nm = new NotificationManager(storage, new MessageProvider());

        AppSettings settings = new AppSettings();
        settings.setQuietHoursEnabled(true);
        // Force quiet hours active by setting 00:00 to 23:59:59
        settings.setQuietHoursStart(LocalTime.MIN);
        settings.setQuietHoursEnd(LocalTime.MAX);
        nm.setSettings(settings);

        AtomicBoolean desktopSent = new AtomicBoolean(false);
        NotificationChannel mockDesktop = new NotificationChannel() {
            @Override public String name() { return "macOS"; }
            @Override public boolean isEnabled() { return true; }
            @Override public void setEnabled(boolean enabled) {}
            @Override public boolean send(Notification notification) {
                desktopSent.set(true);
                return true;
            }
        };

        nm.register(mockDesktop);

        Task task = new Task("Overdue homework");
        task.setDueDate(LocalDate.now().minusDays(3));
        nm.notifyTaskEvent(task, Notification.Type.OVERDUE);

        // Desktop delivery was suppressed during quiet hours
        assertFalse(desktopSent.get());

        // But in-app notification exists in history!
        assertEquals(1, nm.getNotifications().size());
        assertEquals("Overdue", nm.getNotifications().get(0).getTitle());
    }
}
