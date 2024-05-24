package net.simforge.fstracker3;

import flightsim.simconnect.config.ConfigurationNotFoundException;

import java.io.IOException;

public class FSTracker31 {
    public static void main(String[] args) {
        while (true) {
            final FSDataFeeder feeder = new FSDataFeeder();
            try {
                feeder.connectAndRunCollectionCycle();
            } catch (final IOException e) {
                System.err.println("Connection unsuccessful: " + e.getMessage());
            } catch (final ConfigurationNotFoundException e) {
                System.err.println("Configuration not found: " + e.getMessage());
                break;
            } catch (final InterruptedException e) {
                break;
            }

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
