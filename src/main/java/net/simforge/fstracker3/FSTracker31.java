package net.simforge.fstracker3;

import flightsim.simconnect.config.ConfigurationNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FSTracker31 {
    private static final Logger log = LoggerFactory.getLogger(FSTracker31.class);

    public static void main(String[] args) {
        log.info("Starting tracker");
        while (true) {
            final FSDataFeeder feeder = new FSDataFeeder();
            try {
                feeder.connectAndRunCollectionCycle();
            } catch (final IOException e) {
                log.warn("Connection unsuccessful: " + e.getMessage());
            } catch (final ConfigurationNotFoundException e) {
                log.error("Configuration not found", e);
                break;
            } catch (final InterruptedException e) {
                break;
            }

            try {
                //noinspection BusyWait
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
