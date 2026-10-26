package com.tomorrow.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A notification generated for a task event, with delivery status per channel.
 */
public class Notification {
    public enum Type { CREATED, POSTPONED, DUE_TODAY, OVERDUE, COMPLETED, REMINDER }
    public enum Priority { P1, P2, P3, P4, P5 }

    private String id;
    private String taskId;
    private String taskTitle;
    private Type type;
    private Priority priority;
    private String title;
    private String message;
    private Instant createdAt;
    private LocalDate dueDate;
    private int postponementCount;
    private boolean read;
    private boolean dismissed;
    private String dedupKey;
    private final Map<String, Boolean> channelStatus = new LinkedHashMap<>();

    public Notification() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public int getPostponementCount() { return postponementCount; }
    public void setPostponementCount(int postponementCount) { this.postponementCount = postponementCount; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public boolean isDismissed() { return dismissed; }
    public void setDismissed(boolean dismissed) { this.dismissed = dismissed; }

    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String dedupKey) { this.dedupKey = dedupKey; }

    public Map<String, Boolean> getChannelStatus() { return channelStatus; }
    public void setChannelStatus(String channel, boolean delivered) {
        channelStatus.put(channel, delivered);
    }

    public boolean isDelivered(String channel) {
        return Boolean.TRUE.equals(channelStatus.get(channel));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification)) return false;
        Notification n = (Notification) o;
        return Objects.equals(id, n.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
