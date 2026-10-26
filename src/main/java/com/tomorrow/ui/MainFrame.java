package com.tomorrow.ui;

import com.tomorrow.model.Task;
import com.tomorrow.notification.EmailChannel;
import com.tomorrow.notification.Notification;
import com.tomorrow.service.AppSettings;
import com.tomorrow.service.NotificationManager;
import com.tomorrow.service.PriorityCalculator;
import com.tomorrow.service.ReminderScheduler;
import com.tomorrow.service.TaskManager;
import com.tomorrow.storage.JsonStorage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Main application window with dashboard, filter tabs, task list,
 * action controls, in-app notification center, and settings integration.
 */
public class MainFrame extends JFrame {
    private final TaskManager taskManager;
    private final NotificationManager notifMgr;
    private final AppSettings settings;
    private final JsonStorage storage;
    private final PriorityCalculator priorityCalc = new PriorityCalculator();

    private ReminderScheduler reminderScheduler;
    private Task selectedTask = null;
    private TaskManager.Filter currentFilter = TaskManager.Filter.ALL;

    private JLabel lblDashboard;
    private JPanel taskListPanel;
    private NotificationPanel notificationPanel;

    private JButton btnFilterAll;
    private JButton btnFilterPending;
    private JButton btnFilterCompleted;
    private JButton btnFilterOverdue;

    private JButton btnPostponeSelected;
    private JButton btnCompleteSelected;
    private JButton btnEditSelected;
    private JButton btnDeleteSelected;

