package net.simforge.parkedaircraft2;

import flightsim.simconnect.config.ConfigurationNotFoundException;

import javax.swing.*;
import java.io.IOException;

public class ParkedAircraft {

    public static void main(String[] args) throws IOException, ConfigurationNotFoundException {
        Logic.get().start();
        SimWorker.get().start();

        SwingUtilities.invokeLater(() -> {
            try {
                new MainFrame();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
