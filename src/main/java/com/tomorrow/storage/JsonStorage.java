package com.tomorrow.storage;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.tomorrow.model.Task;
import com.tomorrow.notification.Notification;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Gson-based JSON persistence with explicit LocalDate handling and safe file replacement.
 */
public class JsonStorage {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .setPrettyPrinting()
            .create();

    private final Path dataDir;
    private final Path tasksFile;
    private final Path notificationsFile;
    private final Path settingsFile;

    public JsonStorage(Path dataDir) {
        this.dataDir = dataDir;
        this.tasksFile = dataDir.resolve("tasks.json");
        this.notificationsFile = dataDir.resolve("notifications.json");
        this.settingsFile = dataDir.resolve("settings.json");
    }

    public Path getDataDir() {
        return dataDir;
    }

    /* ---------------- tasks ---------------- */

    public List<Task> loadTasks() {
        return loadList(tasksFile, new TypeToken<List<Task>>() {}.getType());
    }

    public void saveTasks(List<Task> tasks) {
        saveList(tasksFile, tasks);
    }

    /* ---------------- notifications ---------------- */

    public List<Notification> loadNotifications() {
        return loadList(notificationsFile, new TypeToken<List<Notification>>() {}.getType());
    }

    public void saveNotifications(List<Notification> notifications) {
        saveList(notificationsFile, notifications);
    }

    /* ---------------- settings ---------------- */

    @SuppressWarnings("unchecked")
    public Map<String, Object> loadSettings() {
        Object obj = loadObject(settingsFile, new TypeToken<Map<String, Object>>() {}.getType());
        return obj instanceof Map ? (Map<String, Object>) obj : Map.of();
    }

    public void saveSettings(Map<String, Object> settings) {
        saveList(settingsFile, List.of(settings));
    }

    /* ---------------- generic ---------------- */

    private <T> List<T> loadList(Path file, Type type) {
        if (!Files.exists(file)) {
            return List.of();
        }
        String json;
        try {
            json = Files.readString(file);
        } catch (IOException e) {
            throw new StorageException("Unable to read " + file.getFileName() + ": " + e.getMessage(), e);
        }
        if (json == null || json.trim().isEmpty()) {
            return List.of();
        }
        try {
            List<T> list = GSON.fromJson(json, type);
            return list == null ? List.of() : list;
        } catch (Exception e) {
            throw new StorageException("Malformed JSON in " + file.getFileName()
                    + ". File was not overwritten. Fix the file or remove it: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T loadObject(Path file, Type type) {
        if (!Files.exists(file)) {
            return null;
        }
        String json;
        try {
            json = Files.readString(file);
        } catch (IOException e) {
            throw new StorageException("Unable to read " + file.getFileName() + ": " + e.getMessage(), e);
        }
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return (T) GSON.fromJson(json, type);
        } catch (Exception e) {
            throw new StorageException("Malformed JSON in " + file.getFileName()
                    + ". File was not overwritten. Fix the file or remove it: " + e.getMessage(), e);
        }
    }

    private <T> void saveList(Path file, List<T> list) {
        try {
            Files.createDirectories(dataDir);
            String json = GSON.toJson(list);
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, json);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new StorageException("Failed to write " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }

    public static class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        @Override
        public JsonElement serialize(LocalDate date, Type type, JsonSerializationContext ctx) {
            return new JsonPrimitive(date.format(DATE_FMT));
        }

        @Override
        public LocalDate deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) {
            return LocalDate.parse(json.getAsString(), DATE_FMT);
        }
    }

    public static class StorageException extends RuntimeException {
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
