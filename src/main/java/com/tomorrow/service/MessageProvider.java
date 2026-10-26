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

    private volatile Tone tone;
    // last message used per (taskId, type) to avoid immediate repetition
    private final Map<String, String> lastMessage = new ConcurrentHashMap<>();

    public MessageProvider() {
        this(Tone.HUMOROUS);
    }

    public MessageProvider(Tone tone) {
        this.tone = tone != null ? tone : Tone.HUMOROUS;
    }

    public Tone getTone() {
        return tone;
    }

    public void setTone(Tone tone) {
        this.tone = tone != null ? tone : Tone.HUMOROUS;
    }

    private String safeTitle(Task task) {
        if (task == null || task.getTitle() == null || task.getTitle().isBlank()) return "Untitled Task";
        return task.getTitle();
    }

    private String safeId(Task task) {
        if (task == null || task.getId() == null) return "task";
        return task.getId();
    }

    public String created(Task task) {
        String title = safeTitle(task);
        List<String> humorous = List.of(
                "New task added: \"" + title + "\". Future you will deal with it.",
                "Task \"" + title + "\" created. Procrastination officially scheduled.",
                "Added \"" + title + "\". It's not late yet — enjoy it while it lasts.");
        List<String> professional = List.of(
                "Task created: " + title,
                "A new task has been added: " + title,
                "Task \"" + title + "\" is now tracked.");
        return pick(safeId(task) + ":created", humorous, professional);
    }

    public String postponed(Task task) {
        String title = safeTitle(task);
        LocalDate due = task != null && task.getDueDate() != null ? task.getDueDate() : LocalDate.now();
        int postCount = task != null ? task.getPostponementCount() : 1;
        List<String> humorous = List.of(
                "\"" + title + "\" pushed to " + due
                        + ". Tomorrow's problem is now literally tomorrow.",
                "Postponed \"" + title + "\" (attempt #"
                        + postCount + "). The deadline retreats.",
                "\"" + title + "\" is now due " + due
                        + ". Denial is a valid strategy.");
        List<String> professional = List.of(
                "Task postponed: " + title + ", new due date " + due,
                "\"" + title + "\" has been postponed (postponement #"
                        + postCount + ").",
                "Due date updated to " + due + " for task " + title);
        return pick(safeId(task) + ":postponed", humorous, professional);
    }

    public String dueToday(Task task) {
        String title = safeTitle(task);
        List<String> humorous = List.of(
                "\"" + title + "\" is due today. Do it now, or don't — we won't tell.",
                "Heads up: \"" + title + "\" expires today. Tick tock.",
                "\"" + title + "\" is due today. The future has arrived.");
        List<String> professional = List.of(
                "Task due today: " + title,
                "\"" + title + "\" is due today.",
                "Reminder: " + title + " is due today.");
        return pick(safeId(task) + ":dueToday", humorous, professional);
    }

    public String overdue(Task task, LocalDate today, long daysOverdue) {
        String title = safeTitle(task);
        List<String> humorous = List.of(
                "\"" + title + "\" is " + daysOverdue + " day(s) overdue. "
                        + "It misses you.",
                "Overdue: \"" + title + "\". The due date gave up on you.",
                "\"" + title + "\" was due " + daysOverdue + " day(s) ago. "
                        + "It's fine. Probably.");
        List<String> professional = List.of(
                "Task overdue: " + title + " (" + daysOverdue + " day(s) overdue)",
                "\"" + title + "\" is overdue by " + daysOverdue + " day(s).",
                "Overdue task: " + title);
        return pick(safeId(task) + ":overdue", humorous, professional);
    }

    public String completed(Task task) {
        String title = safeTitle(task);
        List<String> humorous = List.of(
                "Completed: \"" + title + "\". A rare and glorious event.",
                "\"" + title + "\" marked complete. Productivity: unlocked.",
                "You finished \"" + title + "\". Take a bow (or a nap).");
        List<String> professional = List.of(
                "Task completed: " + title,
                "\"" + title + "\" has been marked complete.",
                "Task " + title + " is now complete.");
        return pick(safeId(task) + ":completed", humorous, professional);
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
