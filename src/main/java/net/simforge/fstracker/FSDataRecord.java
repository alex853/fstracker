package net.simforge.fstracker;

import java.text.DecimalFormat;
import java.time.LocalDateTime;

public class FSDataRecord {
    private LocalDateTime localDateTime;
    private LocalDateTime fsZuluDateTime;

    private boolean slewMode;
    private boolean paused;
    private boolean onGround;

    private double groundAltitude;
    private double altitude;
    private double gs;
    private double tas;
    private double ias;
    private double heading;
    private double latitude;
    private double longitude;

    private String aircraftTitle;

    private String atcFlightNumber;
    private String atcIdentifier;
    private String atcAirlineName;
    private String atcAircraftType;
    private String atcModel;

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public LocalDateTime getFsZuluDateTime() {
        return fsZuluDateTime;
    }

    public void setFsZuluDateTime(LocalDateTime fsZuluDateTime) {
        this.fsZuluDateTime = fsZuluDateTime;
    }

    public boolean isSlewMode() {
        return slewMode;
    }

    public void setSlewMode(boolean slewMode) {
        this.slewMode = slewMode;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public double getGroundAltitude() {
        return groundAltitude;
    }

    public void setGroundAltitude(double groundAltitude) {
        this.groundAltitude = groundAltitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getGs() {
        return gs;
    }

    public void setGs(double gs) {
        this.gs = gs;
    }

    public double getTas() {
        return tas;
    }

    public void setTas(double tas) {
        this.tas = tas;
    }

    public double getIas() {
        return ias;
    }

    public void setIas(double ias) {
        this.ias = ias;
    }

    public double getHeading() {
        return heading;
    }

    public void setHeading(double heading) {
        this.heading = heading;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAircraftTitle() {
        return aircraftTitle;
    }

    public void setAircraftTitle(String aircraftTitle) {
        this.aircraftTitle = aircraftTitle;
    }

    public String getAtcFlightNumber() {
        return atcFlightNumber;
    }

    public void setAtcFlightNumber(String atcFlightNumber) {
        this.atcFlightNumber = atcFlightNumber;
    }

    public String getAtcIdentifier() {
        return atcIdentifier;
    }

    public void setAtcIdentifier(String atcIdentifier) {
        this.atcIdentifier = atcIdentifier;
    }

    public String getAtcAirlineName() {
        return atcAirlineName;
    }

    public void setAtcAirlineName(String atcAirlineName) {
        this.atcAirlineName = atcAirlineName;
    }

    public String getAtcAircraftType() {
        return atcAircraftType;
    }

    public void setAtcAircraftType(String atcAircraftType) {
        this.atcAircraftType = atcAircraftType;
    }

    public String getAtcModel() {
        return atcModel;
    }

    public void setAtcModel(String atcModel) {
        this.atcModel = atcModel;
    }

    @Override
    public String toString() {
        return "FSData||| ZULU: " + fsZuluDateTime + " | SLEW " + slewMode + " | PAUSED " + paused + " | ON_GROUND " + onGround + " | GALT " + dd1(groundAltitude) + " | ALT " + dd1(altitude) + " | GS " + dd1(gs) + " | TAS " + dd1(tas) + " | IAS " + dd1(ias) + " | HDG " + dd1(heading) + " | LAT " + dd6(latitude) + " | LON " + dd6(longitude) + " | TITLE " + aircraftTitle + " | ATC " + (atcFlightNumber + "/" + atcIdentifier + "/" + atcAirlineName + "/" + atcAircraftType + "/" + atcModel);

    }

    private static String dd1(double v) {
        return new DecimalFormat("0.0").format(v);
    }

    private static String dd6(double v) {
        return new DecimalFormat("0.000000").format(v);
    }

}
