package com.tomorrow.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A single task. Dates are stored as {@link LocalDate} and serialized as ISO-8601 strings.
 */
public class Task {
    private String id;
    private String title;
    private String description;
    private LocalDate creationDate;
    private LocalDate originalDueDate;
    private LocalDate dueDate;
    private int postponementCount;
    private boolean completed;
    private LocalDate completionDate;

    public Task() {
        this.id = UUID.randomUUID().toString();
        this.creationDate = LocalDate.now();
        this.dueDate = LocalDate.now();
        this.originalDueDate = LocalDate.now();
        this.postponementCount = 0;
        this.completed = false;
    }

    public Task(String title) {
        this();
        setTitle(title);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be blank.");
        }
        this.title = title.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDate getOriginalDueDate() {
        return originalDueDate;
    }

    public void setOriginalDueDate(LocalDate originalDueDate) {
        this.originalDueDate = originalDueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public int getPostponementCount() {
        return postponementCount;
    }

    public void setPostponementCount(int postponementCount) {
        this.postponementCount = postponementCount;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public LocalDate getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(LocalDate completionDate) {
        this.completionDate = completionDate;
    }

    /**
     * Postpone this pending task by exactly one calendar day.
     */
    public void postpone() {
        if (completed) {
            throw new IllegalStateException("Completed tasks cannot be postponed.");
        }
        if (this.dueDate == null) {
            this.dueDate = LocalDate.now();
        }
        this.dueDate = this.dueDate.plusDays(1);
        this.postponementCount++;
    }

    /**
     * Mark this task complete, stamping the completion date.
     */
    public void complete() {
        this.completed = true;
        this.completionDate = LocalDate.now();
    }

    /**
     * A task is overdue when incomplete and its current due date is before today.
     */
    public boolean isOverdue(LocalDate today) {
        if (today == null) {
            today = LocalDate.now();
        }
        return !completed && dueDate != null && dueDate.isBefore(today);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return title + " (due " + dueDate + ")";
    }
}
