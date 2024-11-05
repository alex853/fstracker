package net.simforge.fsremotecontrol;

import com.google.gson.Gson;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import net.simforge.fsdatafeeder.FSDataFeeder;
import net.simforge.fsdatafeeder.SimState;
import net.simforge.fsdatafeeder.SimStateConsumer;
import net.simforge.fsdatafeeder.SimStateField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static final SimStateField[] fields = {
            SimStateField.Plane_Altitude,
            SimStateField.Ground_Velocity
    };

    private static final String session = "1";

    public static void main(String[] args) {
        log.info("Starting FSRemoteControl");

        final SimStateConsumer consumer = App::sendSimState;

        while (true) {
            final FSDataFeeder feeder = new FSDataFeeder(consumer, fields);
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

    private static void sendSimState(final SimState simState) {
        final String urlString = "https://f1.simforge.co.uk:7125/service/v1/sim/post?session=" + session;
        final String jsonInputString = new Gson().toJson(simState.getState());

        try {
            final URL url = new URL(urlString);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            try (final OutputStream os = connection.getOutputStream()) {
                final byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            final int responseCode = connection.getResponseCode();
            log.info("Response Code: " + responseCode);

            connection.disconnect();
        } catch (Exception e) {
            log.error("error on sending", e);
        }
    }
}
