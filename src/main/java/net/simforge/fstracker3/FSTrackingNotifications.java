package net.simforge.fstracker3;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class FSTrackingNotifications {

    private static final List<String> eventsToNotify = Arrays.asList(
            "Engine Startup",
            "Engine Shutdown",
            "Parking Brake Off",
            "Parking Brake On",
            "Takeoff",
            "Landing");


    private static final LinkedList<NotificationInfo> shownNotifications = new LinkedList<>();

    public synchronized static void showNotification(final Set<String> events) {
        removeExpiredNotifications();

        events.stream()
                .filter(eventsToNotify::contains)
                .forEach(event -> {
                    shownNotifications.stream()
                            .filter(n -> n.event.equals(event))
                            .findFirst()
                            .ifPresent(shownNotifications::remove);

                    show(event);
                });

        invalidateUI();
    }

    private static void removeExpiredNotifications() {
        while (!shownNotifications.isEmpty()
                && shownNotifications.getFirst().isExpired()) {
            shownNotifications.remove(0);
        }
    }

    private static void invalidateUI() {
        if (shownNotifications.isEmpty()) {
            frame.setVisible(false);
            return;
        }

        System.out.println("=== SHOWN NOTIFICATIONS ====");
        shownNotifications.forEach(n -> System.out.println(n.event));

        frame.setTitle(shownNotifications.getLast().event);
        frameLabel.setText(shownNotifications.getLast().event);
        frame.setFocusableWindowState(false);
        frame.setVisible(true);
    }

    private static JFrame frame;
    private static JLabel frameLabel;

    public static void init() {
        frame = new JFrame();

        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        final int frameWidth = 150;
        final int frameHeight = 30;
        final int marginRight = 10;
        final int marginBottom = 50;

        frame.setSize(frameWidth, frameHeight);
        frame.setLocation((int) screenSize.getWidth() - frameWidth - marginRight, (int) screenSize.getHeight() - frameHeight - marginBottom);
        frame.setLayout(null);//using no layout managers
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setOpacity(0.7f);
        frame.setBackground(Color.yellow);

        frameLabel = new JLabel("label", SwingConstants.CENTER);
        frameLabel.setBounds(0, 0, frameWidth, frameHeight);
        frameLabel.setBackground(Color.yellow);

        frame.add(frameLabel);
    }

    public static synchronized void dispose() {
    }

    private static void show(final String event) {
        shownNotifications.add(new NotificationInfo(event));
    }

    private static class NotificationInfo {
        private final long shownSince = System.currentTimeMillis();
        private final String event;

        public NotificationInfo(final String event) {
            this.event = event;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - shownSince > 10000;
        }
    }
}
