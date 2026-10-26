package com.tomorrow.service;

import com.tomorrow.model.Task;
import com.tomorrow.storage.JsonStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ponytail: TaskManager operations incl. postponement, completion, deletion, filtering.
 */
class TaskManagerTest {

    @TempDir
    Path dir;
    private final AtomicInteger counter = new AtomicInteger();

    private TaskManager newTm() {
        return new TaskManager(new JsonStorage(dir.resolve("run-" + counter.getAndIncrement())));
    }

    @Test
    void addAndList() {
        TaskManager tm = newTm();
        Task t = tm.add("Buy milk", null, LocalDate.now().plusDays(2));
        assertNotNull(t.getId());
        assertEquals(1, tm.getTasks().size());
        assertEquals("Buy milk", tm.getTasks().get(0).getTitle());
        assertEquals(1, tm.stats().total());
    }

    @Test
    void rejectsBlankTitle() {
        TaskManager tm = newTm();
        assertThrows(IllegalArgumentException.class, () -> tm.add("", null, LocalDate.now()));
    }

    @Test
    void postpone() {
        TaskManager tm = newTm();
        Task t = tm.add("task", null, LocalDate.now());
        LocalDate before = t.getDueDate();
        tm.postpone(t.getId());
        Task got = tm.getTasks().get(0);
        assertEquals(before.plusDays(1), got.getDueDate());
        assertEquals(before, got.getOriginalDueDate());
        assertEquals(1, got.getPostponementCount());
    }

    @Test
    void postponeRejectsCompleted() {
        TaskManager tm = newTm();
        Task t = tm.add("task", null, LocalDate.now());
        tm.complete(t.getId());
        assertThrows(IllegalStateException.class, () -> tm.postpone(t.getId()));
    }

    @Test
    void complete() {
        TaskManager tm = newTm();
        Task t = tm.add("task", null, LocalDate.now());
        tm.complete(t.getId());
        assertTrue(tm.getTasks().get(0).isCompleted());
        assertNotNull(tm.getTasks().get(0).getCompletionDate());
    }

    @Test
    void delete() {
        TaskManager tm = newTm();
        Task t = tm.add("task", null, LocalDate.now());
        tm.delete(t.getId());
        assertTrue(tm.getTasks().isEmpty());
    }

    @Test
    void filters() {
        TaskManager tm = newTm();
        Task overdue = tm.add("overdue", null, LocalDate.now().minusDays(2));
        Task dueToday = tm.add("today", null, LocalDate.now());
        Task future = tm.add("future", null, LocalDate.now().plusDays(5));
        tm.complete(future.getId());

        assertEquals(3, tm.filter(TaskManager.Filter.ALL).size());
        assertEquals(2, tm.filter(TaskManager.Filter.PENDING).size());
        assertEquals(1, tm.filter(TaskManager.Filter.COMPLETED).size());
        assertEquals(1, tm.filter(TaskManager.Filter.OVERDUE).size());
        assertEquals(overdue.getId(), tm.filter(TaskManager.Filter.OVERDUE).get(0).getId());
    }

    @Test
    void statsDerived() {
        TaskManager t = newTm();
        t.add("a", null, LocalDate.now().minusDays(2)); // overdue
        t.add("b", null, LocalDate.now());              // due today
        String id3 = t.add("c", null, LocalDate.now().plusDays(5)).getId();
        t.complete(id3);
        TaskManager.Stats s = t.stats();
        assertEquals(3, s.total());
        assertEquals(2, s.pending());
        assertEquals(1, s.completed());
        assertEquals(1, s.overdue());
        assertEquals(0, s.postponements());
    }

    @Test
    void unknownIdThrows() {
        TaskManager tm = newTm();
        assertThrows(IllegalArgumentException.class, () -> tm.complete("nope"));
    }
}
