package net.simforge.fstracker3;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

public class TrackEntry {
    private final Map<Field, Object> fields = new HashMap<>();

    public void put(final Field field, final String value) {
        fields.put(field, value);
    }

    public void put(final Field field, final int value) {
        fields.put(field, value);
    }

    public void put(final Field field, final long value) {
        fields.put(field, value);
    }

    public void put(final Field field, final double value) {
        fields.put(field, value);
    }

    public boolean has(final Field field) {
        return fields.containsKey(field);
    }

    public long getTimestamp() {
        return (long) fields.get(Field.timestamp);
    }

    public LocalDateTime getDateTime() {
        return LocalDateTime.ofEpochSecond(getTimestamp(), 0, ZoneOffset.UTC);
    }

    public String getTitle() {
        return (String) fields.get(Field.title);
    }

    public double getLatitude() {
        return (double) fields.get(Field.latitude);
    }

    public double getLongitude() {
        return (double) fields.get(Field.longitude);
    }

    public double getAltitude() {
        return (double) fields.get(Field.altitude);
    }

    public int getOnGround() {
        return (int) fields.get(Field.on_ground);
    }

    public double getGroundspeed() {
        return (double) fields.get(Field.groundspeed);
    }

    public int getParkingBrake() {
        return (int) fields.get(Field.parking_brake);
    }

    public int getEngineRunning() {
        return (int) fields.get(Field.engine_running);
    }

    public enum Field {
        timestamp("ts"), // simconnect:Title
        title("title"), // simconnect:Title
        latitude("lat"), // simconnect:Plane Latitude
        longitude("lon"), // simconnect:Plane Longitude
        altitude("alt"), // simconnect:Plane Altitude
        on_ground("on_ground"), // simconnect:Sim On Ground
        groundspeed("groundspeed"), // simconnect:Ground Velocity
        parking_brake("parking_brake"), // simconnect:Brake Parking Position
        engine_running("engine_running"); // simconnect:Eng Combustion:1

        private final String code;

        Field(final String code) {
            this.code = code;
        }
    }
}
