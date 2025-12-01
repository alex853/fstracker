package net.simforge.parkedaircraft2;

import net.simforge.fsdatafeeder.SimStateField;

public class MoveAircraftDefinition {
    public static final SimStateField[] fields = {
            SimStateField.Plane_Latitude,
            SimStateField.Plane_Longitude,
            SimStateField.Plane_Altitude,
            SimStateField.Plane_Heading_Degrees_True
    };

    final double latitude;
    final double longitude;
    final double altitude;
    final double heading;

    public MoveAircraftDefinition(double latitude, double longitude, double altitude, double heading) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.heading = heading;
    }

}
