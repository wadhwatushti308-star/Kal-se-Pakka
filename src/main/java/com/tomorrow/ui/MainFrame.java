package com.tomorrow.ui;

import com.tomorrow.model.Task;
import com.tomorrow.service.TaskManager;
import com.tomorrow.service.NotificationManager;
import com.tomorrow.service.AppSettings;
import com.tomorrow.service.ReminderScheduler;
import com.tomorrow.notification.Notification;
import com.tomorrow.notification.EmailChannel;
import com.tomorrow.notification.InAppChannel;
import com.tomorrow.notification.MacOSChannel;
import com.tomorrow.storage.JsonStorage;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.file.Path;
import javax.swing.border.EmptyBorder;
import javax.swing.*;

/**
 * Main application window with dashboard, task list, and notification panel.
 * Task cards display information; all actions are via the bottom action bar.
 */
public class MainFrame extends JFrame {
    private final TaskManager taskManager;
    private final NotificationManager notifMgr;
    private final AppSettings settings;
    private final JsonStorage storage;
    private JPanel taskPanel;
    private JLabel lblStats;

    public MainFrame(TaskManager taskManager, NotificationManager notifMgr, AppSettings settings) {
        this.taskManager = taskManager;
        this.notifMgr = notifMgr;
        this.settings = settings;
        Path dataDir = Path.of(System.getProperty("user.home"), ".local", "share", "tomorrow");
        this.storage = new JsonStorage(dataDir);

        setTitle("Tomorrow - Task Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);

        initializeUI();

        // load tasks and refresh UI
        taskManager.load(List.of());
        refreshTaskList();
        scheduleReminders();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(0, 0));

        // Dashboard panel (north)
        add(createDashboardPanel(), BorderLayout.NORTH);

        // Task panel (center)
        taskPanel = new JPanel();
        taskPanel.setLayout(new GridLayout(0, 1, 8, 8));
        taskPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane taskScroll = new JScrollPane(taskPanel);
        taskScroll.setBorder(BorderFactory.createTitledBorder("Tasks"));
        add(taskScroll, BorderLayout.CENTER);

        // Bottom panel: actions + notification panel
        JPanel bottom = new JPanel(new BorderLayout(0, 0));
        bottom.add(createActionBar(), BorderLayout.WEST);
        bottom.add(createNotificationPanel(), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        pack();
    }

    private JPanel createDashboardPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBorder(BorderFactory.createTitledBorder("Dashboard"));
        lblStats = new JLabel("Loading...");
        p.add(lblStats);
        refreshStats();
        return p;
    }

    private void refreshStats() {
        TaskManager.Stats s = taskManager.stats();
        lblStats.setText(
                "Total: " + s.total() + " | "
                        + "Pending: " + s.pending() + " | "
                        + "Completed: " + s.completed() + " | "
                        + "Overdue: " + s.overdue() + " | "
                        + "Postponements: " + s.postponements()
        );
    }

