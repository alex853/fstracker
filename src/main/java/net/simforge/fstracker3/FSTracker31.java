package net.simforge.fstracker3;

import flightsim.simconnect.config.ConfigurationNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class FSTracker31 {
    private static final Logger log = LoggerFactory.getLogger(FSTracker31.class);

    public static void main(String[] args) {
        log.info("Starting tracker");

        FSTrackingNotifications.init();

        final TrackWriter trackWriter = new TrackWriter();
        final LinkedList<TrackEntry> trackTail = new LinkedList<>();

        final TrackEntryConsumer consumer = trackEntry -> {
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
            final FSDataFeeder feeder = new FSDataFeeder(consumer);
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
}