    public MainFrame(TaskManager taskManager, NotificationManager notifMgr, AppSettings settings, JsonStorage storage) {
        this.taskManager = taskManager;
        this.notifMgr = notifMgr;
        this.settings = settings;
        this.storage = storage;

        setTitle("Tomorrow — Task Manager for Procrastinators");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 680));
        setPreferredSize(new Dimension(1080, 740));
        setLocationRelativeTo(null);

        initializeUI();
        initScheduler();

        // Load tasks and notifications from disk
        taskManager.load(storage.loadTasks());
        notifMgr.load(storage.loadNotifications());

        // Refresh UI
        refreshUI();

        // Reconcile missed reminders without duplicate bursts
        reminderScheduler.reconcileOnLaunch();

        // Clean shutdown on close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (reminderScheduler != null) {
                    reminderScheduler.shutdown();
                }
            }
        });
    }

    private void initScheduler() {
        if (reminderScheduler != null) {
            reminderScheduler.stop();
        }
        EmailChannel emailChannel = (EmailChannel) notifMgr.getChannels().get("email");
        reminderScheduler = new ReminderScheduler(taskManager, notifMgr, emailChannel, settings);
        reminderScheduler.start();
    }

    private void initializeUI() {
        getContentPane().setLayout(new BorderLayout(0, 0));

        // Top: Dashboard + Filter Bar
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.add(createDashboardPanel());
        topContainer.add(createFilterBar());
        getContentPane().add(topContainer, BorderLayout.NORTH);

        // Center: SplitPane with Task List (left) and Notification Panel (right)
        taskListPanel = new JPanel();
        taskListPanel.setLayout(new BoxLayout(taskListPanel, BoxLayout.Y_AXIS));
        taskListPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane taskScroll = new JScrollPane(taskListPanel);
        taskScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Tasks",
                TitledBorder.LEFT, TitledBorder.TOP));
        taskScroll.getVerticalScrollBar().setUnitIncrement(16);

        notificationPanel = new NotificationPanel(notifMgr, this::selectAndScrollToTask, this::refreshUI);
        notificationPanel.setPreferredSize(new Dimension(320, 400));
        notificationPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Notification Center",
                TitledBorder.LEFT, TitledBorder.TOP));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, taskScroll, notificationPanel);
        splitPane.setResizeWeight(0.68);
        splitPane.setDividerSize(6);
        getContentPane().add(splitPane, BorderLayout.CENTER);

        // Bottom: Action Bar
        getContentPane().add(createActionBar(), BorderLayout.SOUTH);
    }

    private JPanel createDashboardPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        p.setBackground(new Color(248, 249, 250));
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(222, 226, 230)));

        lblDashboard = new JLabel("Loading statistics...");
        lblDashboard.setFont(lblDashboard.getFont().deriveFont(Font.BOLD, 13f));
        p.add(lblDashboard);
        return p;
    }

    private JPanel createFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));

        btnFilterAll = new JButton("All");
        btnFilterPending = new JButton("Pending");
        btnFilterCompleted = new JButton("Completed");
        btnFilterOverdue = new JButton("Overdue");

        btnFilterAll.addActionListener(e -> setFilter(TaskManager.Filter.ALL));
        btnFilterPending.addActionListener(e -> setFilter(TaskManager.Filter.PENDING));
        btnFilterCompleted.addActionListener(e -> setFilter(TaskManager.Filter.COMPLETED));
        btnFilterOverdue.addActionListener(e -> setFilter(TaskManager.Filter.OVERDUE));

        bar.add(new JLabel("Filter:"));
        bar.add(btnFilterAll);
        bar.add(btnFilterPending);
        bar.add(btnFilterCompleted);
        bar.add(btnFilterOverdue);

        return bar;
    }

    private void setFilter(TaskManager.Filter filter) {
        this.currentFilter = filter;
        refreshTaskList();
        updateFilterButtons();
    }

    private void updateFilterButtons() {
        TaskManager.Stats s = taskManager.stats();
        btnFilterAll.setText("All (" + s.total() + ")");
        btnFilterPending.setText("Pending (" + s.pending() + ")");
        btnFilterCompleted.setText("Completed (" + s.completed() + ")");
        btnFilterOverdue.setText("Overdue (" + s.overdue() + ")");

        btnFilterAll.setFont(btnFilterAll.getFont().deriveFont(currentFilter == TaskManager.Filter.ALL ? Font.BOLD : Font.PLAIN));
        btnFilterPending.setFont(btnFilterPending.getFont().deriveFont(currentFilter == TaskManager.Filter.PENDING ? Font.BOLD : Font.PLAIN));
        btnFilterCompleted.setFont(btnFilterCompleted.getFont().deriveFont(currentFilter == TaskManager.Filter.COMPLETED ? Font.BOLD : Font.PLAIN));
        btnFilterOverdue.setFont(btnFilterOverdue.getFont().deriveFont(currentFilter == TaskManager.Filter.OVERDUE ? Font.BOLD : Font.PLAIN));
    }

    private JPanel createActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(222, 226, 230)));

        JButton btnAdd = new JButton("+ Add Task");
        btnAdd.setFont(btnAdd.getFont().deriveFont(Font.BOLD));
        btnAdd.addActionListener(e -> showAddEditDialog(null));
        bar.add(btnAdd);

        btnPostponeSelected = new JButton("Postpone (+1 Day)");
        btnPostponeSelected.addActionListener(e -> {
            if (selectedTask != null && !selectedTask.isCompleted()) {
                postponeTask(selectedTask);
            }
        });
        bar.add(btnPostponeSelected);

        btnCompleteSelected = new JButton("Complete");
        btnCompleteSelected.addActionListener(e -> {
            if (selectedTask != null && !selectedTask.isCompleted()) {
                completeTask(selectedTask);
            }
        });
        bar.add(btnCompleteSelected);

        btnEditSelected = new JButton("Edit");
        btnEditSelected.addActionListener(e -> {
            if (selectedTask != null) {
                showAddEditDialog(selectedTask.getId());
            }
        });
        bar.add(btnEditSelected);

        btnDeleteSelected = new JButton("Delete");
        btnDeleteSelected.addActionListener(e -> {
            if (selectedTask != null) {
                deleteTask(selectedTask);
            }
        });
        bar.add(btnDeleteSelected);

        bar.add(Box.createHorizontalStrut(16));

        JButton btnSettings = new JButton("Settings...");
        btnSettings.addActionListener(e -> showSettingsDialog());
        bar.add(btnSettings);

        updateActionButtons();
        return bar;
    }

    private void updateActionButtons() {
        boolean hasSelection = selectedTask != null;
        boolean isPending = hasSelection && !selectedTask.isCompleted();

        btnPostponeSelected.setEnabled(isPending);
        btnCompleteSelected.setEnabled(isPending);
        btnEditSelected.setEnabled(hasSelection);
        btnDeleteSelected.setEnabled(hasSelection);
    }

    public void refreshUI() {
        refreshStats();
        refreshTaskList();
        updateFilterButtons();
        updateActionButtons();
        if (notificationPanel != null) {
            notificationPanel.refresh();
        }
    }

    private void refreshStats() {
        TaskManager.Stats s = taskManager.stats();
        lblDashboard.setText(String.format(
                "Total Tasks: %d   |   Pending: %d   |   Completed: %d   |   Overdue: %d   |   Total Postponements: %d",
                s.total(), s.pending(), s.completed(), s.overdue(), s.postponements()
        ));
    }

    private void refreshTaskList() {
        taskListPanel.removeAll();
        List<Task> filtered = taskManager.filter(currentFilter);

        if (filtered.isEmpty()) {
            JPanel emptyPnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JLabel lblEmpty = new JLabel(currentFilter == TaskManager.Filter.ALL
                    ? "No tasks yet! Click '+ Add Task' to plan what you'll postpone tomorrow."
                    : "No tasks found under filter '" + currentFilter + "'.");
            lblEmpty.setFont(lblEmpty.getFont().deriveFont(Font.ITALIC, 13f));
            lblEmpty.setForeground(Color.GRAY);
            emptyPnl.add(lblEmpty);
            taskListPanel.add(emptyPnl);
        } else {
            for (Task t : filtered) {
                taskListPanel.add(createTaskCard(t));
                taskListPanel.add(Box.createVerticalStrut(8));
            }
        }

        taskListPanel.revalidate();
        taskListPanel.repaint();
    }

    private JPanel createTaskCard(Task task) {
        boolean isSelected = selectedTask != null && selectedTask.getId().equals(task.getId());
        LocalDate today = LocalDate.now();

        JPanel card = new JPanel(new BorderLayout(8, 6));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        isSelected ? new Color(13, 110, 253) : new Color(220, 224, 230),
                        isSelected ? 2 : 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        card.setBackground(isSelected ? new Color(240, 247, 255) : Color.WHITE);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        // Click to select
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                selectedTask = task;
                refreshTaskList();
                updateActionButtons();
            }
        });

        // Top info: Title + Priority + Status Badge
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel lblTitle = new JLabel(task.getTitle());
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 14f));
        if (task.isCompleted()) {
            lblTitle.setForeground(new Color(108, 117, 125));
        }
        top.add(lblTitle, BorderLayout.WEST);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        badges.setOpaque(false);

        Notification.Priority priority = priorityCalc.priorityFor(task.getDueDate(), today);
        JLabel lblPriority = new JLabel("[" + priority.name() + "]");
        lblPriority.setFont(lblPriority.getFont().deriveFont(Font.BOLD, 11f));
        lblPriority.setForeground(getPriorityColor(priority));
        badges.add(lblPriority);

        if (task.getPostponementCount() > 0) {
            JLabel lblPost = new JLabel("Pushed " + task.getPostponementCount() + "x");
            lblPost.setFont(lblPost.getFont().deriveFont(11f));
            lblPost.setForeground(new Color(150, 80, 20));
            badges.add(lblPost);
        }

        JLabel lblStatus = new JLabel(task.isCompleted() ? "Completed" : "Pending");
        lblStatus.setFont(lblStatus.getFont().deriveFont(Font.BOLD, 11f));
        lblStatus.setForeground(task.isCompleted() ? new Color(25, 135, 84) : new Color(13, 110, 253));
        badges.add(lblStatus);

        top.add(badges, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        // Center: Description (if any)
        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            JLabel lblDesc = new JLabel(task.getDescription());
            lblDesc.setFont(lblDesc.getFont().deriveFont(12f));
            lblDesc.setForeground(new Color(90, 90, 90));
            card.add(lblDesc, BorderLayout.CENTER);
        }

        // Bottom: Due date info + Quick action buttons
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);

        String dueText;
        Color dueColor;
        if (task.isCompleted()) {
            dueText = "Finished on " + (task.getCompletionDate() != null ? task.getCompletionDate() : "today");
            dueColor = new Color(108, 117, 125);
        } else if (task.isOverdue(today)) {
            long days = ChronoUnit.DAYS.between(task.getDueDate(), today);
            dueText = "OVERDUE by " + days + " day(s) (due " + task.getDueDate() + ")";
            dueColor = new Color(220, 53, 69);
        } else if (task.getDueDate().equals(today)) {
            dueText = "DUE TODAY (" + task.getDueDate() + ")";
            dueColor = new Color(217, 119, 6);
        } else {
            long days = ChronoUnit.DAYS.between(today, task.getDueDate());
            dueText = "Due in " + days + " day(s) (" + task.getDueDate() + ")";
            dueColor = new Color(40, 116, 166);
        }

        JLabel lblDueDate = new JLabel(dueText);
        lblDueDate.setFont(lblDueDate.getFont().deriveFont(Font.BOLD, 11f));
        lblDueDate.setForeground(dueColor);
        bottom.add(lblDueDate, BorderLayout.WEST);

        // Quick action buttons directly on the card
        JPanel cardActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        cardActions.setOpaque(false);

        if (!task.isCompleted()) {
            JButton btnCardPostpone = new JButton("Postpone");
            btnCardPostpone.setFont(btnCardPostpone.getFont().deriveFont(10f));
            btnCardPostpone.setMargin(new Insets(1, 4, 1, 4));
            btnCardPostpone.addActionListener(e -> postponeTask(task));
            cardActions.add(btnCardPostpone);

            JButton btnCardComplete = new JButton("Complete");
            btnCardComplete.setFont(btnCardComplete.getFont().deriveFont(10f));
            btnCardComplete.setMargin(new Insets(1, 4, 1, 4));
            btnCardComplete.addActionListener(e -> completeTask(task));
            cardActions.add(btnCardComplete);
        }

        JButton btnCardEdit = new JButton("Edit");
        btnCardEdit.setFont(btnCardEdit.getFont().deriveFont(10f));
        btnCardEdit.setMargin(new Insets(1, 4, 1, 4));
        btnCardEdit.addActionListener(e -> showAddEditDialog(task.getId()));
        cardActions.add(btnCardEdit);

        JButton btnCardDelete = new JButton("Delete");
        btnCardDelete.setFont(btnCardDelete.getFont().deriveFont(10f));
        btnCardDelete.setMargin(new Insets(1, 4, 1, 4));
        btnCardDelete.addActionListener(e -> deleteTask(task));
        cardActions.add(btnCardDelete);

        bottom.add(cardActions, BorderLayout.EAST);
        card.add(bottom, BorderLayout.SOUTH);

        return card;
    }

    private void postponeTask(Task task) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Push \"" + task.getTitle() + "\" to " + task.getDueDate().plusDays(1) + "?\n"
                        + "Remember: Tomorrow's problem is tomorrow's problem.",
                "Confirm Postpone", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            taskManager.postpone(task.getId());
            notifMgr.notifyTaskEvent(task, Notification.Type.POSTPONED);
            refreshUI();
        }
    }

    private void completeTask(Task task) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Mark \"" + task.getTitle() + "\" complete?\nAre you sure you want to give up procrastinating on this?",
                "Confirm Complete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            taskManager.complete(task.getId());
            notifMgr.notifyTaskEvent(task, Notification.Type.COMPLETED);
            refreshUI();
        }
    }

    private void deleteTask(Task task) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Permanently delete task \"" + task.getTitle() + "\"?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            if (selectedTask != null && selectedTask.getId().equals(task.getId())) {
                selectedTask = null;
            }
            taskManager.delete(task.getId());
            refreshUI();
        }
    }

    private void selectAndScrollToTask(String taskId) {
        taskManager.getTask(taskId).ifPresent(task -> {
            this.selectedTask = task;
            this.currentFilter = TaskManager.Filter.ALL;
            refreshUI();
        });
    }

    private void showAddEditDialog(String taskId) {
        boolean isEdit = taskId != null;
        Task existing = isEdit ? taskManager.getTask(taskId).orElse(null) : null;

        JDialog dialog = new JDialog(this, isEdit ? "Edit Task" : "Add Task", true);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.setSize(420, 320);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(12, 14, 8, 14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtTitle = new JTextField(existing != null ? existing.getTitle() : "", 20);
        JTextArea txtDesc = new JTextArea(existing != null && existing.getDescription() != null ? existing.getDescription() : "", 4, 20);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(txtDesc);

        LocalDate defaultDate = existing != null ? existing.getDueDate() : LocalDate.now();
        JTextField txtDueDate = new JTextField(defaultDate.toString(), 12);
        txtDueDate.setToolTipText("YYYY-MM-DD format. Default is today.");

        // Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.25;
        form.add(new JLabel("Title *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        form.add(txtTitle, gbc);

        // Due date
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.25;
        form.add(new JLabel("Due Date:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        JPanel duePnl = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        duePnl.add(txtDueDate);
        JButton btnToday = new JButton("Today");
        btnToday.addActionListener(e -> txtDueDate.setText(LocalDate.now().toString()));
        duePnl.add(Box.createHorizontalStrut(6));
        duePnl.add(btnToday);
        form.add(duePnl, gbc);

        // Description
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.25;
        form.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        form.add(descScroll, gbc);

        dialog.add(form, BorderLayout.CENTER);

        // Buttons
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton(isEdit ? "Save Changes" : "Create Task");
        btnSave.addActionListener(e -> {
            String title = txtTitle.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Task title cannot be blank.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            LocalDate due;
            try {
                String dueStr = txtDueDate.getText().trim();
                due = dueStr.isEmpty() ? LocalDate.now() : LocalDate.parse(dueStr);
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid date format. Please use YYYY-MM-DD.", "Date Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String desc = txtDesc.getText().trim();

            if (isEdit && existing != null) {
                taskManager.edit(existing.getId(), title, desc, due);
                selectedTask = existing;
            } else {
                Task created = taskManager.add(title, desc, due);
                selectedTask = created;
                notifMgr.notifyTaskEvent(created, Notification.Type.CREATED);
            }

            refreshUI();
            dialog.dispose();
        });

        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dialog.dispose());

        btnBar.add(btnCancel);
        btnBar.add(btnSave);
        dialog.add(btnBar, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void showSettingsDialog() {
        EmailChannel emailChannel = (EmailChannel) notifMgr.getChannels().get("email");
        SettingsDialog dialog = new SettingsDialog(this, settings, storage, notifMgr, emailChannel, () -> {
            initScheduler();
            refreshUI();
        });
        dialog.setVisible(true);
    }

    private Color getPriorityColor(Notification.Priority p) {
        if (p == null) return Color.GRAY;
        return switch (p) {
            case P1 -> new Color(220, 53, 69);   // Red
            case P2 -> new Color(253, 126, 20);  // Orange
            case P3 -> new Color(217, 119, 6);   // Amber
            case P4 -> new Color(13, 110, 253);  // Blue
            case P5 -> new Color(108, 117, 125); // Gray
        };
    }
}
