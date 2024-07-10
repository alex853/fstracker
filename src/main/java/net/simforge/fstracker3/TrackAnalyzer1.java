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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.simforge.fstracker3.TrackEntry.Field.engine_running;

public class TrackAnalyzer1 {
    private static final DecimalFormat d1 = new DecimalFormat("0.0");

    public static List<TrackEntry> loadTrackDataAll() throws IOException {
        final TrackReader reader = TrackReader.buildForAll();
        return reader.readTrackData();
    }

    public static List<TrackEntry> loadTrackDataAfter(final LocalDateTime threshold) throws IOException {
        final TrackReader reader = TrackReader.buildForAll();
        return reader.readTrackData().stream()
                .filter(t -> t.getDateTime().isAfter(threshold))
                .collect(Collectors.toList());
    }

    public static List<SegmentInfo> convertTrackDataToSegments(final List<TrackEntry> trackData) {
        return convertTrackDataToSegments(trackData, true);
    }

    public static List<SegmentInfo> convertTrackDataToSegments(final List<TrackEntry> trackData, final boolean verbose) {
        TrackEntry prev = null;
        SegmentInfo segment = null;
        final List<SegmentInfo> segments = new ArrayList<>();
        for (TrackEntry each : trackData) {
            try {
                if (prev == null) {
                    segment = SegmentInfo.zero().addStartEvent("Track START").setStartPosition(each);
                    continue;
                }

                long timeDelta = each.getTimestamp() - prev.getTimestamp();
                boolean timeStepTooBig = timeDelta > 10000;
                if (timeStepTooBig) {
                    if (verbose) print(prev, "Time gap FROM");
                    segments.add(segment.addFinishEvent("Time gap FROM").setFinishPosition(prev));

                    if (verbose) print(each, "Time gap TO");
                    segment = SegmentInfo.zero().addStartEvent("Time gap TO").setStartPosition(each);
                    continue;
                }

                if (isOutsideSimulation(each) != isOutsideSimulation(prev)) {
                    if (isOutsideSimulation(each)) {
                        if (verbose) print(each, "Simulation OUT");
                        segments.add(segment.addFinishEvent("Simulation OUT").setFinishPosition(prev));
                        if (verbose) printSegment(segment);

                        if (verbose) printWithAirport(each, Collections.emptySet());

                        segment = SegmentInfo.zero().addStartEvent("Simulation OUT").setStartPosition(each);

                        continue;
                    } else {
                        if (verbose) print(each, "Simulation IN");
                        segments.add(segment.addFinishEvent("Simulation IN").setFinishPosition(prev));

                        segment = SegmentInfo.zero().addStartEvent("Simulation IN").setStartPosition(each);

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
                    if (verbose) print(prev, "Jump FROM");
                    segments.add(segment.addFinishEvent("Jump FROM").setFinishPosition(prev));

                    if (verbose) print(each, "Jump TO");
                    segment = SegmentInfo.zero().addStartEvent("Jump TO").setStartPosition(each);

                    continue;
                }

                final Set<String> events = new TreeSet<>();

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
                    if (verbose) printSegment(segment);
                    segments.add(segment.addFinishEvents(events).setFinishPosition(each));

                    if (verbose) printWithAirport(each, events);
                    segment = SegmentInfo.zero().addStartEvents(events).setStartPosition(each);
                }
            } finally {
                prev = each;
            }
        }

        if (!segment.isZero()) {
            if (verbose) printSegment(segment);
            segments.add(segment.addFinishEvent("Track END"));
        }
        return segments;
    }

