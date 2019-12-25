package net.simforge.fstracker;

import com.google.gson.Gson;
import net.simforge.commons.misc.Misc;
import net.simforge.fstracker.fsdata.FSData;
import net.simforge.fstracker.fsdata.FSDataProvider;
import net.simforge.fstracker.fsdata.FSDataRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.simforge.commons.misc.Str.isEmpty;

public class FSDataFileWriter implements Runnable {
    private FSDataProvider provider;
    private volatile boolean stopRequested = false;
    private LocalDate dateOfFile;
    private FileOutputStream fos;

    public FSDataFileWriter(FSDataProvider provider) {
        this.provider = provider;
    }

    @Override
    public void run() {
        while (!stopRequested) {
            FSDataRecord next = provider.next();
            if (next == null) {
                Misc.sleep(100);
                continue;
            }

            if (dateOfFile == null) {
                openFile();
            } else if (!dateOfFile.equals(currDate())) {
                closeFile();
                openFile();
            }

            String line = toJson(next) + "\r\n";
            try {
                fos.write(line.getBytes());

                fos.flush();
            } catch (IOException e) {
                e.printStackTrace(); // todo
            }
        }

        closeFile();
    }

    public void requestStop() {
        stopRequested = true;
    }

    private String toJson(FSDataRecord record) {
        DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        DateTimeFormatter fsZuluFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        Map<String, Object> recordMap = new LinkedHashMap<>();

        recordMap.put("dt", dtFormatter.format(record.getDateTime()));
        recordMap.put("status", record.getStatus());

        FSData fsData = record.getFsData();
        if (fsData != null) {
            Map<String, Object> fsDataMap = new LinkedHashMap<>();

            fsDataMap.put("dt", fsZuluFormatter.format(fsData.getFsZuluDateTime()));
            fsDataMap.put("slew", booleanToString(fsData.isSlewMode()));
            fsDataMap.put("paused", booleanToString(fsData.isPaused()));
            fsDataMap.put("onGround", booleanToString(fsData.isOnGround()));

            fsDataMap.put("elev", d1(fsData.getGroundAltitude()));
            fsDataMap.put("alt", d1(fsData.getAltitude()));
            fsDataMap.put("gs", d1(fsData.getGs()));
            fsDataMap.put("tas", d1(fsData.getTas()));
            fsDataMap.put("ias", d1(fsData.getIas()));
            fsDataMap.put("hdg", d1(fsData.getHeading()));
            fsDataMap.put("lat", d6(fsData.getLatitude()));
            fsDataMap.put("lng", d6(fsData.getLongitude()));

            if (!isEmpty(fsData.getAircraftTitle()))
                fsDataMap.put("a.title", fsData.getAircraftTitle());

            if (!isEmpty(fsData.getAtcFlightNumber()))
                fsDataMap.put("atc.fn", fsData.getAtcFlightNumber());
            if (!isEmpty(fsData.getAtcIdentifier()))
                fsDataMap.put("atc.id", fsData.getAtcIdentifier());
            if (!isEmpty(fsData.getAtcAirlineName()))
                fsDataMap.put("atc.an", fsData.getAtcAirlineName());
            if (!isEmpty(fsData.getAtcAircraftType()))
                fsDataMap.put("atc.typ", fsData.getAtcAircraftType());
            if (!isEmpty(fsData.getAtcModel()))
                fsDataMap.put("atc.mdl", fsData.getAtcModel());

            recordMap.put("fs", fsDataMap);
        }

        return new Gson().toJson(recordMap);
    }

    private void openFile() {
        dateOfFile = currDate();
        new File("fsdata").mkdirs();
        try {
            fos = new FileOutputStream("fsdata/" + dateOfFile.toString() + ".txt", true);
        } catch (FileNotFoundException e) {
            e.printStackTrace(); // todo
        }
    }

    private void closeFile() {
        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace(); // todo
        }
    }

    private LocalDate currDate() {
        return LocalDate.now(ZoneId.of("UTC"));
    }

    private String booleanToString(boolean v) {
        return v ? "Y" : "N";
    }

    private DecimalFormat d1 = new DecimalFormat("0.0");
    private DecimalFormat d6 = new DecimalFormat("0.000000");

    private String d1(double v) {
        return dd(d1, v);
    }

    private String d6(double v) {
        return dd(d6, v);
    }

    private String dd(DecimalFormat dd, double v) {
        String r = dd.format(v);
        return r.replace(',', '.');
    }
}
