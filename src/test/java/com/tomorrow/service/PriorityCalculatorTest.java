package com.tomorrow.service;

import com.tomorrow.model.Task;
import com.tomorrow.notification.Notification;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ponytail: priority boundary dates and sorting.
 */
class PriorityCalculatorTest {

    private final PriorityCalculator calc = new PriorityCalculator();
    private final LocalDate today = LocalDate.of(2026, 6, 15);

    @Test
    void boundaries() {
        assertEquals(Notification.Priority.P1, calc.priorityFor(today.minusDays(3), today));
        assertEquals(Notification.Priority.P1, calc.priorityFor(today.minusDays(10), today));
        assertEquals(Notification.Priority.P2, calc.priorityFor(today.minusDays(2), today));
        assertEquals(Notification.Priority.P2, calc.priorityFor(today.minusDays(1), today));
        assertEquals(Notification.Priority.P3, calc.priorityFor(today, today));
        assertEquals(Notification.Priority.P4, calc.priorityFor(today.plusDays(1), today));
        assertEquals(Notification.Priority.P4, calc.priorityFor(today.plusDays(3), today));
        assertEquals(Notification.Priority.P5, calc.priorityFor(today.plusDays(4), today));
        assertEquals(Notification.Priority.P5, calc.priorityFor(today.plusDays(100), today));
    }

    @Test
    void sortsP1FirstThenDueDateThenPostponement() {
        Task overdue3 = new Task("p1"); overdue3.setDueDate(today.minusDays(3));
        Task overdue1 = new Task("p2"); overdue1.setDueDate(today.minusDays(1));
        Task dueToday = new Task("p3"); dueToday.setDueDate(today);
        Task dueSoon = new Task("p4"); dueSoon.setDueDate(today.plusDays(2));

        Notification n1 = notif(overdue3, Notification.Priority.P1, 5);
        Notification n2 = notif(overdue1, Notification.Priority.P2, 0);
        Notification n3 = notif(dueToday, Notification.Priority.P3, 0);
        Notification n4 = notif(dueSoon, Notification.Priority.P4, 0);
        Notification n1b = notif(overdue3, Notification.Priority.P1, 0); // same tier, fewer postponements

        List<Notification> list = new ArrayList<>(List.of(n3, n1, n2, n4, n1b));
        calc.sort(list);

        assertEquals(Notification.Priority.P1, list.get(0).getPriority());
        assertEquals(Notification.Priority.P1, list.get(1).getPriority());
        // within P1, more postponements first
        assertEquals(5, list.get(0).getPostponementCount());
        assertEquals(0, list.get(1).getPostponementCount());
        assertEquals(Notification.Priority.P2, list.get(2).getPriority());
        assertEquals(Notification.Priority.P3, list.get(3).getPriority());
        assertEquals(Notification.Priority.P4, list.get(4).getPriority());
    }

    private Notification notif(Task task, Notification.Priority p, int postponements) {
        Notification n = new Notification();
        n.setTaskId(task.getId());
        n.setDueDate(task.getDueDate());
        n.setPostponementCount(postponements);
        n.setPriority(p);
        return n;
    }
}
