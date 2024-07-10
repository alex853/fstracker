package net.simforge.fstracker3;

import net.simforge.commons.misc.Geo;
import net.simforge.commons.misc.Str;
import net.simforge.refdata.airports.Airport;
import net.simforge.refdata.airports.Airports;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.List;
import java.util.function.Function;

public class TrackAnalyzer2 {
    private static final DecimalFormat d1 = new DecimalFormat("0.0");

    public static void main(String[] args) throws IOException {
        final TrackReader reader = TrackReader.buildForAll();
        final List<TrackEntry> trackData = reader.readTrackData();
        System.out.println(trackData.size());

        TrackEntry prev = null;
        TrackWindow window = new TrackWindow(7);
        for (TrackEntry each : trackData) {
            try {
                if (prev == null) {
                    continue;
                }

                long timeDelta = each.getTimestamp() - prev.getTimestamp();
                boolean timeStepTooBig = timeDelta > 10000;
                if (timeStepTooBig) {
                    print(prev, "Time gap FROM");
                    print(each, "Time gap TO");
                    window.reset();
                    continue;
                }

                double distanceChange = Geo.distance(
                        Geo.coords(each.getLatitude(), each.getLongitude()),
                        Geo.coords(prev.getLatitude(), prev.getLongitude()));
                boolean jump = distanceChange > 4000.0 / 3600.0; // it is based on 1 second step
                if (jump) {
                    print(prev, "Jump FROM");
                    print(each, "Jump TO");
                    window.reset();
                    continue;
                }

                window.add(each);

                final LocalDateTime ts = LocalDateTime.ofEpochSecond(each.getTimestamp(), 0, ZoneOffset.UTC);
                final Airport airport = Airports.get().findWithinBoundary(Geo.coords(each.getLatitude(), each.getLongitude()));
                System.out.println(ts
                        + " " + Str.al(airport != null ? airport.getIcao() : "n/a", 4)
                        + " " + (window.isFull() ? window.getStatus() : "NOT FULL"));
            } finally {
                prev = each;
            }
        }
    }

    private static void print(final TrackEntry info, final String event) {
        final LocalDateTime ts = LocalDateTime.ofEpochSecond(info.getTimestamp(), 0, ZoneOffset.UTC);
        System.out.println(ts + " " + event);
    }

    private static class TrackWindow {
        private final int size;
        private final LinkedList<TrackEntry> data = new LinkedList<>();
        private final Set<Event> lastEvents = new TreeSet<>();
        private final Map<String, Status1> status = new HashMap<>();

        public TrackWindow(final int size) {
            this.size = size;
        }

        public void reset() {
            data.clear();
            lastEvents.clear();
        }

        public void add(final TrackEntry info) {
            data.add(info);
            while (data.size() > size) {
                data.remove(0);
            }
            recalculateLastEvents();
        }

        public boolean isFull() {
            return data.size() == size;
        }

        public Set<Event> getLastEvents() {
            return Collections.unmodifiableSet(lastEvents);
        }

        public Map<String, Status1> getStatus() {
            return Collections.unmodifiableMap(status);
        }

        private void recalculateLastEvents() {
            calculateStatus("Flying", (info) -> info.getOnGround() == 0);
            calculateStatus("Moving", (info) -> info.getGroundspeed() > 0.1);
            calculateStatus("ParkingBrake", (info) -> info.getParkingBrake() == 1);
            //calculateStatus("EngineRunning", (info) -> info.getEngineRunning() == 1);
        }

        private void calculateStatus(final String name,
                                     final Function<TrackEntry, Boolean> currentStatusFn) {
            int trues = 0;
            int falses = 0;
            for (final TrackEntry each : data) {
                boolean eachStatus = currentStatusFn.apply(each);
                if (eachStatus) {
                    trues++;
                } else {
                    falses++;
                }
            }
            int mostlyThreshold = (int) Math.round(size * 0.75);
            if (trues == size) {
                status.put(name, Status1.Yes);
            } else if (trues >= mostlyThreshold) {
                status.put(name, Status1.YesMostly);
            } else if (falses == size) {
                status.put(name, Status1.No);
            } else if (falses >= mostlyThreshold) {
                status.put(name, Status1.NoMostly);
            } else {
                status.put(name, Status1.Changing);
            }
        }
    }

    private enum Status1 {
        Yes,
        YesMostly,
        Changing,
        NoMostly,
        No
    }

    private enum Status {
        InAir, // 00000

        OnGround, // 11111
        Changing, // 1101000
        LandingUncertainty,
        StartMoving,
        StopMoving,

    }

    private enum Event {
        Takeoff, // 1110000
        Landing, // 0000111
        TakeoffUncertainty, // 1101000
        LandingUncertainty,
        StartMoving,
        StopMoving,

    }
}