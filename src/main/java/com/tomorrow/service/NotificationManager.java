package com.tomorrow.service;

import com.tomorrow.model.Task;
import com.tomorrow.notification.*;
import com.tomorrow.storage.JsonStorage;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Creates, orders, deduplicates, delivers, and persists notifications.
 */
public class NotificationManager {
    private final List<Notification> notifications = new ArrayList<>();
    private final JsonStorage storage;
    private final PriorityCalculator priorityCalc = new PriorityCalculator();
    private final MessageProvider messageProvider;
    private final Map<String, NotificationChannel> channels = new LinkedHashMap<>();

    public NotificationManager(JsonStorage storage, MessageProvider messageProvider) {
        this.storage = storage;
        this.messageProvider = messageProvider;
        register(new InAppChannel());
    }

    public void register(NotificationChannel channel) {
        channels.put(channel.name(), channel);
    }

    public Map<String, NotificationChannel> getChannels() {
        return Collections.unmodifiableMap(channels);
    }

    public void load(List<Notification> loaded) {
        notifications.clear();
        notifications.addAll(loaded != null ? loaded : List.of());
    }

    public List<Notification> getNotifications() {
        List<Notification> copy = new ArrayList<>(notifications);
        priorityCalc.sort(copy);
        return Collections.unmodifiableList(copy);
    }

    public List<Notification> getUnread() {
        return getNotifications().stream().filter(n -> !n.isRead() && !n.isDismissed()).collect(Collectors.toList());
    }

    public int unreadCount() {
        return (int) notifications.stream().filter(n -> !n.isRead() && !n.isDismissed()).count();
    }

    public void markRead(String id) {
        find(id).ifPresent(n -> n.setRead(true));
        persist();
    }

    public void dismiss(String id) {
        find(id).ifPresent(n -> n.setDismissed(true));
        persist();
    }

    /**
     * Create and deliver a notification for a task event, deduplicating by
     * taskId:type:dueDate so restarts don't recreate the same reminder.
     */
    public void notifyTaskEvent(Task task, Notification.Type type) {
        LocalDate today = LocalDate.now();
        String dedupKey = task.getId() + ":" + type + ":" + task.getDueDate();

        if (notifications.stream().anyMatch(n -> n.getDedupKey() != null
                && n.getDedupKey().equals(dedupKey) && !n.isDismissed())) {
            return; // duplicate suppressed
        }

        Notification n = new Notification();
        n.setTaskId(task.getId());
        n.setTaskTitle(task.getTitle());
        n.setType(type);
        n.setPriority(priorityCalc.priorityFor(task.getDueDate(), today));
        n.setDueDate(task.getDueDate());
        n.setPostponementCount(task.getPostponementCount());

        switch (type) {
            case CREATED -> n.setTitle("Task created");
            case POSTPONED -> n.setTitle("Task postponed");
            case DUE_TODAY -> n.setTitle("Due today");
            case OVERDUE -> n.setTitle("Overdue");
            case COMPLETED -> n.setTitle("Task completed");
            case REMINDER -> n.setTitle("Reminder");
        }

        String message = switch (type) {
            case CREATED -> messageProvider.created(task);
            case POSTPONED -> messageProvider.postponed(task);
            case DUE_TODAY -> messageProvider.dueToday(task);
            case OVERDUE -> messageProvider.overdue(task, today,
                    today.toEpochDay() - task.getDueDate().toEpochDay());
            case COMPLETED -> messageProvider.completed(task);
            case REMINDER -> "Reminder for " + task.getTitle();
        };
        n.setMessage(message);
        n.setDedupKey(dedupKey);

        notifications.add(n);
        deliver(n);
        persist();
    }

    public void deliver(Notification n) {
        List<NotificationChannel> failed = new ArrayList<>();
        for (NotificationChannel ch : channels.values()) {
            if (!ch.isEnabled()) continue;
            try {
                ch.send(n);
            } catch (Throwable t) {
                n.setChannelStatus(ch.name(), false);
                failed.add(ch);
            }
        }
        if (!failed.isEmpty()) {
            System.err.println("[Tomorrow] Notification delivery failed on channels: "
                    + failed.stream().map(NotificationChannel::name).collect(Collectors.joining(", ")));
        }
    }

    public void persist() {
        storage.saveNotifications(notifications);
    }

    private Optional<Notification> find(String id) {
        return notifications.stream().filter(n -> n.getId().equals(id)).findFirst();
    }
}
