package net.simforge.parkedaircraft2;

import flightsim.simconnect.config.ConfigurationNotFoundException;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

// todo ak0 queues between SimWorker, Logic and MainFrame, no direct command calls
// todo ak0 implement icao codes
// todo ak0 fuel support
// todo ak0 no gaps in colored area
// todo ak1 improved look-and-feel
// todo ak1 move debug rows to bottom and fill them with gray?
// todo ak1 another icon
// todo ak1 bring form to front
// todo ak1 60 seconds timer before autocancellation
// todo ak2 simconnect.text probe
// todo ak2 hide at auto-start
// todo ak2 limit max distance for simple movement
// todo ak2 non-resizable frame
// todo ak3 FLT-movement support
// todo ak3 refueling workflow with truck, time simulation, etc
// todo ak3 integrate tracking code into the app, rename it into fs-tracker?
// todo ak3
public class ParkedAircraft {

    public static void main(String[] args) throws IOException, ConfigurationNotFoundException {
        Logic.get().start();
        SimWorker.get().start();

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.put("Label.font", new Font("Verdana", Font.PLAIN, 14));
                UIManager.put("Button.font", new Font("Verdana", Font.PLAIN, 14));
                UIManager.put("TextField.font", new Font("Verdana", Font.PLAIN, 14));

                new MainFrame();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
