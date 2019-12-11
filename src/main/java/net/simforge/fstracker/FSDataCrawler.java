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
                            System.out.println(LocalDateTime.now() + " Could not connect to SIM");
                            fsuipc_wrapper.Close();
                            continue;
                        }

                        connected = true;
                        fsuipc = new FSUIPC();
                    }

                    short year = fsuipc.getShort(0x0240);
                    if (year == 0) {
                        System.out.println(LocalDateTime.now() + " Year = 0, disconnection is assumed");
                        fsuipc_wrapper.Close();
                        fsuipc = null;
                        connected = false;
                        continue;
                    }

                    long startTs = System.currentTimeMillis();

                    boolean slewMode = fsuipc.getShort(0x05DC) == 1;
                    boolean paused = fsuipc.getShort(0x0264) == 1;
                    boolean onGround = fsuipc.getShort(0x0366) == 1;

                    LocalDateTime fsLocalDateTime = LocalDateTime.of(
                            fsuipc.getShort(0x0240),
                            1,
                            1,
                            fsuipc.getByte(0x023B),
                            fsuipc.getByte(0x023C),
                            fsuipc.getByte(0x023A));
                    fsLocalDateTime = fsLocalDateTime.plusDays(fsuipc.getShort(0x023E) - 1);

                    double groundAltitude = fsuipc.getInt(0x0020) / 256.0 / 0.3048;
                    double altitude = fsuipc.getInt(0x0574) / 0.3048;
                    double gs = fsuipc.getInt(0x02B4) / 65536.0 / 1852.0 * 3600.0;
                    double tas = fsuipc.getInt(0x02B8) / 128.0;
                    double ias = fsuipc.getInt(0x02BC) / 128.0;
                    double heading = fsuipc.getInt(0x0580) / 65536.0 / 65536.0 * 360.0;
                    if (heading < 0) heading += 360;
                    double latitude = fsuipc.getLong(0x0560) * 90.0/(10001750.0 * 65536.0 * 65536.0);
                    double longitude = fsuipc.getLong(0x0568) * 360.0/(65536.0 * 65536.0 * 65536.0 * 65536.0);

                    String aircraftTitle = fsuipc.getString(0x3D00, 256).trim();

                    String atcFlightNumber = fsuipc.getString(0x3130, 12).trim();
                    String atcIdentifier = fsuipc.getString(0x313C, 12).trim();
                    String atcAirlineName = fsuipc.getString(0x3148, 24).trim();
                    String atcAircraftType = fsuipc.getString(0x3160, 24).trim();
                    String atcModel = fsuipc.getString(0x3500, 24).trim();
                    String atc = atcFlightNumber + "/" + atcIdentifier + "/" + atcAirlineName + "/" + atcAircraftType + "/" + atcModel;

                    long elapsedTs = System.currentTimeMillis() - startTs;

                    if (elapsedTs > 500) {
                        System.out.println(LocalDateTime.now() + " READING IS TOO SLOW, disconnection is assumed");
                        fsuipc_wrapper.Close();
                        fsuipc = null;
                        connected = false;
                        continue;
                    }

                    System.out.println(LocalDateTime.now() + " Data: DT " + fsLocalDateTime + " | SLEW " + slewMode + " | PAUSED " + paused + " | ON_GROUND " + onGround + " | GALT " + dd1(groundAltitude) + " | ALT " + dd1(altitude) + " | GS " + dd1(gs) + " | TAS " + dd1(tas) + " | IAS " + dd1(ias) + " | HDG " + dd1(heading) + " | LAT " + dd6(latitude) + " | LON " + dd6(longitude) + " | TITLE " + aircraftTitle + " | ATC " + atc);

                }
            }
        };


        fsuipcThread.start();

    }

    private static class ReadResult {

    }

    private static String dd1(double v) {
        return new DecimalFormat("0.0").format(v);
    }

    private static String dd6(double v) {
        return new DecimalFormat("0.000000").format(v);
    }

}