    public static FlightInfo recognizeFlight(final SegmentInfo flyingSegment, final List<SegmentInfo> segments) {
        // todo ak1 implement max minutes
        final List<SegmentInfo> before = trimBefore(segments.subList(0, segments.indexOf(flyingSegment)), 30);
        final List<SegmentInfo> after = trimAfter(segments.subList(segments.indexOf(flyingSegment) + 1, segments.size()), 30);

        SegmentInfo beginningOfFlight;

        final Optional<SegmentInfo> startsWithEngineStart = findInReversalOrder(before, s -> s.hasStartEvent("Engine Startup"));
        if (startsWithEngineStart.isPresent()) {
            final List<SegmentInfo> engineStartupToTakeoff = before.subList(before.indexOf(startsWithEngineStart.get()), before.size());
            final Optional<SegmentInfo> firstStartMoving = engineStartupToTakeoff.stream().filter(s -> s.hasStartEvent("Start Moving")).findFirst();
            if (firstStartMoving.isPresent()) {
                beginningOfFlight = firstStartMoving.get();
            } else {
                beginningOfFlight = startsWithEngineStart.get();
            }
        } else {
            final Optional<SegmentInfo> startsWithParkingBrakeOff = findInReversalOrder(before, s -> s.hasStartEvent("Parking Brake Off"));
            if (startsWithParkingBrakeOff.isPresent()) {
                beginningOfFlight = startsWithParkingBrakeOff.get();
            } else {
                final Optional<SegmentInfo> startsWithStartMoving = findInReversalOrder(before, s -> s.hasStartEvent("Start Moving"));
                if (startsWithStartMoving.isPresent()) {
                    beginningOfFlight = startsWithStartMoving.get();
                } else {
                    System.out.println("UNABLE TO FIND BEGINNING OF FLIGHT!!!!!");
                    return null;
                }
            }
        }

        SegmentInfo endOfFlight;

        final Optional<SegmentInfo> endsWithEngineShutdown = after.stream().filter(s -> s.hasFinishEvent("Engine Shutdown")).findFirst();
        if (endsWithEngineShutdown.isPresent()) {
            final List<SegmentInfo> landingToEngineShutdown = after.subList(0, after.indexOf(endsWithEngineShutdown.get()) + 1);
            final Optional<SegmentInfo> lastStopMoving = findInReversalOrder(landingToEngineShutdown, s -> s.hasFinishEvent("Stop Moving"));
            if (lastStopMoving.isPresent()) {
                endOfFlight = lastStopMoving.get();
            } else {
                endOfFlight = endsWithEngineShutdown.get();
            }
        } else {
            final Optional<SegmentInfo> endsWithParkingBrakeOn = after.stream().filter(s -> s.hasFinishEvent("Parking Brake On")).findFirst();
            if (endsWithParkingBrakeOn.isPresent()) {
                endOfFlight = endsWithParkingBrakeOn.get();
            } else {
                final Optional<SegmentInfo> endsWithStopMoving = after.stream().filter(s -> s.hasFinishEvent("Stop Moving")).findFirst();
                if (endsWithStopMoving.isPresent()) {
                    endOfFlight = endsWithStopMoving.get();
                } else {
                    System.out.println("UNABLE TO FIND END OF FLIGHT!!!!!");
                    return null;
                }
            }
        }

        final List<SegmentInfo> departureSegments = before.subList(before.indexOf(beginningOfFlight), before.size());
        final List<SegmentInfo> arrivalSegments = after.subList(0, after.indexOf(endOfFlight) + 1);

        final double totalDistance = sumDistance(departureSegments)
                + flyingSegment.getDistance()
                + sumDistance(arrivalSegments);

        return new FlightInfo(beginningOfFlight, flyingSegment, endOfFlight, totalDistance);
    }

    public static void printFlightInfo(final FlightInfo flight) {
        System.out.println("=========== FLIGHT INFO =================================");
        System.out.println("DOF         " + JavaTime.yMd.format(flight.getTimeOut()));
        System.out.println("DEPARTURE   "
                + Str.al(icaoOrNA(flight.getDepartureIcao()), 4)
                + "    TIME OUT " + JavaTime.toHhmm(flight.getTimeOut().toLocalTime())
                + "    TIME OFF " + JavaTime.toHhmm(flight.getTimeOff().toLocalTime()));
        System.out.println("DESTINATION "
                + Str.al(icaoOrNA(flight.getDestinationIcao()), 4)
                + "    TIME ON  " + JavaTime.toHhmm(flight.getTimeOn().toLocalTime())
                + "    TIME IN  " + JavaTime.toHhmm(flight.getTimeIn().toLocalTime()));
        System.out.println("DISTANCE            " + d1.format(flight.getTotalDistance()) + " nm");
        System.out.println("FLIGHT TIME         " + JavaTime.toHhmm(flight.getTotalTime()));
    }

    private static String icaoOrNA(final String icao) {
        return icao != null ? icao : "n/a";
    }

    private static double sumDistance(final List<SegmentInfo> segments) {
        return segments.stream().map(SegmentInfo::getDistance).reduce(Double::sum).orElse(0.0);
    }