    private JPanel createActionBar() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder("Actions"));
        JButton btnAdd = new JButton("Add Task");
        btnAdd.addActionListener(e -> showAddEditDialog(null));
        p.add(btnAdd);

        JButton btnPostpone = new JButton("Postpone Selected");
        btnPostpone.addActionListener(e -> {
            Task first = taskManager.getTasks().stream()
                    .filter(t -> !t.isCompleted())
                    .findFirst().orElse(null);
            if (first != null) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Postpone \"" + first.getTitle() + "\" by one day?",
                        "Confirm Postpone", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    taskManager.postpone(first.getId());
                    refreshTaskList();
                    notifMgr.notifyTaskEvent(first, Notification.Type.POSTPONED);
                }
            }
        });
        p.add(btnPostpone);

        JButton btnComplete = new JButton("Complete Selected");
        btnComplete.addActionListener(e -> {
            Task first = taskManager.getTasks().stream()
                    .filter(t -> !t.isCompleted())
                    .findFirst().orElse(null);
            if (first != null) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Mark \"" + first.getTitle() + "\" as completed?",
                        "Confirm Completion", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    taskManager.complete(first.getId());
                    refreshTaskList();
                    notifMgr.notifyTaskEvent(first, Notification.Type.COMPLETED);
                }
            }
        });
        p.add(btnComplete);

        JButton btnDelete = new JButton("Delete Selected");
        btnDelete.addActionListener(e -> {
            Task first = taskManager.getTasks().stream()
                    .filter(t -> !t.isCompleted())
                    .findFirst().orElse(null);
            if (first != null) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Delete task \"" + first.getTitle() + "\"?",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    taskManager.delete(first.getId());
                    refreshTaskList();
                    scheduleReminders();
                }
            }
        });
        p.add(btnDelete);

        JButton btnSettings = new JButton("Settings");
        btnSettings.addActionListener(e -> showSettings());
        p.add(btnSettings);

        return p;
    }

    private JPanel createNotificationPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Notifications"));
        JLabel lblUnread = new JLabel("Unread: " + unreadCount());
        p.add(lblUnread, BorderLayout.NORTH);
        List<Notification> unread = notifMgr.getNotifications().stream()
                .filter(n -> !n.isRead() && !n.isDismissed())
                .limit(5)
                .collect(Collectors.toList());
        JPanel listPanel = new JPanel();
        listLayout = new java.awt.GridLayout(Math.min(unread.size(), 5), 1);
        listPanel.setLayout(listLayout);
        for (Notification n : unread) {
            JLabel lbl = new JLabel((n.getTitle() != null ? n.getTitle() : "Notify") + ": " + 
                    (n.getMessage() != null ? n.getMessage().substring(0, Math.min(40, n.getMessage().length())) : ""));
            p.add(lbl, BorderLayout.CENTER);
        }
        if (unread.isEmpty()) {
            p.add(new JLabel("No notifications"), BorderLayout.CENTER);
        }
        return p;
    }

    private int unreadCount() {
        return (int) taskManager.getTasks().stream()
                .flatMap(t -> notifMgr.getNotifications().stream())
                .filter(n -> !n.isRead() && !n.isDismissed())
                .count();
    }

    java.awt.LayoutManager listLayout;

    private void refreshTaskList() {
        taskPanel.removeAll();
        List<Task> all = taskManager.getTasks();
        for (Task t : all) {
            taskPanel.add(createTaskCard(t));
        }
        refreshStats();
        taskPanel.revalidate();
        taskPanel.repaint();
    }

    private Component createTaskCard(Task task) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        card.setPreferredSize(new Dimension(360, 80));
        card.setBackground(Color.WHITE);

        JPanel info = new JPanel(new GridLayout(2, 1));
        JLabel lblTitle = new JLabel(task.getTitle());
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD));
        info.add(lblTitle);

        String dueLabel = "Due: " + task.getDueDate();
        if (task.isOverdue(LocalDate.now())) {
            dueLabel = "<html><u>OVERDUE: " + task.getDueDate() + "</u></html>";
        } else if (task.getDueDate().equals(LocalDate.now())) {
            dueLabel = "<html><b>Due Today</b></html>";
        }
        JLabel lblDue = new JLabel(dueLabel);
        info.add(lblDue);
        card.add(info, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        String status = task.isCompleted() ? "Completed" : "Pending";
        right.add(new JLabel(status));
        right.add(new JLabel("Post: " + task.getPostponementCount()));
        card.add(right, BorderLayout.EAST);

        return card;
    }

    private void showAddEditDialog(String id) {
        JDialog dialog = new JDialog(this, id == null ? "Add Task" : "Edit Task", true);
        dialog.setLayout(new java.awt.GridLayout(3, 2, 5, 5));
        dialog.setSize(300, 200);

        JTextField txtTitle = new JTextField();
        JTextArea txtDesc = new JTextArea();
        JTextField txtDue = new JTextField();
        txtDue.setToolTipText("Due date (ISO format, e.g. 2025-12-25)");

        if (id != null) {
            Task t = taskManager.getTasks().stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
            if (t != null) {
                txtTitle.setText(t.getTitle());
                txtDesc.setText(t.getDescription());
                txtDue.setText(t.getDueDate().toString());
            }
        }

        dialog.add(new JLabel("Title:"));
        dialog.add(txtTitle);
        dialog.add(new JLabel("Description:"));
        dialog.add(new JScrollPane(txtDesc));
        dialog.add(new JLabel("Due Date:"));
        dialog.add(txtDue);

        JButton btnOk = new JButton("OK");
        btnOk.addActionListener(e -> {
            try {
                LocalDate due = LocalDate.parse(txtDue.getText().trim());
                taskManager.add(txtTitle.getText().trim(),
                        txtDesc.getText().trim(),
                        due);
                notifMgr.notifyTaskEvent(taskManager.getTasks().get(taskManager.getTasks().size() - 1), Notification.Type.CREATED);
                refreshTaskList();
                scheduleReminders();
                dialog.dispose();
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid date format. Use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        dialog.add(btnOk);
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dialog.dispose());
        dialog.add(btnCancel);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showSettings() {
        JDialog dialog = new JDialog(this, "Settings", true);
        dialog.setLayout(new BorderLayout(5, 5));
        dialog.setSize(400, 300);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Settings UI - channel toggles, email config, tone, reminders, quiet hours"), BorderLayout.CENTER);
        dialog.add(panel, BorderLayout.CENTER);

        JButton btnSave = new JButton("Save");
        btnSave.addActionListener(e -> {
            storage.saveSettings(settings.toMap());
            notifMgr.getChannels().clear();
            if (settings.isInAppEnabled()) notifMgr.register(new InAppChannel());
            if (settings.isMacOSEnabled()) notifMgr.register(new MacOSChannel());
            if (settings.isEmailEnabled()) {
                EmailChannel email = new EmailChannel();
                email.setSmtp(settings.getEmailHost(), settings.getEmailPort(),
                        settings.getEmailUsername(), new char[0],
                        settings.getEmailFrom(), settings.getEmailRecipient(),
                        settings.isEmailAuth(), settings.isEmailStarttls());
                notifMgr.register(email);
            }
            scheduleReminders();
            dialog.dispose();
        });
        dialog.add(btnSave, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void scheduleReminders() {
        new ReminderScheduler(taskManager, notifMgr,
                (EmailChannel) notifMgr.getChannels().get("email"),
                settings).start();
    }}
