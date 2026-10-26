package com.tomorrow.ui;

import com.tomorrow.notification.Notification;
import com.tomorrow.service.NotificationManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * In-app Notification Center displaying priority-sorted notifications with
 * unread badges, mark-as-read, dismiss, and jump-to-task actions.
 */
public class NotificationPanel extends JPanel {
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final NotificationManager notifMgr;
    private final Consumer<String> onSelectTask;
    private final Runnable onNotificationChanged;

    private JLabel lblHeader;
    private JPanel listContainer;

    public NotificationPanel(NotificationManager notifMgr, Consumer<String> onSelectTask, Runnable onNotificationChanged) {
        this.notifMgr = notifMgr;
        this.onSelectTask = onSelectTask;
        this.onNotificationChanged = onNotificationChanged;

        setLayout(new BorderLayout(4, 4));
        setBorder(new EmptyBorder(6, 6, 6, 6));

        initializeUI();
        refresh();
    }

    private void initializeUI() {
        JPanel topBar = new JPanel(new BorderLayout());
        lblHeader = new JLabel("Notifications");
        lblHeader.setFont(lblHeader.getFont().deriveFont(Font.BOLD, 13f));
        topBar.add(lblHeader, BorderLayout.WEST);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        JButton btnMarkAllRead = new JButton("Read All");
        btnMarkAllRead.setFont(btnMarkAllRead.getFont().deriveFont(11f));
        btnMarkAllRead.addActionListener(e -> {
            for (Notification n : notifMgr.getNotifications()) {
                if (!n.isRead()) {
                    notifMgr.markRead(n.getId());
                }
            }
            refresh();
            if (onNotificationChanged != null) onNotificationChanged.run();
        });
        btnBar.add(btnMarkAllRead);
        topBar.add(btnBar, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(listContainer);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        add(scroll, BorderLayout.CENTER);
    }

    public void refresh() {
        listContainer.removeAll();

        List<Notification> all = notifMgr.getNotifications();
        List<Notification> active = all.stream().filter(n -> !n.isDismissed()).toList();
        int unread = notifMgr.unreadCount();

        lblHeader.setText("Notifications (" + unread + " unread / " + active.size() + " total)");

        if (active.isEmpty()) {
            JPanel emptyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JLabel lblEmpty = new JLabel("No active notifications. Procrastinate peacefully!");
            lblEmpty.setFont(lblEmpty.getFont().deriveFont(Font.ITALIC));
            lblEmpty.setForeground(Color.GRAY);
            emptyPanel.add(lblEmpty);
            listContainer.add(emptyPanel);
        } else {
            for (Notification n : active) {
                listContainer.add(createNotificationCard(n));
                listContainer.add(Box.createVerticalStrut(4));
            }
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    private JPanel createNotificationCard(Notification n) {
        JPanel card = new JPanel(new BorderLayout(6, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, getPriorityColor(n.getPriority())),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                        new EmptyBorder(6, 8, 6, 8)
                )
        ));

        if (!n.isRead()) {
            card.setBackground(new Color(245, 248, 255));
        } else {
            card.setBackground(Color.WHITE);
        }

        // Header: [P1] Title + Time
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        String priorityText = n.getPriority() != null ? "[" + n.getPriority().name() + "] " : "";
        String titleText = priorityText + (n.getTitle() != null ? n.getTitle() : "Notification");
        JLabel lblTitle = new JLabel(titleText);
        lblTitle.setFont(lblTitle.getFont().deriveFont(n.isRead() ? Font.PLAIN : Font.BOLD, 12f));
        header.add(lblTitle, BorderLayout.WEST);

        String timeStr = n.getCreatedAt() != null ? TIME_FMT.format(n.getCreatedAt()) : "";
        JLabel lblTime = new JLabel(timeStr);
        lblTime.setFont(lblTime.getFont().deriveFont(10f));
        lblTime.setForeground(Color.GRAY);
        header.add(lblTime, BorderLayout.EAST);

        card.add(header, BorderLayout.NORTH);

        // Body message
        JLabel lblMsg = new JLabel("<html><body style='width: 220px;'>" + escapeHtml(n.getMessage()) + "</body></html>");
        lblMsg.setFont(lblMsg.getFont().deriveFont(11f));
        lblMsg.setForeground(new Color(60, 60, 60));
        card.add(lblMsg, BorderLayout.CENTER);

        // Action buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.setOpaque(false);

        if (n.getTaskId() != null && !n.getTaskId().isEmpty()) {
            JButton btnJump = new JButton("View Task");
            btnJump.setFont(btnJump.getFont().deriveFont(10f));
            btnJump.setMargin(new Insets(1, 4, 1, 4));
            btnJump.addActionListener(e -> {
                if (onSelectTask != null) onSelectTask.accept(n.getTaskId());
            });
            actions.add(btnJump);
        }

        if (!n.isRead()) {
            JButton btnRead = new JButton("Mark Read");
            btnRead.setFont(btnRead.getFont().deriveFont(10f));
            btnRead.setMargin(new Insets(1, 4, 1, 4));
            btnRead.addActionListener(e -> {
                notifMgr.markRead(n.getId());
                refresh();
                if (onNotificationChanged != null) onNotificationChanged.run();
            });
            actions.add(btnRead);
        }

        JButton btnDismiss = new JButton("Dismiss");
        btnDismiss.setFont(btnDismiss.getFont().deriveFont(10f));
        btnDismiss.setMargin(new Insets(1, 4, 1, 4));
        btnDismiss.addActionListener(e -> {
            notifMgr.dismiss(n.getId());
            refresh();
            if (onNotificationChanged != null) onNotificationChanged.run();
        });
        actions.add(btnDismiss);

        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private Color getPriorityColor(Notification.Priority p) {
        if (p == null) return Color.GRAY;
        return switch (p) {
            case P1 -> new Color(220, 53, 69);   // Red
            case P2 -> new Color(253, 126, 20);  // Orange
            case P3 -> new Color(255, 193, 7);   // Yellow / Amber
            case P4 -> new Color(13, 110, 253);  // Blue
            case P5 -> new Color(108, 117, 125); // Gray
        };
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
