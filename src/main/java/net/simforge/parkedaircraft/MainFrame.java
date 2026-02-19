package net.simforge.parkedaircraft;

import net.miginfocom.swing.MigLayout;
import net.simforge.ourairports.OurAirportsIndex;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.DecimalFormat;

public class MainFrame extends JFrame {

    private static final OurAirportsIndex airportsIndex;

    static {
        try {
            airportsIndex = OurAirportsIndex.loadFromResources();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private TrayIcon trayIcon;

    private JLabel simConnectLabel;
    private JLabel simStatusLabel;
    private JLabel aircraftTitleLabel;
    private JLabel statusToRestoreLabel;
    private JLabel simPositionCaptionLabel;
    private JLabel savedDataCaptionLabel;
    private JLabel simPositionIcaoLabel;
    private JLabel savedDataIcaoLabel;
    private JLabel simDataFuelLabel;
    private JLabel savedDataFuelLabel;
    private JLabel distanceLabel;
    private JButton restoreButton;
    private JButton cancelButton;
    private JLabel inSimStatusLabel;
    private JLabel hasSavedStatusLabel;
    private JLabel parkingStatusLabel;
    private JLabel fuelStatusLabel;

    public MainFrame(boolean keepHidden) throws HeadlessException {
        setTitle("Parked Aircraft");
        setSize(400, 350);

        buildUi();

        initTray();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                hideToTray();
            }
        });

        if (!keepHidden) {
            setVisible(true);
        }

