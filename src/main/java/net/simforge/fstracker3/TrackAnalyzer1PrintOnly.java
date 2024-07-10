package net.simforge.fstracker3;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

public class TrackAnalyzer1PrintOnly {
    public static void main(String[] args) throws IOException {
        final List<TrackEntryInfo> trackData = TrackAnalyzer1.loadTrackDataAfter(LocalDateTime.of(2024, Month.JULY, 10, 0, 0));
        System.out.println(trackData.size());

        final List<TrackAnalyzer1.SegmentInfo> segments = TrackAnalyzer1.convertTrackDataToSegments(trackData);

        System.out.println();
        System.out.println();
        System.out.println();
        segments.stream()
                .filter(segmentInfo -> segmentInfo.isTakeoffLanding() && !segmentInfo.isBouncing())
                .forEach(s -> {
                    System.out.println();
                    System.out.println(s);
                    System.out.println();
                    final TrackAnalyzer1.FlightInfo flightInfo = TrackAnalyzer1.recognizeFlight(s, segments);
                    if (flightInfo == null) {
                        return;
                    }
                    TrackAnalyzer1.printFlightInfo(flightInfo);
                });
    }
}
