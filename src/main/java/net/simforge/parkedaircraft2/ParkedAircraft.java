package net.simforge.parkedaircraft2;

import flightsim.simconnect.config.ConfigurationNotFoundException;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

// todo ak fuel support
// todo ak0 5 columns instead of 4 ?
// todo ak0 'loading' state does not work well
// todo ak0 test how loading sequence works in case of loading on parking
// todo ak0 test how loading sequence works in case of loading in air
// todo ak1 do not save when aircraft is flying - todo ak1 saving condition
// todo ak1 do not save if aircraft has not been landed before
// todo ak1 do not restore if aircraft is loaded flying
// todo ak1 improved look-and-feel
// todo ak2 another icon
// todo ak1 60 seconds timer before autocancellation
// todo ak2 rethink statusToRestoreLabel
// todo ak3 somehow set atc id (and store it?)
// todo ak3 simconnect.text experiments
// todo ak2 hide at auto-start
// todo ak1 limit max distance for simple movement
// todo ak2 non-resizable frame
// todo ak3 cloud storage of aircraft states
// todo ak3 FLT-movement support
// todo ak3 refueling workflow, time simulation, etc
// todo ak3 integrate tracking code into the app, rename it into fs-tracker?
// todo ak3 remove 'test message'
// todo ak3 storing version
// todo ak3 version check and alert
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
