package com.tomorrow.notification;

/**
 * A notification delivery channel. Channels do not decide priority or generate task logic.
 */
public interface NotificationChannel {
    String name();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    /**
     * Deliver the notification. Returns true if accepted for delivery (not necessarily read).
     * Implementations must not throw; failures are recorded and reported via result.
     */
    boolean send(Notification notification);
}
