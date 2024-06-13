package net.simforge.fstracker3;

import net.simforge.commons.misc.Geo;
import net.simforge.commons.misc.JavaTime;
import net.simforge.commons.misc.Str;
import net.simforge.refdata.airports.Airport;
import net.simforge.refdata.airports.Airports;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static net.simforge.fstracker3.TrackEntryInfo.Field.engine_running;

public class TrackAnalyzer1 {
    private static final DecimalFormat d1 = new DecimalFormat("0.0");

    public static void main(String[] args) throws IOException {
        final TrackReader reader = TrackReader.buildForAll();
        final List<TrackEntryInfo> trackData = reader.readTrackData();
        System.out.println(trackData.size());

        TrackEntryInfo prev = null;
        SegmentInfo segment = SegmentInfo.zero();
        for (TrackEntryInfo each : trackData) {
            try {
                if (prev == null) {
                    continue;
                }

                long timeDelta = each.getTimestamp() - prev.getTimestamp();
                boolean timeStepTooBig = timeDelta > 10000;
                if (timeStepTooBig) {
                    print(prev, "Time gap FROM");
                    print(each, "Time gap TO");
                    segment = SegmentInfo.zero();
                    continue;
                }

                if (isOutsideSimulation(each) != isOutsideSimulation(prev)) {
                    if (isOutsideSimulation(each)) {
                        print(each, "Simulation OUT");

                        printSegment(segment);
                        printWithAirport(each, new ArrayList<>());
                        segment = SegmentInfo.zero();

                        continue;
                    } else {
                        print(each, "Simulation IN");

                        segment = SegmentInfo.zero();

                        continue;
                    }
                } else if (isOutsideSimulation(each)) {
                    continue;
                }

                double distanceChange = Geo.distance(
                        Geo.coords(each.getLatitude(), each.getLongitude()),
                        Geo.coords(prev.getLatitude(), prev.getLongitude()));
                boolean jump = distanceChange > 4000.0 / 3600.0; // it is based on 1 second step
                if (jump) {
                    print(prev, "Jump FROM");
                    print(each, "Jump TO");
                    segment = SegmentInfo.zero();
                    continue;
                }

                final List<String> events = new ArrayList<>();

                if (prev.has(engine_running) && each.has(engine_running)) {
                    boolean engineStartup = prev.getEngineRunning() == 0 && each.getEngineRunning() == 1;
                    boolean engineShutdown = prev.getEngineRunning() == 1 && each.getEngineRunning() == 0;
                    if (engineStartup) {
                        events.add("Engine Startup");
                    } else if (engineShutdown) {
                        events.add("Engine Shutdown");
                    }
                }

                boolean parkingBrakeOff = prev.getParkingBrake() == 1 && each.getParkingBrake() == 0;
                boolean parkingBrakeOn = prev.getParkingBrake() == 0 && each.getParkingBrake() == 1;

                if (parkingBrakeOff) {
                    events.add("Parking Brake Off");
                } else if (parkingBrakeOn) {
                    events.add("Parking Brake On");
                }

                boolean startMoving = prev.getOnGround() == 1 && prev.getGroundspeed() < 0.1 && each.getOnGround() == 1 && each.getGroundspeed() >= 0.1;
                boolean stopMoving = prev.getOnGround() == 1 && prev.getGroundspeed() >= 0.1 && each.getOnGround() == 1 && each.getGroundspeed() < 0.1;

                if (startMoving) {
                    events.add("Start Moving");
                } else if (stopMoving) {
                    events.add("Stop Moving");
                }

                boolean takeoff = prev.getOnGround() == 1 && each.getOnGround() == 0;
                boolean landing = prev.getOnGround() == 0 && each.getOnGround() == 1;

                if (takeoff) {
                    events.add("Takeoff");
                } else if (landing) {
                    events.add("Landing");
                }

                if (events.isEmpty()) {
                    segment = segment.plusStep(1000, distanceChange);
                } else {
                    printSegment(segment);
                    printWithAirport(each, events);
                    segment = SegmentInfo.zero();
                }
            } finally {
                prev = each;
            }
        }

        if (!segment.isZero()) {
            printSegment(segment);
        }
    }

    private static boolean isOutsideSimulation(TrackEntryInfo each) {
        return Geo.distance(Geo.coords(each.getLatitude(), each.getLongitude()), Geo.coords(0, 0)) < 1;
    }

    private static void print(final TrackEntryInfo info, final String event) {
        final LocalDateTime ts = LocalDateTime.ofEpochSecond(info.getTimestamp(), 0, ZoneOffset.UTC);
        System.out.println(ts + " " + event);
    }

    private static void printWithAirport(final TrackEntryInfo info, final List<String> events) {
        final LocalDateTime ts = LocalDateTime.ofEpochSecond(info.getTimestamp(), 0, ZoneOffset.UTC);
        final Airport airport = Airports.get().findWithinBoundary(Geo.coords(info.getLatitude(), info.getLongitude()));
        System.out.println(ts
                + " " + Str.al(airport != null ? airport.getIcao() : "n/a", 4)
                + " " + events);
    }

    private static void printSegment(final SegmentInfo segmentInfo) {
        System.out.println("                               Segment data - " + segmentInfo);
    }

    private static class SegmentInfo {
        private final long time;
        private final double distance;

        public static SegmentInfo zero() {
            return new SegmentInfo(0 ,0 );
        }

        private SegmentInfo(final long time, final double distance) {
            this.time = time;
            this.distance = distance;
        }

        @Override
        public String toString() {
            return  "time: " + JavaTime.toHhmm(Duration.ofSeconds(time / 1000)) + " " + d1.format(distance) + " nm";
        }

        public SegmentInfo plusStep(final int timeChange, final double distanceChange) {
            return new SegmentInfo(this.time + timeChange, this.distance + distanceChange);
        }

        public boolean isZero() {
            return time == 0 && distance == 0;
        }
    }
}
