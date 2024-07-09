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
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import static net.simforge.fstracker3.TrackEntryInfo.Field.engine_running;

public class TrackAnalyzer1 {
    private static final DecimalFormat d1 = new DecimalFormat("0.0");

    public static void main(String[] args) throws IOException {
        final TrackReader reader = TrackReader.buildForAll();
        final List<TrackEntryInfo> trackData = reader.readTrackData().stream()
                .filter(t -> t.getDateTime().isAfter(LocalDateTime.of(2024, Month.JUNE, 22, 0, 0)))
                .toList();
        System.out.println(trackData.size());

        TrackEntryInfo prev = null;
        SegmentInfo segment = null;
        final List<SegmentInfo> segments = new ArrayList<>();
        for (TrackEntryInfo each : trackData) {
            try {
                if (prev == null) {
                    segment = SegmentInfo.zero().addStartEvent("Track START").setStartPosition(each);
                    continue;
                }

                long timeDelta = each.getTimestamp() - prev.getTimestamp();
                boolean timeStepTooBig = timeDelta > 10000;
                if (timeStepTooBig) {
                    print(prev, "Time gap FROM");
                    segments.add(segment.addFinishEvent("Time gap FROM").setFinishPosition(prev));

                    print(each, "Time gap TO");
                    segment = SegmentInfo.zero().addStartEvent("Time gap TO").setStartPosition(each);
                    continue;
                }

                if (isOutsideSimulation(each) != isOutsideSimulation(prev)) {
                    if (isOutsideSimulation(each)) {
                        print(each, "Simulation OUT");
                        segments.add(segment.addFinishEvent("Simulation OUT").setFinishPosition(prev));
                        printSegment(segment);

                        printWithAirport(each, Collections.emptySet());

                        segment = SegmentInfo.zero().addStartEvent("Simulation OUT").setStartPosition(each);

                        continue;
                    } else {
                        print(each, "Simulation IN");
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
                    print(prev, "Jump FROM");
                    segments.add(segment.addFinishEvent("Jump FROM").setFinishPosition(prev));

                    print(each, "Jump TO");
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
                    printSegment(segment);
                    segments.add(segment.addFinishEvents(events).setFinishPosition(each));

                    printWithAirport(each, events);
                    segment = SegmentInfo.zero().addStartEvents(events).setStartPosition(each);
                }
            } finally {
                prev = each;
            }
        }

        if (!segment.isZero()) {
            printSegment(segment);
            segments.add(segment.addFinishEvent("Track END"));
        }

        System.out.println();
        System.out.println();
        System.out.println();
        segments.stream()
                .filter(SegmentInfo::isTakeoffLanding)
                .forEach(s -> {
                    System.out.println();
                    System.out.println(s);
                    System.out.println();
                    recognizeFlight(s, segments);
                });
    }

    private static void recognizeFlight(final SegmentInfo flyingSegment, final List<SegmentInfo> segments) {
        final List<SegmentInfo> before = segments.subList(0, segments.indexOf(flyingSegment));
        final List<SegmentInfo> after = segments.subList(segments.indexOf(flyingSegment)+1, segments.size());

        if (flyingSegment.getDuration().compareTo(Duration.ofSeconds(30)) < 0) {
            System.out.println("SHOUT!!! FLYING SEGMENT TOO SHORT, BOUNCING?");
            return;
        }



        final List<SegmentInfo> beforeTrimmed = trimObviousCasesInBefore(before);
        final Optional<SegmentInfo> startsWithEngineStart = findInReversalOrder(beforeTrimmed, s -> s.hasStartEvent("Engine Startup"));
        if (startsWithEngineStart.isEmpty()) {
            System.out.println("SHOUT!!! Engine Startup not found!!!!");
            return;
        }
        final List<SegmentInfo> engineStartupToTakeoff = beforeTrimmed.subList(beforeTrimmed.indexOf(startsWithEngineStart.get()), beforeTrimmed.size());
        final Optional<SegmentInfo> firstStartMoving = engineStartupToTakeoff.stream().filter(s -> s.hasStartEvent("Start Moving")).findFirst();
        if (firstStartMoving.isEmpty()) {
            System.out.println("SHOUT!!! Start Moving not found!!!!");
            return;
        }

        final SegmentInfo beginningOfFlight = firstStartMoving.get();
        final List<SegmentInfo> departureSegments = engineStartupToTakeoff.subList(engineStartupToTakeoff.indexOf(beginningOfFlight), engineStartupToTakeoff.size());



        final List<SegmentInfo> afterTrimmed = trimObviousCasesInAfter(after);
        final Optional<SegmentInfo> endsWithEngineShutdown = afterTrimmed.stream().filter(s -> s.hasFinishEvent("Engine Shutdown")).findFirst();
        if (endsWithEngineShutdown.isEmpty()) {
            System.out.println("SHOUT!!! Engine Shutdown not found!!!!");
            return;
        }
        final List<SegmentInfo> landingToEngineShutdown = afterTrimmed.subList(0, afterTrimmed.indexOf(endsWithEngineShutdown.get()) + 1);
        final Optional<SegmentInfo> lastStopMoving = findInReversalOrder(landingToEngineShutdown, s -> s.hasFinishEvent("Stop Moving"));
        if (lastStopMoving.isEmpty()) {
            System.out.println("SHOUT!!! Stop Moving not found!!!!");
            return;
        }

        final SegmentInfo endOfFlight = lastStopMoving.get();
        final List<SegmentInfo> arrivalSegments = landingToEngineShutdown.subList(0, landingToEngineShutdown.indexOf(endOfFlight) + 1);



        final double totalDistance = sumDistance(departureSegments)
                + flyingSegment.getDistance()
                + sumDistance(arrivalSegments);



        System.out.println("=========== FLIGHT INFO =================================");
        System.out.println("DOF         " + JavaTime.yMd.format(beginningOfFlight.getStartPosition().getDateTime()));
        System.out.println("DEPARTURE   "
                + Str.al(findAirportIcao(flyingSegment.getStartPosition()), 4)
                + "    TIME OUT " + JavaTime.toHhmm(beginningOfFlight.getStartPosition().getDateTime().toLocalTime())
                + "    TIME OFF " + JavaTime.toHhmm(flyingSegment.getStartPosition().getDateTime().toLocalTime()));
        System.out.println("DESTINATION "
                + Str.al(findAirportIcao(flyingSegment.getFinishPosition()), 4)
                + "    TIME ON  " + JavaTime.toHhmm(flyingSegment.getFinishPosition().getDateTime().toLocalTime())
                + "    TIME IN  " + JavaTime.toHhmm(endOfFlight.getFinishPosition().getDateTime().toLocalTime()));
        System.out.println("DISTANCE            " + d1.format(totalDistance) + " nm");
        System.out.println("FLIGHT TIME         " + JavaTime.toHhmm(Duration.ofSeconds(endOfFlight.getFinishPosition().getTimestamp() - beginningOfFlight.getStartPosition().getTimestamp())));
    }

    private static double sumDistance(final List<SegmentInfo> segments) {
        return segments.stream().map(SegmentInfo::getDistance).reduce(Double::sum).orElse(0.0);
    }

    private static List<SegmentInfo> trimObviousCasesInBefore(final List<SegmentInfo> before) {
        final Optional<SegmentInfo> trimBy = findInReversalOrder(before,
                s -> s.hasAnyStartEvent("Simulation IN", "Time gap TO", "Jump TO"));
        if (trimBy.isEmpty()) {
            return before;
        }
        return before.subList(before.indexOf(trimBy.get()), before.size());
    }

    private static List<SegmentInfo> trimObviousCasesInAfter(final List<SegmentInfo> after) {
        final Optional<SegmentInfo> trimBy = after.stream()
                .filter(s -> s.hasAnyFinishEvent("Simulation OUT", "Time gap FROM", "Jump FROM"))
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

    private static boolean isOutsideSimulation(TrackEntryInfo each) {
        return Geo.distance(Geo.coords(each.getLatitude(), each.getLongitude()), Geo.coords(0, 0)) < 1;
    }

    private static void print(final TrackEntryInfo info, final String event) {
        final LocalDateTime ts = LocalDateTime.ofEpochSecond(info.getTimestamp(), 0, ZoneOffset.UTC);
        System.out.println(ts + " " + event);
    }

    private static void printWithAirport(final TrackEntryInfo info, final Set<String> events) {
        final LocalDateTime ts = LocalDateTime.ofEpochSecond(info.getTimestamp(), 0, ZoneOffset.UTC);
        System.out.println(ts
                + " / " + Str.al(findAirportIcao(info), 4)
                + " / " + info.getTitle()
                + " / " + events);
    }

    private static String findAirportIcao(TrackEntryInfo info) {
        final Airport airport = Airports.get().findWithinBoundary(Geo.coords(info.getLatitude(), info.getLongitude()));
        return airport != null ? airport.getIcao() : "n/a";
    }

    private static void printSegment(final SegmentInfo segmentInfo) {
        System.out.println("                               Segment data - " + segmentInfo);
    }

    private static class SegmentInfo {
        private final long time;
        private final double distance;
        private final Set<String> startEvents = new TreeSet<>();
        private final Set<String> finishEvents = new TreeSet<>();
        private final TrackEntryInfo startPosition;
        private final TrackEntryInfo finishPosition;

        public static SegmentInfo zero() {
            return new SegmentInfo(0 ,0, Collections.emptySet(), Collections.emptySet(), null, null);
        }

        private SegmentInfo(final long time,
                            final double distance,
                            final Set<String> startEvents,
                            final Set<String> finishEvents,
                            final TrackEntryInfo startPosition,
                            final TrackEntryInfo finishPosition) {
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

        public boolean isTakeoffLanding() {
            return hasStartEvent("Takeoff") && hasFinishEvent("Landing");
        }

        public Duration getDuration() {
            return Duration.ofMillis(time);
        }

        public SegmentInfo setStartPosition(final TrackEntryInfo start) {
            return new SegmentInfo(this.time, this.distance, startEvents, finishEvents, start, finishPosition);
        }

        public SegmentInfo setFinishPosition(final TrackEntryInfo finish) {
            return new SegmentInfo(this.time, this.distance, startEvents, finishEvents, startPosition, finish);
        }

        public TrackEntryInfo getStartPosition() {
            return startPosition;
        }

        public TrackEntryInfo getFinishPosition() {
            return finishPosition;
        }

        public double getDistance() {
            return distance;
        }
    }
}
