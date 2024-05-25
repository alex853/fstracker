package net.simforge.fstracker3;

import java.io.IOException;
import java.util.List;

public class TrackAnalyzer1 {
    public static void main(String[] args) throws IOException {
        final TrackReader reader = TrackReader.buildForAll();
        final List<TrackEntryInfo> trackData = reader.readTrackData();
        System.out.println(trackData.size());
    }
}
