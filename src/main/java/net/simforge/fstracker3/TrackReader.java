package net.simforge.fstracker3;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.simforge.commons.io.IOHelper;
import net.simforge.commons.misc.JavaTime;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TrackReader {
    public static TrackReader buildForAll() {
        return new TrackReader(null, null);
    }

    public static TrackReader buildForLast7Days() {
        final LocalDateTime now = JavaTime.nowUtc();
        return new TrackReader(now.minusDays(7), now);
    }

    private final LocalDateTime fromDate;
    private final LocalDateTime toDate;

    private TrackReader(LocalDateTime fromDate, LocalDateTime toDate) {
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public List<TrackEntry> readTrackData() throws IOException {
        final List<TrackStorage.FileInfo> files = TrackStorage.listFiles();

        final TrackStorage.FileInfo fromFile = files.get(0);
        final TrackStorage.FileInfo toFile = files.get(files.size() - 1);

        final List<TrackEntry> result = new ArrayList<>();

        for (int i = files.indexOf(fromFile); i <= files.indexOf(toFile); i++) {
            final TrackStorage.FileInfo trackFileInfo = files.get(i);
            result.addAll(readTrackData(trackFileInfo));
        }

        return result;
    }

    private List<TrackEntry> readTrackData(final TrackStorage.FileInfo trackFileInfo) throws IOException {
        final File file = trackFileInfo.getFile();
        final String content = IOHelper.loadFile(file);
        final Gson gson = new Gson();
        final JsonObject jsonObject = gson.fromJson(content, JsonObject.class);

        final int version = jsonObject.getAsJsonPrimitive("version").getAsInt();
        if (version != 1) {
            throw new IllegalArgumentException();
        }

        final JsonObject trackObject = jsonObject.getAsJsonObject("track");
        final JsonArray trackDataArray = trackObject.getAsJsonArray("data");

        final List<TrackEntry> result = new ArrayList<>();

        for (int i = 0; i < trackDataArray.size(); i++) {
            final String row = trackDataArray.get(i).getAsString();
            final String[] values = row.split(";");

            final TrackEntry entry = new TrackEntry();
            entry.put(TrackEntry.Field.timestamp, Long.parseLong(values[0]));
            entry.put(TrackEntry.Field.title, values[1]);
            entry.put(TrackEntry.Field.latitude, Double.parseDouble(values[2]));
            entry.put(TrackEntry.Field.longitude, Double.parseDouble(values[3]));
            entry.put(TrackEntry.Field.altitude, Double.parseDouble(values[4]));
            entry.put(TrackEntry.Field.on_ground, Integer.parseInt(values[5]));
            entry.put(TrackEntry.Field.groundspeed, Double.parseDouble(values[6]));
            entry.put(TrackEntry.Field.parking_brake, Integer.parseInt(values[7]));
            if (values.length > 8) {
                entry.put(TrackEntry.Field.engine_running, Integer.parseInt(values[8]));
            }

            result.add(entry);
        }

        return result;
    }
}
