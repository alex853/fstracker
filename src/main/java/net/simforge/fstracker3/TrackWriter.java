package net.simforge.fstracker3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.simforge.commons.io.IOHelper;
import net.simforge.commons.misc.JavaTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedList;

import static net.simforge.fstracker3.TrackEntryInfo.Field.*;

public class TrackWriter {
    private static final Logger log = LoggerFactory.getLogger(TrackWriter.class);

    private static final DecimalFormat d1 = new DecimalFormat("0.0");
    private static final DecimalFormat d3 = new DecimalFormat("0.0##");
    private static final DecimalFormat d6 = new DecimalFormat("0.0#####");

    private String partitionFileName;
    private final LinkedList<TrackEntryInfo> partitionData = new LinkedList<>();
    private long lastWriteTs;

    public synchronized void append(final SimState simState) throws IOException {
        if (isPartitionFull() || isPartitionNotInitialized()) {
            writePartitionData();
            startNewPartition();
        }

        partitionData.add(toTrackEntryInfo(simState));

        if (isTimeToWritePartitionData()) {
            writePartitionData();
        }
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
        trackInfo.put(engine_running, simState.getEngineCombustion1());

        return trackInfo;
    }

    private void writePartitionData() throws IOException {
        if (partitionData.isEmpty()) {
            return;
        }

        final Gson gson = new GsonBuilder().setPrettyPrinting().create();

        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("version", 1);

        final JsonObject rangeObject = new JsonObject();
        rangeObject.addProperty("from", partitionData.getFirst().getTimestamp());
        rangeObject.addProperty("to", partitionData.getLast().getTimestamp());
        jsonObject.add("range", rangeObject);

        final JsonArray trackDataArray = new JsonArray();
        partitionData.forEach(entry -> trackDataArray.add(
                entry.getTimestamp() + ";" +
                entry.getTitle() + ";" +
                d6.format(entry.getLatitude()) + ";" +
                d6.format(entry.getLongitude()) + ";" +
                d1.format(entry.getAltitude()) + ";" +
                entry.getOnGround() + ";" +
                d3.format(entry.getGroundspeed()) + ";" +
                entry.getParkingBrake() + ";" +
                entry.getEngineRunning()));

        final JsonObject trackObject = new JsonObject();
        trackObject.addProperty("fields", "ts;title;lat;lon;alt;on_ground;groundspeed;parking_brake;engine_running");
        trackObject.add("data", trackDataArray);

        jsonObject.add("track", trackObject);

        final String json = gson.toJson(jsonObject);

        final File file = new File(partitionFileName);
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();
        IOHelper.saveFile(file, json);

        lastWriteTs = System.currentTimeMillis();
        log.trace("Partition data with {} positions saved", trackDataArray.size());
    }

    private void cleanState() {
        partitionFileName = null;
        partitionData.clear();
        lastWriteTs = 0;
        log.trace("Partition data cleaned up");
    }

    private void startNewPartition() {
        final LocalDateTime now = JavaTime.nowUtc();
        partitionFileName = String.format("Segments/%s/track/track_%s.json",
                JavaTime.yMd.format(now).replace(':', '-').replace(' ', '_'),
                JavaTime.yMdHms.format(now).replace(':', '-').replace(' ', '_'));
        partitionData.clear();
        lastWriteTs = 0;
        log.trace("New partition {} started", partitionFileName);
    }

    private boolean isTimeToWritePartitionData() {
        return (System.currentTimeMillis() - lastWriteTs) >= 30000;
    }

    private boolean isPartitionFull() {
        return partitionData.size() >= 1000;
    }

    private boolean isPartitionNotInitialized() {
        return partitionFileName == null;
    }

    public synchronized void close() throws IOException {
        if (isPartitionNotInitialized()) {
            return;
        }

        writePartitionData();
        cleanState();
    }
}
