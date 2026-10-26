package com.tomorrow.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ponytail: core model validation, postponement, completion, overdue.
 */
class TaskTest {

    @Test
    void defaultsToTodayAndValidTitle() {
        Task t = new Task("Buy milk");
        assertEquals("Buy milk", t.getTitle());
        assertEquals(LocalDate.now(), t.getCreationDate());
        assertEquals(LocalDate.now(), t.getDueDate());
        assertEquals(LocalDate.now(), t.getOriginalDueDate());
        assertEquals(0, t.getPostponementCount());
        assertFalse(t.isCompleted());
        assertNull(t.getCompletionDate());
        assertFalse(t.isOverdue(LocalDate.now()));
    }

    @Test
    void rejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class, () -> new Task(""));
        assertThrows(IllegalArgumentException.class, () -> new Task("   "));
        Task t = new Task("ok");
        assertThrows(IllegalArgumentException.class, () -> t.setTitle(null));
        assertThrows(IllegalArgumentException.class, () -> t.setTitle("  "));
    }

    @Test
    void allowsPastDueDate() {
        Task t = new Task("late");
        t.setDueDate(LocalDate.now().minusDays(5));
        assertTrue(t.isOverdue(LocalDate.now()));
    }

    @Test
    void postponeAddsOneDayAndIncrements() {
        Task t = new Task("task");
        LocalDate before = t.getDueDate();
        t.postpone();
        assertEquals(before.plusDays(1), t.getDueDate());
        assertEquals(before, t.getOriginalDueDate());
        assertEquals(1, t.getPostponementCount());
    }

    @Test
    void completedCannotBePostponed() {
        Task t = new Task("task");
        t.complete();
        assertThrows(IllegalStateException.class, t::postpone);
    }

    @Test
    void completeStampsDate() {
        Task t = new Task("task");
        LocalDate before = LocalDate.now();
        t.complete();
        assertTrue(t.isCompleted());
        assertNotNull(t.getCompletionDate());
        assertTrue(t.getCompletionDate().isBefore(before.plusDays(1)));
        assertFalse(t.isOverdue(LocalDate.now()));
    }

    @Test
    void notOverdueWhenDueTodayOrFuture() {
        Task t = new Task("task");
        t.setDueDate(LocalDate.now());
        assertFalse(t.isOverdue(LocalDate.now()));
        t.setDueDate(LocalDate.now().plusDays(3));
        assertFalse(t.isOverdue(LocalDate.now()));
    }
}