        Timer timer = new Timer(200, e -> updateUi());
        timer.start();
    }

    private void buildUi() {
        setLayout(new MigLayout("insets 0, gap 0", "[25%][25%][25%][25%]", "[][][][][][][][][]"));

        simConnectLabel = addCmp(new JLabel("SIM CONNECT", SwingConstants.CENTER), "cell 0 0 2 1, growx, align center");
        simStatusLabel = addCmp(new JLabel("SIM STATUS", SwingConstants.CENTER), "cell 2 0 2 1, growx, align center");

        addCmp(new JLabel("Aircraft", SwingConstants.CENTER), "cell 0 1, growx, align center");
        aircraftTitleLabel = addCmp(new JLabel("", SwingConstants.LEFT), "cell 1 1 3 1, growx, align left");

        statusToRestoreLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 0 2 4 1, growx, align center");

        simPositionCaptionLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 0 3 2 1, growx, align center");
        savedDataCaptionLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 2 3 2 1, growx, align center");

        simPositionIcaoLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 0 4 2 1, growx, align center");
        savedDataIcaoLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 2 4 2 1, growx, align center");

        distanceLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 0 5 4 1, growx, align center");

        simDataFuelLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 0 6 2 1, growx, align center");
        savedDataFuelLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 2 6 2 1, growx, align center");

        restoreButton = new JButton("Restore");
        restoreButton.addActionListener(e -> restoreAction());

        // todo ak1 deprecated?
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> cancelAction());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.add(restoreButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, "cell 0 7 4 1, growx, align center");

        inSimStatusLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 0 8 4 1, growx, align center");
        inSimStatusLabel.setBackground(Color.LIGHT_GRAY);

        hasSavedStatusLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 0 9 4 1, growx, align center");
        hasSavedStatusLabel.setBackground(Color.LIGHT_GRAY);

        parkingStatusLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 0 10 4 1, growx, align center");
        parkingStatusLabel.setBackground(Color.LIGHT_GRAY);

        fuelStatusLabel = addCmp(new JLabel("", SwingConstants.CENTER), "cell 0 11 4 1, growx, align center");
        fuelStatusLabel.setBackground(Color.LIGHT_GRAY);
    }

    private void updateUi() {
        final Logic.State state = Logic.get().getState();

        final Logic.SimStatus simStatus = state.getSimStatus();

        final Logic.TrackingState currentState = state.getTrackingState();
        final String title = currentState != null ? currentState.title : "n/a";
        final String inSimStatus = currentState != null ? (currentState.inSimulation ? "YES" : "NO") : "n/a";

        final Logic.RestorationStatus restorationStatus = state.getRestorationStatus();

        final String parkingStatus = simStatus == Logic.SimStatus.FullyReady
                && currentState != null ?
                ("Parking status: " + (currentState.aircraft.isOnGround() ? "On Ground   " : "") +
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

        simStatusLabel.setText("Sim: " + simStatus);
        simStatusLabel.setBackground(switch (simStatus) {
            case NotStarted -> Color.LIGHT_GRAY;
            case MainScreen, Loading -> Color.ORANGE;
            case ReadyToFly, FullyReady -> Color.GREEN;
        });

        aircraftTitleLabel.setText(title);

        final boolean showSavedData = switch (restorationStatus) {
            case NothingToRestore -> false;
            case WaitForSimReady, WaitForUserConfirmation -> true;
        };

        if (showSavedData) {
            // todo ak1 rethink statusToRestoreLabel.setText("THERE IS SAVED STATUS TO RESTORE");
            savedDataCaptionLabel.setText("Saved Data");
            savedDataIcaoLabel.setText(findIcao(state.getSavedAircraftToRestore().latitude, state.getSavedAircraftToRestore().longitude));
            savedDataFuelLabel.setText(df0_0.format(state.getSavedAircraftToRestore().fuel.getTotal()) + " gal");
        } else {
            // todo ak1 rethink statusToRestoreLabel.setText("no saved status found");
            savedDataCaptionLabel.setText("");
            savedDataIcaoLabel.setText("");
            savedDataFuelLabel.setText("");
        }

        final boolean showSimData = switch (simStatus) {
            default -> false;
            case ReadyToFly, FullyReady -> true;
        };

        if (showSimData) {
            simPositionCaptionLabel.setText("Sim Data");
            if (state.getTrackingState().aircraft.isOnGround()) {
                simPositionIcaoLabel.setText(findIcao(state.getTrackingState().aircraft.latitude, state.getTrackingState().aircraft.longitude));
            } else {
                simPositionIcaoLabel.setText("Flying");
            }
            simDataFuelLabel.setText(df0_0.format(state.getTrackingState().aircraft.getTotalFuelQuantity()) + " gal");
        } else {
            simPositionCaptionLabel.setText("");
            simPositionIcaoLabel.setText("");
            simDataFuelLabel.setText("");
        }

        if (showSimData && showSavedData) {
            distanceLabel.setText(formatDistance(state));
        } else {
            distanceLabel.setText("");
        }

        if (showSavedData
                && !state.isAircraftAtToSavedPosition()
                && restorationStatus == Logic.RestorationStatus.WaitForUserConfirmation) {
            restoreButton.setVisible(true);
            cancelButton.setVisible(true);
        } else {
            restoreButton.setVisible(false);
            cancelButton.setVisible(false);
        }

        inSimStatusLabel.setText("IN SIM: " + inSimStatus);
        hasSavedStatusLabel.setText("Saved status: "
                + (state.getTrackingState() != null && Tools.loadIfExists(state.getTrackingState().title) != null ? "EXISTS" : "not found")
                + " R/S: " + state.getRestorationStatus().name().substring(0, 10) + "...");
        parkingStatusLabel.setText(parkingStatus);
        fuelStatusLabel.setText("Fuel: " + (state.getTrackingState() != null
                ? formatFuelTanks(state.getTrackingState().aircraft)
                : "n/a"));

        if (state.isBringFormToFront()) {
            if (!isVisible()) {
                setVisible(true);
            }

            setExtendedState(getExtendedState() & ~JFrame.ICONIFIED);

            toFront();
            requestFocus();
            requestFocusInWindow();

            setAlwaysOnTop(true);
            setAlwaysOnTop(false);

            Logic.get().whenBringFormToFront();
        }
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
        component.setBorder(new EmptyBorder(6, 8, 6, 8));
        return component;
    }

    private static final DecimalFormat df0_o = new DecimalFormat("0.#");
    private static final DecimalFormat df0_0 = new DecimalFormat("0.0");
    private static final DecimalFormat df0_00 = new DecimalFormat("0.00");

    private String formatDistance(final Logic.State state) {
        if (state.isAircraftAtToSavedPosition()) {
            return "At the saved position";
        }

        final double distance = state.getDistanceToSavedPosition();
        if (distance > 10) {
            return (int) distance + " nm away";
        } else if (distance >= 0.1) {
            return df0_0.format(distance) + " nm away";
        } else {
            return df0_00.format(distance) + " nm away";
        }
    }

    private String formatFuelTanks(final AircraftStateDefinition aircraft) {
        return df0_0.format(aircraft.getTotalFuelQuantity())
                + " Tanks: " + df0_o.format(aircraft.fuelCenterQuantity) + "/"
                + df0_o.format(aircraft.fuelCenter2Quantity) + "/"
                + df0_o.format(aircraft.fuelCenter3Quantity) + "/"
                + df0_o.format(aircraft.fuelLeftMainQuantity) + "/"
                + df0_o.format(aircraft.fuelRightMainQuantity) + "/"
                + df0_o.format(aircraft.fuelLeftAuxQuantity) + "/"
                + df0_o.format(aircraft.fuelRightAuxQuantity) + "/"
                + df0_o.format(aircraft.fuelLeftTipQuantity) + "/"
                + df0_o.format(aircraft.fuelRightTipQuantity) + "/"
                + df0_o.format(aircraft.fuelExternal1Quantity) + "/"
                + df0_o.format(aircraft.fuelExternal2Quantity);
    }

    private String findIcao(double lat, double lon) {
        OurAirportsIndex.AirportAndDistance nearest = airportsIndex.findNearestIcaoIndexed(lat, lon);
        return nearest != null && nearest.distance < 3 ? nearest.airport.icao : "n/a";
    }
}