    private static List<SegmentInfo> trimBefore(final List<SegmentInfo> before, final int maxMinutes) {
        final Optional<SegmentInfo> trimBy = findInReversalOrder(before,
                s -> s.hasAnyStartEvent("Simulation IN", "Time gap TO", "Jump TO")
                    || (s.isTakeoffLanding() && !s.isBouncing()));
        if (trimBy.isEmpty()) {
            return before;
        }
        return before.subList(before.indexOf(trimBy.get()), before.size());
    }

    private static List<SegmentInfo> trimAfter(final List<SegmentInfo> after, final int maxMinutes) {
        final Optional<SegmentInfo> trimBy = after.stream()
                .filter(s -> s.hasAnyFinishEvent("Simulation OUT", "Time gap FROM", "Jump FROM")
                        || (s.isTakeoffLanding() && !s.isBouncing()))
                .findFirst();
        if (trimBy.isEmpty()) {
            return after;
        }
        return after.subList(0, after.indexOf(trimBy.get()));
    }

    private static List<SegmentInfo> reverse(final List<SegmentInfo> segments) {
        final List<SegmentInfo> reversed = new ArrayList<>(segments);
        Collections.reverse(reversed);
        return reversed;
    }

    private static Optional<SegmentInfo> findInReversalOrder(final List<SegmentInfo> segments, final Predicate<SegmentInfo> predicate) {
        final List<SegmentInfo> reversed = reverse(segments);
        return reversed.stream().filter(predicate).findFirst();
    }

    private static boolean isOutsideSimulation(TrackEntry each) {
        return Geo.distance(Geo.coords(each.getLatitude(), each.getLongitude()), Geo.coords(0, 0)) < 1;
    }

    private static void print(final TrackEntry info, final String event) {
        final LocalDateTime ts = LocalDateTime.ofEpochSecond(info.getTimestamp(), 0, ZoneOffset.UTC);
        System.out.println(ts + " " + event);
    }

    private static void printWithAirport(final TrackEntry info, final Set<String> events) {
        final LocalDateTime ts = LocalDateTime.ofEpochSecond(info.getTimestamp(), 0, ZoneOffset.UTC);
        System.out.println(ts
                + " / " + Str.al(findAirportIcao(info), 4)
                + " / " + info.getTitle()
                + " / " + events);
    }

    private static String findAirportIcao(TrackEntry info) {
        final Airport airport = Airports.get().findWithinBoundary(Geo.coords(info.getLatitude(), info.getLongitude()));
        return airport != null ? airport.getIcao() : "n/a";
    }

    private static void printSegment(final SegmentInfo segmentInfo) {
        System.out.println("                               Segment data - " + segmentInfo);
    }

    public static class FlightInfo {
        private final SegmentInfo beginningOfFlight;
        private final SegmentInfo flyingSegment;
        private final SegmentInfo endOfFlight;
        private final double totalDistance;

        public FlightInfo(final SegmentInfo beginningOfFlight,
                          final SegmentInfo flyingSegment,
                          final SegmentInfo endOfFlight,
                          final double totalDistance) {
            this.beginningOfFlight = beginningOfFlight;
            this.flyingSegment = flyingSegment;
            this.endOfFlight = endOfFlight;
            this.totalDistance = totalDistance;
        }

        public LocalDateTime getTimeOut() {
            return beginningOfFlight.getStartPosition().getDateTime();
        }

        public LocalDateTime getTimeOff() {
            return flyingSegment.getStartPosition().getDateTime();
        }

        public LocalDateTime getTimeOn() {
            return flyingSegment.getFinishPosition().getDateTime();
        }

        public LocalDateTime getTimeIn() {
            return endOfFlight.getFinishPosition().getDateTime();
        }

        public double getTotalDistance() {
            return totalDistance;
        }

        public Duration getTotalTime() {
            return Duration.ofSeconds(endOfFlight.getFinishPosition().getTimestamp() - beginningOfFlight.getStartPosition().getTimestamp());
        }

        public Duration getAirTime() {
            return Duration.ofSeconds(flyingSegment.getFinishPosition().getTimestamp() - flyingSegment.getStartPosition().getTimestamp());
        }

        public String getDepartureIcao() {
            final Airport airport = Airports.get().findWithinBoundary(Geo.coords(flyingSegment.getStartPosition().getLatitude(), flyingSegment.getStartPosition().getLongitude()));
            return airport != null ? airport.getIcao() : null;
        }

