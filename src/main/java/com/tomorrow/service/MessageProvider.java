package com.tomorrow.service;

import com.tomorrow.model.Task;
import com.tomorrow.notification.Notification;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Selects friendly notification text. Supports humorous (default) and professional tones.
 */
public class MessageProvider {
    public enum Tone { HUMOROUS, PROFESSIONAL }

    private final Tone tone;
    // last message used per (taskId, type) to avoid immediate repetition
    private final Map<String, String> lastMessage = new ConcurrentHashMap<>();

    public MessageProvider() {
        this(Tone.HUMOROUS);
    }

    public MessageProvider(Tone tone) {
        this.tone = tone;
    }

    public Tone getTone() {
        return tone;
    }

    public String created(Task task) {
        List<String> humorous = List.of(
                "New task added: \"" + task.getTitle() + "\". Future you will deal with it.",
                "Task \"" + task.getTitle() + "\" created. Procrastination officially scheduled.",
                "Added \"" + task.getTitle() + "\". It's not late yet — enjoy it while it lasts.");
        List<String> professional = List.of(
                "Task created: " + task.getTitle(),
                "A new task has been added: " + task.getTitle(),
                "Task \"" + task.getTitle() + "\" is now tracked.");
        return pick(task.getId() + ":created", humorous, professional);
    }

    public String postponed(Task task) {
        List<String> humorous = List.of(
                "\"" + task.getTitle() + "\" pushed to " + task.getDueDate()
                        + ". Tomorrow's problem is now literally tomorrow.",
                "Postponed \"" + task.getTitle() + "\" (attempt #"
                        + task.getPostponementCount() + "). The deadline retreats.",
                "\"" + task.getTitle() + "\" is now due " + task.getDueDate()
                        + ". Denial is a valid strategy.");
        List<String> professional = List.of(
                "Task postponed: " + task.getTitle() + ", new due date " + task.getDueDate(),
                "\"" + task.getTitle() + "\" has been postponed (postponement #"
                        + task.getPostponementCount() + ").",
                "Due date updated to " + task.getDueDate() + " for task " + task.getTitle());
        return pick(task.getId() + ":postponed", humorous, professional);
    }

    public String dueToday(Task task) {
        List<String> humorous = List.of(
                "\"" + task.getTitle() + "\" is due today. Do it now, or don't — we won't tell.",
                "Heads up: \"" + task.getTitle() + "\" expires today. Tick tock.",
                "\"" + task.getTitle() + "\" is due today. The future has arrived.");
        List<String> professional = List.of(
                "Task due today: " + task.getTitle(),
                "\"" + task.getTitle() + "\" is due today.",
                "Reminder: " + task.getTitle() + " is due today.");
        return pick(task.getId() + ":dueToday", humorous, professional);
    }

    public String overdue(Task task, LocalDate today, long daysOverdue) {
        List<String> humorous = List.of(
                "\"" + task.getTitle() + "\" is " + daysOverdue + " day(s) overdue. "
                        + "It misses you.",
                "Overdue: \"" + task.getTitle() + "\". The due date gave up on you.",
                "\"" + task.getTitle() + "\" was due " + daysOverdue + " day(s) ago. "
                        + "It's fine. Probably.");
        List<String> professional = List.of(
                "Task overdue: " + task.getTitle() + " (" + daysOverdue + " day(s) overdue)",
                "\"" + task.getTitle() + "\" is overdue by " + daysOverdue + " day(s).",
                "Overdue task: " + task.getTitle());
        return pick(task.getId() + ":overdue", humorous, professional);
    }

    public String completed(Task task) {
        List<String> humorous = List.of(
                "Completed: \"" + task.getTitle() + "\". A rare and glorious event.",
                "\"" + task.getTitle() + "\" marked complete. Productivity: unlocked.",
                "You finished \"" + task.getTitle() + "\". Take a bow (or a nap).");
        List<String> professional = List.of(
                "Task completed: " + task.getTitle(),
                "\"" + task.getTitle() + "\" has been marked complete.",
                "Task " + task.getTitle() + " is now complete.");
        return pick(task.getId() + ":completed", humorous, professional);
    }

    private String pick(String key, List<String> humorous, List<String> professional) {
        List<String> options = tone == Tone.PROFESSIONAL ? professional : humorous;
        String last = lastMessage.get(key);
        String chosen;
        if (options.size() > 1 && last != null) {
            List<String> alternates = new ArrayList<>();
            for (String o : options) {
                if (!o.equals(last)) alternates.add(o);
            }
            if (!alternates.isEmpty()) {
                chosen = alternates.get(new Random().nextInt(alternates.size()));
            } else {
                chosen = options.get(new Random().nextInt(options.size()));
            }
        } else {
            chosen = options.get(new Random().nextInt(options.size()));
        }
        lastMessage.put(key, chosen);
        return chosen;
    }
}
