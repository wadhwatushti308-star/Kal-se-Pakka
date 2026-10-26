package com.tomorrow.service;

import com.tomorrow.model.Task;
import com.tomorrow.notification.*;
import com.tomorrow.storage.JsonStorage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private volatile AppSettings settings;
    private final ExecutorService asyncDeliveryExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "tomorrow-notification-delivery");
        t.setDaemon(true);
        return t;
    });

    public NotificationManager(JsonStorage storage, MessageProvider messageProvider) {
        this.storage = storage;
        this.messageProvider = messageProvider;
        register(new InAppChannel());
    }

    public void setSettings(AppSettings settings) {
        this.settings = settings;
    }

    public MessageProvider getMessageProvider() {
        return messageProvider;
    }

    public void register(NotificationChannel channel) {
        if (channel != null) {
            channels.put(channel.name(), channel);
        }
    }

    public void unregister(String name) {
        if (name != null) {
            channels.remove(name);
        }
    }

    public void clearChannels() {
        channels.clear();
    }

    public Map<String, NotificationChannel> getChannels() {
        return Collections.unmodifiableMap(channels);
    }

    public void load(List<Notification> loaded) {
        notifications.clear();
        if (loaded != null) {
            notifications.addAll(loaded);
        }
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
        if (task == null) return;
        LocalDate today = LocalDate.now();
        LocalDate due = task.getDueDate() != null ? task.getDueDate() : today;
        String dedupKey = task.getId() + ":" + type + ":" + due;

        if (notifications.stream().anyMatch(n -> n.getDedupKey() != null
                && n.getDedupKey().equals(dedupKey) && !n.isDismissed())) {
            return; // duplicate suppressed
        }

        Notification n = new Notification();
        n.setTaskId(task.getId());
        n.setTaskTitle(task.getTitle());
        n.setType(type);
        n.setPriority(priorityCalc.priorityFor(due, today));
        n.setDueDate(due);
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
                    today.toEpochDay() - due.toEpochDay());
            case COMPLETED -> messageProvider.completed(task);
            case REMINDER -> "Reminder for " + (task.getTitle() != null ? task.getTitle() : "Task");
        };
        n.setMessage(message);
        n.setDedupKey(dedupKey);

        notifications.add(n);
        deliver(n);
        persist();
    }

    public void deliver(Notification n) {
        boolean quiet = settings != null && settings.isQuietHoursEnabled() && settings.inQuietHours(LocalTime.now());

        for (NotificationChannel ch : channels.values()) {
            if (!ch.isEnabled()) continue;
            String chName = ch.name();

            // In quiet hours, suppress desktop and email notifications, but never in-app
            if (quiet && ("macOS".equalsIgnoreCase(chName) || "email".equalsIgnoreCase(chName))) {
                n.setChannelStatus(chName, false);
                continue;
            }

            // By default, task creation, postponement, and completion do not send separate emails
            if ("email".equalsIgnoreCase(chName) && n.getType() != Notification.Type.REMINDER) {
                continue;
            }

            if ("email".equalsIgnoreCase(chName)) {
                // Keep SMTP work strictly off the Event Dispatch Thread
                asyncDeliveryExecutor.submit(() -> {
                    try {
                        ch.send(n);
                    } catch (Throwable t) {
                        n.setChannelStatus(chName, false);
                    }
                });
            } else {
                try {
                    ch.send(n);
                } catch (Throwable t) {
                    n.setChannelStatus(chName, false);
                }
            }
        }
    }

    public void persist() {
        storage.saveNotifications(notifications);
    }

    private Optional<Notification> find(String id) {
        return notifications.stream().filter(n -> n.getId().equals(id)).findFirst();
    }
}