        public String getDestinationIcao() {
            final Airport airport = Airports.get().findWithinBoundary(Geo.coords(flyingSegment.getFinishPosition().getLatitude(), flyingSegment.getFinishPosition().getLongitude()));
            return airport != null ? airport.getIcao() : null;
        }
    }

    public static class SegmentInfo {
        private final long time;
        private final double distance;
        private final Set<String> startEvents = new TreeSet<>();
        private final Set<String> finishEvents = new TreeSet<>();
        private final TrackEntry startPosition;
        private final TrackEntry finishPosition;

        public static SegmentInfo zero() {
            return new SegmentInfo(0 ,0, Collections.emptySet(), Collections.emptySet(), null, null);
        }

        private SegmentInfo(final long time,
                            final double distance,
                            final Set<String> startEvents,
                            final Set<String> finishEvents,
                            final TrackEntry startPosition,
                            final TrackEntry finishPosition) {
            this.time = time;
            this.distance = distance;
            this.startEvents.addAll(startEvents);
            this.finishEvents.addAll(finishEvents);
            this.startPosition = startPosition;
            this.finishPosition = finishPosition;
        }

        @Override
        public String toString() {
            return  "time: " + JavaTime.toHhmm(Duration.ofSeconds(time / 1000)) + "  |  dist: " + d1.format(distance) + " nm  |  start: " + startEvents + "  |  finish: " + finishEvents;
        }

        public SegmentInfo plusStep(final int timeChange, final double distanceChange) {
            return new SegmentInfo(this.time + timeChange, this.distance + distanceChange, startEvents, finishEvents, startPosition, finishPosition);
        }

        public boolean isZero() {
            return time == 0 && distance == 0;
        }

        public SegmentInfo addStartEvent(final String event) {
            return addStartEvents(Collections.singleton(event));
        }

        public SegmentInfo addStartEvents(final Set<String> events) {
            final Set<String> newStartEvents = new TreeSet<>(startEvents);
            newStartEvents.addAll(events);
            return new SegmentInfo(this.time, this.distance, newStartEvents, finishEvents, startPosition, finishPosition);
        }

        public SegmentInfo addFinishEvent(final String event) {
            return addFinishEvents(Collections.singleton(event));
        }

        public SegmentInfo addFinishEvents(final Set<String> events) {
            final Set<String> newFinishEvents = new TreeSet<>(finishEvents);
            newFinishEvents.addAll(events);
            return new SegmentInfo(this.time, this.distance, startEvents, newFinishEvents, startPosition, finishPosition);
        }

        public boolean hasStartEvent(final String event) {
            return startEvents.contains(event);
        }

        public boolean hasAnyStartEvent(final String... events) {
            for (final String event : events) {
                if (startEvents.contains(event)) {
                    return true;
                }
            }
            return false;
        }

        public Set<String> getStartEvents() {
            return Collections.unmodifiableSet(startEvents);
        }

        public boolean hasFinishEvent(final String event) {
            return finishEvents.contains(event);
        }

        public boolean hasAnyFinishEvent(final String... events) {
            for (final String event : events) {
                if (finishEvents.contains(event)) {
                    return true;
                }
            }
            return false;
        }

        public Set<String> getFinishEvents() {
            return Collections.unmodifiableSet(finishEvents);
        }

        public boolean isTakeoffLanding() {
            return hasStartEvent("Takeoff") && hasFinishEvent("Landing");
        }

        public boolean isBouncing() {
            return isTakeoffLanding() && getDuration().compareTo(Duration.ofSeconds(30)) < 0;
        }

        public Duration getDuration() {
            return Duration.ofMillis(time);
        }

        public SegmentInfo setStartPosition(final TrackEntry start) {
            return new SegmentInfo(this.time, this.distance, startEvents, finishEvents, start, finishPosition);
        }

        public SegmentInfo setFinishPosition(final TrackEntry finish) {
            return new SegmentInfo(this.time, this.distance, startEvents, finishEvents, startPosition, finish);
        }

        public TrackEntry getStartPosition() {
            return startPosition;
        }

        public TrackEntry getFinishPosition() {
            return finishPosition;
        }

        public double getDistance() {
            return distance;
        }
    }
}
