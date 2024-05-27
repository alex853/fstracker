package net.simforge.fstracker3;

import net.simforge.commons.misc.Geo;
import net.simforge.commons.misc.Str;
import net.simforge.refdata.airports.Airport;
import net.simforge.refdata.airports.Airports;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class TrackAnalyzer1 {
    public static void main(String[] args) throws IOException {
        final TrackReader reader = TrackReader.buildForAll();
        final List<TrackEntryInfo> trackData = reader.readTrackData();
        System.out.println(trackData.size());

        TrackEntryInfo prev = null;
        for (TrackEntryInfo each : trackData) {
            if (prev == null) {
                prev = each;
                continue;
            }

            // todo boolean timeStepTooBig = todo;
            // boolean jumped = todo;
            // boolean outsideSimulation = todo;
            boolean takeoff = prev.getOnGround() == 1 && each.getOnGround() == 0;
            boolean landing = prev.getOnGround() == 0 && each.getOnGround() == 1;

            if (takeoff) {
                printWithAirport(each, "Takeoff");
            } else if (landing) {
                printWithAirport(each, "Landing");
            }

            prev = each;
        }
    }

    private static void print(final TrackEntryInfo info, final String event) {
        final LocalDateTime ts = LocalDateTime.ofEpochSecond(info.getTimestamp(), 0, ZoneOffset.UTC);
        System.out.println(ts + " " + event);
    }

    private static void printWithAirport(final TrackEntryInfo info, final String event) {
        final LocalDateTime ts = LocalDateTime.ofEpochSecond(info.getTimestamp(), 0, ZoneOffset.UTC);
        final Airport airport = Airports.get().findWithinBoundary(Geo.coords(info.getLatitude(), info.getLongitude()));
        System.out.println(ts + " " + Str.al(airport != null ? airport.getIcao() : "n/a", 4) + " " + event);
    }
}
