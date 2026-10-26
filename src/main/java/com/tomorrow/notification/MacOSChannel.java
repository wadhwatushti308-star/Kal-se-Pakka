package com.tomorrow.notification;

import java.util.concurrent.TimeUnit;

/**
 * macOS desktop notification channel. Uses osascript to display a notification via
 * AppleScript when available. Delivery failures are recorded but never propagated as
 * exceptions; the notification remains in the in-app center.
 */
public class MacOSChannel implements NotificationChannel {
    private boolean enabled = false;

    @Override
    public String name() {
        return "macOS";
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
        if (!enabled || !isSupported()) {
            if (notification != null) {
                notification.setChannelStatus(name(), false);
            }
            return false;
        }
        try {
            String title = notification.getTitle() != null ? notification.getTitle() : "Tomorrow";
            String msg = notification.getMessage() != null ? notification.getMessage() : "";
            ProcessBuilder pb = new ProcessBuilder(
                    "osascript", "-e",
                    "display notification \"" + escape(msg) + "\" with title \"" + escape(title) + "\"");
            Process p = pb.start();
            boolean finished = p.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                notification.setChannelStatus(name(), false);
                return false;
            }
            boolean ok = p.exitValue() == 0;
            notification.setChannelStatus(name(), ok);
            return ok;
        } catch (Exception e) {
            if (notification != null) {
                notification.setChannelStatus(name(), false);
            }
            return false;
        }
    }

    public boolean isSupported() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("mac");
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }
}
