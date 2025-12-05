package net.simforge.parkedaircraft2;

import flightsim.simconnect.recv.RecvSimObjectDataByType;
import net.simforge.fsdatafeeder.SimState;
import net.simforge.fsdatafeeder.SimStateField;

public class Mk1AircraftStateDefinition {
    public static final SimStateField[] fields = {
            SimStateField.Title,
            SimStateField.Plane_Latitude,
            SimStateField.Plane_Longitude,
            SimStateField.Plane_Altitude,
            SimStateField.Plane_Heading_Degrees_True,
            SimStateField.Sim_On_Ground,
            SimStateField.Ground_Velocity,
            SimStateField.Plane_In_Parking_State,
            SimStateField.Brake_Parking_Position,
            SimStateField.Eng_Combustion_1
    };

    final String title;
    final double latitude;
    final double longitude;
    final double altitude;
    final double heading;
    final int onGround;
    final double groundspeed;
    final int parkingBrake;
    final int eng1running;

    private Mk1AircraftStateDefinition(String title, double latitude, double longitude, double altitude, double heading, int onGround, double groundspeed, int parkingBrake, int eng1running) {
        this.title = title;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.heading = heading;
        this.onGround = onGround;
        this.groundspeed = groundspeed;
        this.parkingBrake = parkingBrake;
        this.eng1running = eng1running;
    }

    public boolean isOnGround() {
        return onGround == 1;
    }

    public static Mk1AircraftStateDefinition from(final RecvSimObjectDataByType received) {
        final SimState simState = SCTools.readSimState(received,
                Mk1AircraftStateDefinition.fields);
        return new Mk1AircraftStateDefinition(
                simState.getString(SimStateField.Title),
                simState.getDouble(SimStateField.Plane_Latitude),
                simState.getDouble(SimStateField.Plane_Longitude),
                simState.getDouble(SimStateField.Plane_Altitude),
                simState.getDouble(SimStateField.Plane_Heading_Degrees_True),
                simState.getInt(SimStateField.Sim_On_Ground),
                simState.getDouble(SimStateField.Ground_Velocity),
                simState.getInt(SimStateField.Brake_Parking_Position),
                simState.getInt(SimStateField.Eng_Combustion_1));
    }
}
