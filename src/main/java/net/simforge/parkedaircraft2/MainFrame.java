package net.simforge.parkedaircraft2;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private JLabel simConnectLabel;
    private JLabel simStatusLabel;
    private JLabel inSimStatusLabel;
    private JLabel aircraftTitleLabel;
    private JLabel statusToRestoreLabel;
    private JLabel parkingStatusLabel;

    public MainFrame() throws HeadlessException {
        setTitle("Parked Aircraft");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

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

        setVisible(true);

        Timer timer = new Timer(200, e -> updateUi());
        timer.start();
    }

    private void updateUi() {
        final Logic logic = Logic.get();

        final Logic.TrackingState currentState = logic.getCurrentState();
        final String title = currentState != null ? currentState.title : "n/a";
        final String inSimStatus = currentState != null ? (currentState.inSimulation ? "YES" : "NO") : "n/a";
        final String parkingStatus = logic.getSimStartupSequence() == Logic.SimStartupSequence.FullyReady
                && currentState != null ?
                ("Parking status: " + (currentState.aircraft.onGround == 1 ? "On Ground   " : "") +
                        (currentState.aircraft.parkingBrake == 1 ? "Parking Brake SET   " : "") +
                        (currentState.aircraft.eng1running == 0 ? "Eng 1 OFF" : ""))
                : "";

        // todo ak sim connect status
        simStatusLabel.setText("SIM STATUS: " + logic.getSimStartupSequence());
        inSimStatusLabel.setText("IN SIM: " + inSimStatus);
        aircraftTitleLabel.setText("Aircraft: " + title);
        statusToRestoreLabel.setText((logic.getStatusToRestore() != null ? "THERE IS SAVED STATUS TO RESTORE" : ""));
        parkingStatusLabel.setText(parkingStatus);
    }
}
