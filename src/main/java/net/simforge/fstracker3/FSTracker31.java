package net.simforge.fstracker3;

import flightsim.simconnect.config.ConfigurationNotFoundException;
import net.simforge.commons.misc.JavaTime;
import net.simforge.fsdatafeeder.FSDataFeeder;
import net.simforge.fsdatafeeder.SimState;
import net.simforge.fsdatafeeder.SimStateConsumer;
import net.simforge.fsdatafeeder.SimStateField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;

import static net.simforge.fstracker3.TrackEntry.Field.*;

public class FSTracker31 {
    private static final Logger log = LoggerFactory.getLogger(FSTracker31.class);
    private static final SimStateField[] fields = {
            SimStateField.Title,
            SimStateField.ATC_Type,
            SimStateField.ATC_Model,
            SimStateField.Plane_Latitude,
            SimStateField.Plane_Longitude,
            SimStateField.Plane_Altitude,
            SimStateField.Sim_On_Ground,
            SimStateField.Ground_Velocity,
            SimStateField.Is_User_Sim,
            SimStateField.Plane_In_Parking_State,
            SimStateField.Brake_Parking_Position,
            SimStateField.Eng_Combustion_1
    };

    public static void main(String[] args) {
        log.info("Starting tracker");

        FSTrackingNotifications.init();

        final TrackWriter trackWriter = new TrackWriter();
        final LinkedList<TrackEntry> trackTail = new LinkedList<>();

        final SimStateConsumer consumer = simState -> {
            final TrackEntry trackEntry = toTrackEntry(simState);

            try {
                trackWriter.append(trackEntry);
            } catch (IOException ex) {
                log.error("Unable to write track data", ex);
            }

            trackTail.add(trackEntry);
            while (trackTail.size() > 100) {
                trackTail.remove();
            }

            final List<TrackAnalyzer1.SegmentInfo> segments = TrackAnalyzer1.convertTrackDataToSegments(trackTail, false);
            if (segments.isEmpty()) {
                return;
            }

            TrackAnalyzer1.SegmentInfo lastSegment = segments.get(segments.size() - 1);
            System.out.println(lastSegment);

//            if (lastSegment.getDuration().getSeconds() >= 5 || !lastSegment.hasFinishEvent("Track END")) {
//                return;
//            }

            FSTrackingNotifications.showNotification(lastSegment.getFinishEvents());
        };

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

        try {
            trackWriter.close();
        } catch (IOException e) {
            log.error("Unable to close track data stream");
        }
        FSTrackingNotifications.dispose();
    }


    private static TrackEntry toTrackEntry(final SimState simState) {
        final TrackEntry trackInfo = new TrackEntry();

        trackInfo.put(timestamp, JavaTime.nowUtc().toEpochSecond(ZoneOffset.UTC));
        trackInfo.put(title, simState.getString(SimStateField.Title));
        trackInfo.put(latitude, simState.getDouble(SimStateField.Plane_Latitude));
        trackInfo.put(longitude, simState.getDouble(SimStateField.Plane_Longitude));
        trackInfo.put(altitude, simState.getDouble(SimStateField.Plane_Altitude));
        trackInfo.put(on_ground, simState.getInt(SimStateField.Sim_On_Ground));
        trackInfo.put(groundspeed, simState.getDouble(SimStateField.Ground_Velocity));
        trackInfo.put(parking_brake, simState.getInt(SimStateField.Brake_Parking_Position));
        trackInfo.put(engine_running, simState.getInt(SimStateField.Eng_Combustion_1));

        return trackInfo;
    }
}
