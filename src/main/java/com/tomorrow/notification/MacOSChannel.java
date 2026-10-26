package com.tomorrow.notification;

import java.io.*;

/**
 * macOS desktop notification channel. Uses osascript to display a notification via
 * AppleScript when available. Delivery failures are recorded but never propagated as
 * exceptions; the notification remains in the in-app center.
 * <p>
 * NOTE: macOS delivery has not been verified on a target Mac. Treat as unverified.
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
            notification.setChannelStatus(name(), false);
            return false;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "osascript", "-e",
                    "display notification \"" + escape(notification.getMessage()) + "\" "
                            + "with title \"" + escape(notification.getTitle()) + "\"");
            Process p = pb.start();
            int code = p.waitFor();
            boolean ok = code == 0;
            notification.setChannelStatus(name(), ok);
            return ok;
        } catch (Exception e) {
            notification.setChannelStatus(name(), false);
            return false;
        }
    }

    private boolean isSupported() {
        try {
            Process p = new ProcessBuilder("uname", "-s").start();
            p.waitFor();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String os = r.readLine();
            return "Darwin".equalsIgnoreCase(os);
        } catch (Exception e) {
            return false;
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
