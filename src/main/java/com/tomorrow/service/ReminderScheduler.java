package com.tomorrow.service;

import com.tomorrow.model.Task;
import com.tomorrow.notification.EmailChannel;
import com.tomorrow.notification.Notification;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Checks tasks periodically while the app is running and delivers reminders.
 * <p>
 * Reminders only run while the application is open. On launch, missed reminders are
 * reconciled without a burst of duplicates (see {@link #reconcileOnLaunch}).
 */
public class ReminderScheduler {
    public interface Clock {
        LocalDate today();
        LocalTime now();
    }

    public static class SystemClock implements Clock {
        @Override
        public LocalDate today() {
            return LocalDate.now();
        }

        @Override
        public LocalTime now() {
            return LocalTime.now();
        }
    }

    private final TaskManager taskManager;
    private final NotificationManager notificationManager;
    private final EmailChannel emailChannel;
    private final AppSettings settings;
    private final Clock clock;
    private final PriorityCalculator priorityCalc = new PriorityCalculator();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "tomorrow-reminder-scheduler");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> handle;
    private LocalDate lastDigestDate;

    public ReminderScheduler(TaskManager taskManager, NotificationManager notificationManager,
                             EmailChannel emailChannel, AppSettings settings) {
        this(taskManager, notificationManager, emailChannel, settings, new SystemClock());
    }

    public ReminderScheduler(TaskManager taskManager, NotificationManager notificationManager,
                             EmailChannel emailChannel, AppSettings settings, Clock clock) {
        this.taskManager = taskManager;
        this.notificationManager = notificationManager;
        this.emailChannel = emailChannel;
        this.settings = settings;
        this.clock = clock;
    }

    public void start() {
        stop();
        long interval = settings.getReminderCheckIntervalMinutes();
        if (interval <= 0) interval = 15;
        handle = scheduler.scheduleAtFixedRate(this::check, 0, interval, TimeUnit.MINUTES);
    }

    public void stop() {
        if (handle != null) handle.cancel(false);
    }

    public void shutdown() {
        stop();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Run one reminder check. Intended to be invoked periodically and once at startup
     * (reconcileOnLaunch). Safe to call repeatedly.
     */
    public void check() {
        LocalDate today = clock.today();
        LocalTime now = clock.now();

        for (Task task : taskManager.getTasks()) {
            if (task.isCompleted()) continue;
            Notification.Priority p = priorityCalc.priorityFor(task.getDueDate(), today);
            boolean overdueOrDue = p == Notification.Priority.P1 || p == Notification.Priority.P2
                    || p == Notification.Priority.P3;
            if (!overdueOrDue) continue;
            if (settings.isQuietHoursEnabled() && settings.inQuietHours(now)) continue;
            notificationManager.notifyTaskEvent(task,
                    p == Notification.Priority.P3 ? Notification.Type.DUE_TODAY : Notification.Type.OVERDUE);
        }
    }

    /**
     * Reconcile missed reminders at launch without a duplicate burst.
     */
    public void reconcileOnLaunch() {
        check();
    }

    /**
     * Send a daily digest email of pending tasks sorted by urgency. At most once per day.
     */
    public void sendDailyDigest() {
        if (!emailChannel.isEnabled() || !emailChannel.isConfigured()) return;
        LocalDate today = clock.today();
        if (lastDigestDate != null && lastDigestDate.equals(today)) return;
        LocalTime now = clock.now();
        if (settings.isQuietHoursEnabled() && settings.inQuietHours(now)) return;

        List<Task> pending = new ArrayList<>(taskManager.filter(TaskManager.Filter.PENDING));
        if (pending.isEmpty()) {
            lastDigestDate = today;
            return;
        }
        pending.sort(Comparator.<Task>comparingInt(t -> tier(priorityCalc.priorityFor(t.getDueDate(), today)))
                .thenComparing(Task::getDueDate));

        StringBuilder sb = new StringBuilder();
        sb.append("Tomorrow daily digest (").append(today).append(")\n\n");
        sb.append("Pending tasks sorted by urgency:\n\n");
        int i = 1;
        for (Task t : pending) {
            Notification.Priority p = priorityCalc.priorityFor(t.getDueDate(), today);
            sb.append(i++).append(". [").append(p).append("] ").append(t.getTitle());
            if (t.getDescription() != null && !t.getDescription().isEmpty()) {
                sb.append(" - ").append(t.getDescription());
            }
            sb.append(" | due ").append(t.getDueDate())
                    .append(" | postponements ").append(t.getPostponementCount()).append("\n");
        }
        sb.append("\nTomorrow — a task manager for procrastinators.");

        Notification digest = new Notification();
        digest.setType(Notification.Type.REMINDER);
        digest.setPriority(Notification.Priority.P3);
        digest.setTitle("Daily digest");
        digest.setMessage(sb.toString());
        digest.setDedupKey("digest:" + today);
        emailChannel.send(digest);
        lastDigestDate = today;
    }

    private int tier(Notification.Priority p) {
        return switch (p) {
            case P1 -> 1;
            case P2 -> 2;
            case P3 -> 3;
            case P4 -> 4;
            case P5 -> 5;
        };
    }
}
