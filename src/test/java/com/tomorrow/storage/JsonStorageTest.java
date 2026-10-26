package com.tomorrow.storage;

import com.tomorrow.model.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ponytail: round-trip persistence, missing file, invalid JSON handling.
 */
class JsonStorageTest {

    @TempDir
    Path dir;

    @Test
    void roundTripTasks() {
        JsonStorage s = new JsonStorage(dir);
        Task t = new Task("Buy milk");
        t.setDueDate(LocalDate.of(2026, 1, 15));
        t.setOriginalDueDate(LocalDate.of(2026, 1, 15));
        t.setDescription("get it");
        t.postpone();
        t.complete();

        s.saveTasks(List.of(t));
        List<Task> loaded = s.loadTasks();
        assertEquals(1, loaded.size());
        Task back = loaded.get(0);
        assertEquals(t.getId(), back.getId());
        assertEquals("Buy milk", back.getTitle());
        assertEquals("get it", back.getDescription());
        assertEquals(LocalDate.of(2026, 1, 16), back.getDueDate());
        assertEquals(LocalDate.of(2026, 1, 15), back.getOriginalDueDate());
        assertEquals(1, back.getPostponementCount());
        assertTrue(back.isCompleted());
        assertNotNull(back.getCompletionDate());
    }

    @Test
    void missingFileReturnsEmpty() throws Exception {
        JsonStorage s = new JsonStorage(dir);
        assertTrue(s.loadTasks().isEmpty());
    }

    @Test
    void invalidJsonNotOverwritten() throws Exception {
        JsonStorage s = new JsonStorage(dir);
        Path file = dir.resolve("tasks.json");
        Files.writeString(file, "{ not valid json");
        JsonStorage.StorageException ex = assertThrows(JsonStorage.StorageException.class, s::loadTasks);
        assertTrue(ex.getMessage().contains("Malformed JSON"));
        // file must remain untouched
        assertEquals("{ not valid json", Files.readString(file));
    }

    @Test
    void emptyFileReturnsEmpty() throws Exception {
        JsonStorage s = new JsonStorage(dir);
        Files.writeString(dir.resolve("tasks.json"), "");
        assertTrue(s.loadTasks().isEmpty());
    }

    @Test
    void roundTripSettings() {
        JsonStorage s = new JsonStorage(dir);
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("tone", "PROFESSIONAL");
        map.put("emailPort", 587);
        map.put("inAppEnabled", true);
        map.put("reminderCheckIntervalMinutes", 30);

        s.saveSettings(map);
        java.util.Map<String, Object> loaded = s.loadSettings();
        assertNotNull(loaded);
        assertEquals("PROFESSIONAL", loaded.get("tone"));
        assertEquals(Boolean.TRUE, loaded.get("inAppEnabled"));
    }

    @Test
    void roundTripNotifications() {
        JsonStorage s = new JsonStorage(dir);
        com.tomorrow.notification.Notification n = new com.tomorrow.notification.Notification();
        n.setTitle("Important");
        n.setMessage("Do not procrastinate too much");
        n.setType(com.tomorrow.notification.Notification.Type.OVERDUE);
        n.setPriority(com.tomorrow.notification.Notification.Priority.P1);
        n.setDueDate(LocalDate.of(2026, 2, 20));

        s.saveNotifications(List.of(n));
        List<com.tomorrow.notification.Notification> loaded = s.loadNotifications();
        assertEquals(1, loaded.size());
        assertEquals("Important", loaded.get(0).getTitle());
        assertEquals(com.tomorrow.notification.Notification.Priority.P1, loaded.get(0).getPriority());
        assertNotNull(loaded.get(0).getCreatedAt());
    }
}
