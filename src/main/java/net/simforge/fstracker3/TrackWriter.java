package net.simforge.fstracker3;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.simforge.commons.io.IOHelper;
import net.simforge.commons.misc.JavaTime;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedList;

import static net.simforge.fstracker3.TrackEntryInfo.Field.*;

public class TrackWriter {
    private String partitionFileName;
    private final LinkedList<TrackEntryInfo> partitionData = new LinkedList<>();

    public void append(final SimState simState) throws IOException {
        if (isPartitionFull() || isPartitionNotInitialized()) {
            startNewPartition();
        }

        partitionData.add(toTrackEntryInfo(simState));
        writePartitionData();
    }

    private TrackEntryInfo toTrackEntryInfo(final SimState simState) {
        final TrackEntryInfo trackInfo = new TrackEntryInfo();

        trackInfo.put(timestamp, JavaTime.nowUtc().toEpochSecond(ZoneOffset.UTC));
        trackInfo.put(title, simState.getTitle());
        trackInfo.put(latitude, simState.getLatitude());
        trackInfo.put(longitude, simState.getLongitude());
        trackInfo.put(altitude, simState.getAltitude());
        trackInfo.put(on_ground, simState.getOnGround());
        trackInfo.put(groundspeed, simState.getGroundVelocity());
        trackInfo.put(parking_brake, simState.getBrakeParkingPosition());

        return trackInfo;
    }

    private void writePartitionData() throws IOException {
        final Gson gson = new Gson();

        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("version", "1");

        final JsonObject rangeObject = new JsonObject();
        rangeObject.addProperty("from", partitionData.getFirst().getTimestamp());
        rangeObject.addProperty("to", partitionData.getLast().getTimestamp());
        jsonObject.add("range", rangeObject);

        final JsonArray trackDataArray = new JsonArray();
        partitionData.forEach(entry -> trackDataArray.add(
                entry.getTimestamp() + ";" +
                entry.getTitle() + ";" +
                entry.getLatitude() + ";" +
                entry.getLongitude() + ";" +
                entry.getAltitude() + ";" +
                entry.getOnGround() + ";" +
                entry.getGroundspeed() + ";" +
                entry.getParkingBrake()));

        final JsonObject trackObject = new JsonObject();
        trackObject.add("data", trackDataArray);
        jsonObject.add("track", trackObject);

        final String json = gson.toJson(jsonObject);

        final File file = new File(partitionFileName);
        file.getParentFile().mkdirs();
        IOHelper.saveFile(file, json);
    }

    private void startNewPartition() {
        final LocalDateTime now = JavaTime.nowUtc();
        partitionFileName = String.format("Segments/%s/track/track_%s.txt",
                JavaTime.yMd.format(now).replace(':', '-').replace(' ', '_'),
                JavaTime.yMdHms.format(now).replace(':', '-').replace(' ', '_'));
        partitionData.clear();
    }

    private boolean isPartitionFull() {
        return partitionData.size() >= 1000;
    }

    private boolean isPartitionNotInitialized() {
        return partitionFileName == null;
    }
}
