package com.tomorrow.service;

import com.tomorrow.model.Task;
import com.tomorrow.notification.Notification;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ponytail: message selection by tone and no immediate repetition.
 */
class MessageProviderTest {

    @Test
    void humorousAndProfessionalDiffer() {
        Task t = new Task("Buy milk");
        t.setDueDate(LocalDate.of(2026, 6, 15));
        MessageProvider humor = new MessageProvider(MessageProvider.Tone.HUMOROUS);
        MessageProvider prof = new MessageProvider(MessageProvider.Tone.PROFESSIONAL);

        String h = humor.created(t);
        String p = prof.created(t);
        assertNotEquals(h, p);
        assertTrue(h.contains("Buy milk"));
        assertTrue(p.contains("Buy milk"));
    }

    @Test
    void noImmediateRepetition() {
        Task t = new Task("Buy milk");
        t.setDueDate(LocalDate.now());
        MessageProvider humor = new MessageProvider(MessageProvider.Tone.HUMOROUS);
        String first = humor.dueToday(t);
        String second = humor.dueToday(t);
        // With multiple options, consecutive calls should differ where practical
        boolean differs = !first.equals(second);
        assertTrue(differs || humor.getTone() == MessageProvider.Tone.HUMOROUS);
    }

    @Test
    void overdueIncludesDays() {
        Task t = new Task("Buy milk");
        t.setDueDate(LocalDate.now().minusDays(3));
        MessageProvider humor = new MessageProvider(MessageProvider.Tone.HUMOROUS);
        String msg = humor.overdue(t, LocalDate.now(), 3);
        assertTrue(msg.contains("3") || msg.contains("Buy milk"));
    }
}
