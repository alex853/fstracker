package net.simforge.parkedaircraft2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainFrame extends JFrame {

    private TrayIcon trayIcon;

    private JLabel simConnectLabel;
    private JLabel simStatusLabel;
    private JLabel inSimStatusLabel;
    private JLabel aircraftTitleLabel;
    private JLabel statusToRestoreLabel;
    private JLabel parkingStatusLabel;

    public MainFrame() throws HeadlessException {
        setTitle("Parked Aircraft");
        setSize(400, 300);

        // ---- UI setup ----
        setLayout(new GridLayout(0, 1));

        simConnectLabel = new JLabel("SIM CONNECT - TODO");
        simStatusLabel = new JLabel();
        inSimStatusLabel = new JLabel();
        aircraftTitleLabel = new JLabel();
        statusToRestoreLabel = new JLabel();
        parkingStatusLabel = new JLabel();

        add(simConnectLabel);
        add(simStatusLabel);
        add(inSimStatusLabel);
        add(aircraftTitleLabel);
        add(statusToRestoreLabel);
        add(parkingStatusLabel);

        initTray();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                hideToTray();
            }
        });

        setVisible(true);

        Timer timer = new Timer(200, e -> updateUi());
        timer.start();
    }

    private void updateUi() {
        final Logic logic = Logic.get();

        final Logic.TrackingState currentState = logic.getCurrentState();
        final String title = currentState != null ? currentState.title : "n/a";
        final String inSimStatus = currentState != null ? (currentState.inSimulation ? "YES" : "NO") : "n/a";
        final String parkingStatus = logic.getSimStatus() == Logic.SimStatus.FullyReady
                && currentState != null ?
                ("Parking status: " + (currentState.aircraft.onGround == 1 ? "On Ground   " : "") +
                        (currentState.aircraft.parkingBrake == 1 ? "Parking Brake SET   " : "") +
                        (currentState.aircraft.eng1running == 0 ? "Eng 1 OFF" : ""))
                : "";

        simConnectLabel.setText("SIM CONNECT: " + (SimWorker.get().isConnected() ? "Connected" : "Disconnected"));
        simStatusLabel.setText("SIM STATUS: " + logic.getSimStatus());
        inSimStatusLabel.setText("IN SIM: " + inSimStatus);
        aircraftTitleLabel.setText("Aircraft: " + title);
        statusToRestoreLabel.setText((logic.getSavedAircraftToRestore() != null ? "THERE IS SAVED STATUS TO RESTORE" : ""));
        parkingStatusLabel.setText(parkingStatus);
    }

    private void initTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray not supported");
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();

        Image image = Toolkit.getDefaultToolkit().getImage(
                MainFrame.class.getResource("/radiolocator.png")
        );

        PopupMenu popupMenu = new PopupMenu();

        MenuItem showItem = new MenuItem("Show");
        showItem.addActionListener(e -> restoreFromTray());
        popupMenu.add(showItem);

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> {
            tray.remove(trayIcon);
            System.exit(0);
        });
        popupMenu.add(exitItem);

        trayIcon = new TrayIcon(image, "Parked Aircraft", popupMenu);
        trayIcon.setImageAutoSize(true);

        trayIcon.addActionListener(e -> restoreFromTray());

        try {
            tray.add(trayIcon);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideToTray() {
        setVisible(false);
    }

    private void restoreFromTray() {
        setVisible(true);
        setExtendedState(JFrame.NORMAL);
        toFront();
    }
}
