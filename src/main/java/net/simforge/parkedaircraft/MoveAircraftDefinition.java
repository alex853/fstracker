package net.simforge.parkedaircraft;

import flightsim.simconnect.SimConnect;
import net.simforge.fsdatafeeder.SimStateField;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

    public MoveAircraftDefinition(final Logic.SavedAircraft savedAircraft) {
        this.latitude = savedAircraft.latitude;
        this.longitude = savedAircraft.longitude;
        this.altitude = savedAircraft.altitude + 1;
        this.heading = savedAircraft.heading;
    }

    public void apply(final SimConnect simConnect) {
        try {
            final byte[] bytes = new byte[4*8];
            final ByteBuffer buf = ByteBuffer.wrap(bytes);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.putDouble(this.latitude);
            buf.putDouble(this.longitude);
            buf.putDouble(this.altitude);
            buf.putDouble(this.heading);
            simConnect.setDataOnSimObject(SimWorker.Definition.MoveAircraft.ordinal(), 0, false, 1, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
