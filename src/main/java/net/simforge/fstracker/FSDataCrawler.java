package net.simforge.fstracker;

import com.flightsim.fsuipc.FSUIPC;
import com.flightsim.fsuipc.fsuipc_wrapper;
import net.simforge.commons.misc.Misc;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FSDataCrawler {
    private static final Queue<ReadResult> readingQueue = new ConcurrentLinkedQueue<>();

    private static volatile boolean stopRequested = false;

    public static void main(String[] args) throws InterruptedException {

        Thread fsuipcThread = new Thread() {
            // if stop requested - disconnect if connected and exit
            // if not connected - try to connect
            // if connected - read record
            // plus read some checking fields before and after (year?)
            // plus calculate elapsed time
            // provide data to outer queue

            private boolean connected = false;
            private FSUIPC fsuipc;

            @Override
            public void run() {
                while (!stopRequested) {
                    Misc.sleep(1000);

                    if (!connected) {
                        int ret = fsuipc_wrapper.Open(fsuipc_wrapper.SIM_ANY);

                        if (ret == 0) {
//                            System.out.println(LocalDateTime.now() + " Could not connect to SIM");
                            readingQueue.offer(new ReadResult(ReadStatus.NO_CONNECTION));
                            fsuipc_wrapper.Close();
                            continue;
                        }

                        connected = true;
                        fsuipc = new FSUIPC();
                    }

                    short year = fsuipc.getShort(0x0240);
                    if (year == 0) {
//                        System.out.println(LocalDateTime.now() + " Year = 0, disconnection is assumed");
                        readingQueue.offer(new ReadResult(ReadStatus.INVALID_CONNECTION));
                        fsuipc_wrapper.Close();
                        fsuipc = null;
                        connected = false;
                        continue;
                    }

                    long startTs = System.currentTimeMillis();

                    FSDataRecord fsDataRecord = readFSDataRecord();

                    long elapsedTs = System.currentTimeMillis() - startTs;

                    if (elapsedTs > 500) {
//                        System.out.println(LocalDateTime.now() + " READING IS TOO SLOW, disconnection is assumed");
                        readingQueue.offer(new ReadResult(ReadStatus.READ_TIMEOUT, fsDataRecord));
                        fsuipc_wrapper.Close();
                        fsuipc = null;
                        connected = false;
                        continue;
                    }

                    readingQueue.offer(new ReadResult(ReadStatus.OK, fsDataRecord));
                }
            }

            private FSDataRecord readFSDataRecord() {
                FSDataRecord fsDataRecord = new FSDataRecord();

                fsDataRecord.setLocalDateTime(LocalDateTime.now());
                LocalDateTime fsLocalDateTime = LocalDateTime.of(
                        fsuipc.getShort(0x0240),
                        1,
                        1,
                        fsuipc.getByte(0x023B),
                        fsuipc.getByte(0x023C),
                        fsuipc.getByte(0x023A));
                fsLocalDateTime = fsLocalDateTime.plusDays(fsuipc.getShort(0x023E) - 1);
                fsDataRecord.setFsZuluDateTime(fsLocalDateTime);

                fsDataRecord.setSlewMode(fsuipc.getShort(0x05DC) == 1);
                fsDataRecord.setPaused(fsuipc.getShort(0x0264) == 1);
                fsDataRecord.setOnGround(fsuipc.getShort(0x0366) == 1);

                fsDataRecord.setGroundAltitude(fsuipc.getInt(0x0020) / 256.0 / 0.3048);
                fsDataRecord.setAltitude(fsuipc.getInt(0x0574) / 0.3048);
                fsDataRecord.setGs(fsuipc.getInt(0x02B4) / 65536.0 / 1852.0 * 3600.0);
                fsDataRecord.setTas(fsuipc.getInt(0x02B8) / 128.0);
                fsDataRecord.setIas(fsuipc.getInt(0x02BC) / 128.0);
                double heading = fsuipc.getInt(0x0580) / 65536.0 / 65536.0 * 360.0;
                if (heading < 0) heading += 360;
                fsDataRecord.setHeading(heading);
                fsDataRecord.setLatitude(fsuipc.getLong(0x0560) * 90.0 / (10001750.0 * 65536.0 * 65536.0));
                fsDataRecord.setLongitude(fsuipc.getLong(0x0568) * 360.0 / (65536.0 * 65536.0 * 65536.0 * 65536.0));

                fsDataRecord.setAircraftTitle(fsuipc.getString(0x3D00, 256).trim());

                fsDataRecord.setAtcFlightNumber(fsuipc.getString(0x3130, 12).trim());
                fsDataRecord.setAtcIdentifier(fsuipc.getString(0x313C, 12).trim());
                fsDataRecord.setAtcAirlineName(fsuipc.getString(0x3148, 24).trim());
                fsDataRecord.setAtcAircraftType(fsuipc.getString(0x3160, 24).trim());
                fsDataRecord.setAtcModel(fsuipc.getString(0x3500, 24).trim());

                return fsDataRecord;
            }
        };


        fsuipcThread.start();

        while (true) {

            while (true) {
                ReadResult readResult = readingQueue.poll();
                if (readResult == null)
                    break;

                System.out.println(readResult.getStatus() + "    " + (readResult.getFsDataRecord() != null ? readResult.getFsDataRecord().toString() : "NO DATA"));
            }

            Thread.sleep(100);

        }
    }

    private static class ReadResult {
        private ReadStatus status;
        private FSDataRecord fsDataRecord;

        public ReadResult(ReadStatus status) {
            this.status = status;
        }

        public ReadResult(ReadStatus status, FSDataRecord fsDataRecord) {
            this.status = status;
            this.fsDataRecord = fsDataRecord;
        }

        public ReadStatus getStatus() {
            return status;
        }

        public FSDataRecord getFsDataRecord() {
            return fsDataRecord;
        }
    }

    private enum ReadStatus { NO_CONNECTION, INVALID_CONNECTION, READ_TIMEOUT, OK }

}
