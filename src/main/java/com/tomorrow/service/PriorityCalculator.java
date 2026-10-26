package com.tomorrow.service;

import com.tomorrow.model.Task;
import com.tomorrow.notification.Notification;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Calculates notification priority from deadline tiers and sorts notifications.
 * <p>
 * Tiers: P1 overdue 3+ days, P2 overdue 1-2 days, P3 due today,
 * P4 due in 1-3 days, P5 due in 4+ days.
 */
public class PriorityCalculator {
    public static final int P1_THRESHOLD = -3;
    public static final int P2_LOW = -2;
    public static final int P2_HIGH = -1;
    public static final int P5_THRESHOLD = 4;

    public Notification.Priority priorityFor(LocalDate dueDate, LocalDate today) {
        if (today == null) today = LocalDate.now();
        if (dueDate == null) return Notification.Priority.P5;
        long days = dueDate.toEpochDay() - today.toEpochDay();
        if (days <= P1_THRESHOLD) return Notification.Priority.P1;
        if (days >= P2_LOW && days <= P2_HIGH) return Notification.Priority.P2;
        if (days == 0) return Notification.Priority.P3;
        if (days >= 1 && days <= 3) return Notification.Priority.P4;
        return Notification.Priority.P5;
    }

    public void sort(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) return;
        Comparator<Notification> byPriority = Comparator.comparingInt(n -> n.getPriority() == null ? 99 : tier(n.getPriority()));
        Comparator<Notification> byDueDate = Comparator
                .comparing(Notification::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<Notification> byPostponement = Comparator
                .comparingInt(Notification::getPostponementCount).reversed();
        notifications.sort(byPriority.thenComparing(byDueDate).thenComparing(byPostponement));
    }

    private int tier(Notification.Priority p) {
        if (p == null) return 99;
        return switch (p) {
            case P1 -> 1;
            case P2 -> 2;
            case P3 -> 3;
            case P4 -> 4;
            case P5 -> 5;
        };
    }
}
