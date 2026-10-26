package com.tomorrow.notification;

import java.util.*;

/**
 * In-app channel: marks notifications delivered in-app. Persistence is handled by
 * NotificationManager; this channel only records delivery status.
 */
public class InAppChannel implements NotificationChannel {
    private boolean enabled = true;

    @Override
    public String name() {
        return "inApp";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean send(Notification notification) {
        notification.setChannelStatus(name(), enabled);
        return enabled;
    }
}
