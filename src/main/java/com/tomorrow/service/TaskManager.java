package com.tomorrow.service;

import com.tomorrow.model.Task;
import com.tomorrow.storage.JsonStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Task operations and validation. Persists changes through {@link JsonStorage}.
 */
public class TaskManager {
    private final List<Task> tasks = new ArrayList<>();
    private final JsonStorage storage;

    public TaskManager(JsonStorage storage) {
        this.storage = storage;
    }

    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public void load(List<Task> loaded) {
        tasks.clear();
        tasks.addAll(loaded != null ? loaded : List.of());
        save();
    }

    public Task add(String title, String description, LocalDate dueDate) {
        Task task = new Task(title);
        task.setDescription(description);
        task.setDueDate(dueDate != null ? dueDate : LocalDate.now());
        task.setOriginalDueDate(task.getDueDate());
        tasks.add(task);
        save();
        return task;
    }

    public void edit(String id, String title, String description, LocalDate dueDate) {
        Task task = find(id);
        task.setTitle(title);
        task.setDescription(description);
        task.setDueDate(dueDate != null ? dueDate : LocalDate.now());
        if (!task.isCompleted()) {
            task.setOriginalDueDate(task.getDueDate());
        }
        save();
    }

    public void complete(String id) {
        Task task = find(id);
        task.complete();
        save();
    }

    public void postpone(String id) {
        Task task = find(id);
        task.postpone();
        save();
    }

    public void delete(String id) {
        tasks.removeIf(t -> t.getId().equals(id));
        save();
    }

    public List<Task> filter(Filter filter) {
        LocalDate today = LocalDate.now();
        return tasks.stream()
                .filter(t -> switch (filter) {
                    case ALL -> true;
                    case PENDING -> !t.isCompleted();
                    case COMPLETED -> t.isCompleted();
                    case OVERDUE -> t.isOverdue(today);
                })
                .collect(Collectors.toList());
    }

    public enum Filter { ALL, PENDING, COMPLETED, OVERDUE }

    public Stats stats() {
        LocalDate today = LocalDate.now();
        int total = tasks.size();
        int pending = 0, completed = 0, overdue = 0, postponements = 0;
        for (Task t : tasks) {
            if (t.isCompleted()) completed++;
            else {
                pending++;
                if (t.isOverdue(today)) overdue++;
            }
            postponements += t.getPostponementCount();
        }
        return new Stats(total, pending, completed, overdue, postponements);
    }

    public record Stats(int total, int pending, int completed, int overdue, int postponements) {}

    private Task find(String id) {
        return tasks.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown task id: " + id));
    }

    private void save() {
        storage.saveTasks(tasks);
    }
}
