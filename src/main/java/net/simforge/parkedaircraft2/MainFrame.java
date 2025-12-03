package net.simforge.parkedaircraft2;

import net.miginfocom.swing.MigLayout;

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
    private JLabel simPositionCaptionLabel;
    private JLabel savedPositionCaptionLabel;
    private JLabel simPositionIcaoLabel;
    private JLabel savedPositionIcaoLabel;
    private JLabel distanceLabel;
    private JButton restoreButton;
    private JButton cancelButton;

    public MainFrame() throws HeadlessException {
        setTitle("Parked Aircraft");
        setSize(400, 300);

        buildUi();

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

    private void buildUi() {
        setLayout(new MigLayout("", "[25%][25%][25%][25%]", "[][][][][][][][]"));

        simConnectLabel = addCmp(new JLabel("SIM CONNECT", SwingConstants.CENTER), "cell 0 0 2 1, growx, align center");
        simStatusLabel = addCmp(new JLabel("SIM STATUS", SwingConstants.CENTER), "cell 2 0 2 1, growx, align center");

        inSimStatusLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 0 1 4 1, growx, align center");

        addCmp(new JLabel("Aircraft", SwingConstants.CENTER), "cell 0 2, growx, align center");
        aircraftTitleLabel = addCmp(new JLabel("", SwingConstants.LEFT), "cell 1 2 3 1, growx, align left");

        statusToRestoreLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 0 3 4 1, growx, align center");

        parkingStatusLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 0 4 4 1, growx, align center");

        simPositionCaptionLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 0 5 2 1, growx, align center");
        savedPositionCaptionLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 2 5 2 1, growx, align center");

        simPositionIcaoLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 0 6 2 1, growx, align center");
        savedPositionIcaoLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 2 6 2 1, growx, align center");

        distanceLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 0 7 4 1, growx, align center");

        restoreButton = new JButton("Restore");
        restoreButton.addActionListener(e -> restoreAction());

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> cancelAction());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.add(restoreButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, "cell 0 8 4 1, growx, align center");
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

        if (SimWorker.get().isConnected()) {
            simConnectLabel.setBackground(Color.GREEN);
            simConnectLabel.setText("SimConnect: Connected");
        } else {
            simConnectLabel.setBackground(Color.ORANGE);
            simConnectLabel.setText("SimConnect: Disconnected");
        }

        simStatusLabel.setText("Sim: " + logic.getSimStatus());
        simStatusLabel.setBackground(switch (logic.getSimStatus()) {
            case NotStarted -> Color.LIGHT_GRAY;
            case MainScreen, Loading -> Color.ORANGE;
            case ReadyToFly, FullyReady -> Color.GREEN;
        });

        inSimStatusLabel.setText("IN SIM: " + inSimStatus);
        aircraftTitleLabel.setText(title);

        switch (logic.getRestorationStatus()) {
            case NothingToRestore -> {
                statusToRestoreLabel.setText("");
                simPositionCaptionLabel.setText("");
                savedPositionCaptionLabel.setText("");
                simPositionIcaoLabel.setText("");
                savedPositionIcaoLabel.setText("");
                distanceLabel.setText("");
                restoreButton.setVisible(false);
                cancelButton.setVisible(false);
            }
            case WaitForSimReady, WaitForUserConfirmation -> {
                statusToRestoreLabel.setText("THERE IS SAVED STATUS TO RESTORE");
                simPositionCaptionLabel.setText("Sim Position");
                savedPositionCaptionLabel.setText("Saved Position");
                simPositionIcaoLabel.setText("ICAO");
                savedPositionIcaoLabel.setText("ICAO");
                distanceLabel.setText(formatDistance());

                if (!Logic.get().isAircraftAtToSavedPosition() && Logic.get().getRestorationStatus() == Logic.RestorationStatus.WaitForUserConfirmation) {
                    restoreButton.setVisible(true);
                    cancelButton.setVisible(true);
                } else {
                    restoreButton.setVisible(false);
                    cancelButton.setVisible(false);
                }
            }
        }
        parkingStatusLabel.setText(parkingStatus);
    }

    private void restoreAction() {
        Logic.get().whenUserRestores();
    }

    private void cancelAction() {
        Logic.get().whenUserCancelsRestoration();
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

    private <T extends JComponent> T addCmp(final T component, final String migParams) {
        add(component, migParams);
        component.setOpaque(true);
        return component;
    }

    private String formatDistance() {
        if (Logic.get().isAircraftAtToSavedPosition()) {
            return "At saved position";
        }

        final double distance = Logic.get().getDistanceToSavedPosition();
        if (distance > 10) {
            return (int) distance + " nm away";
        } else if (distance >= 0.1) {
            return ((int)(distance*10))*0.1 + " nm away";
        } else {
            return ((int)(distance*100))*0.01 + " nm away";
        }
    }
}
