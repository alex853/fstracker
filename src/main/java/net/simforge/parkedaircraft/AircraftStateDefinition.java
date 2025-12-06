package net.simforge.parkedaircraft;

import flightsim.simconnect.recv.RecvSimObjectDataByType;
import net.simforge.fsdatafeeder.SimState;
import net.simforge.fsdatafeeder.SimStateField;

public class AircraftStateDefinition {
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
            SimStateField.Eng_Combustion_1,

            SimStateField.Fuel_Tank_Center_Quantity,
            SimStateField.Fuel_Tank_Center2_Quantity,
            SimStateField.Fuel_Tank_Center3_Quantity,
            SimStateField.Fuel_Tank_Left_Main_Quantity,
            SimStateField.Fuel_Tank_Right_Main_Quantity,
            SimStateField.Fuel_Tank_Left_Aux_Quantity,
            SimStateField.Fuel_Tank_Right_Aux_Quantity,
            SimStateField.Fuel_Tank_Left_Tip_Quantity,
            SimStateField.Fuel_Tank_Right_Tip_Quantity,
            SimStateField.Fuel_Tank_External1_Quantity,
            SimStateField.Fuel_Tank_External2_Quantity,
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

    final double fuelCenterQuantity;
    final double fuelCenter2Quantity;
    final double fuelCenter3Quantity;
    final double fuelLeftMainQuantity;
    final double fuelRightMainQuantity;
    final double fuelLeftAuxQuantity;
    final double fuelRightAuxQuantity;
    final double fuelLeftTipQuantity;
    final double fuelRightTipQuantity;
    final double fuelExternal1Quantity;
    final double fuelExternal2Quantity;

    private AircraftStateDefinition(final SimState simState) {
        this.title = simState.getString(SimStateField.Title);
        this.latitude = simState.getDouble(SimStateField.Plane_Latitude);
        this.longitude = simState.getDouble(SimStateField.Plane_Longitude);
        this.altitude = simState.getDouble(SimStateField.Plane_Altitude);
        this.heading = simState.getDouble(SimStateField.Plane_Heading_Degrees_True);
        this.onGround = simState.getInt(SimStateField.Sim_On_Ground);
        this.groundspeed = simState.getDouble(SimStateField.Ground_Velocity);
        this.parkingBrake = simState.getInt(SimStateField.Brake_Parking_Position);
        this.eng1running = simState.getInt(SimStateField.Eng_Combustion_1);

        this.fuelCenterQuantity = simState.getDouble(SimStateField.Fuel_Tank_Center_Quantity);
        this.fuelCenter2Quantity = simState.getDouble(SimStateField.Fuel_Tank_Center2_Quantity);
        this.fuelCenter3Quantity = simState.getDouble(SimStateField.Fuel_Tank_Center3_Quantity);
        this.fuelLeftMainQuantity = simState.getDouble(SimStateField.Fuel_Tank_Left_Main_Quantity);
        this.fuelRightMainQuantity = simState.getDouble(SimStateField.Fuel_Tank_Right_Main_Quantity);
        this.fuelLeftAuxQuantity = simState.getDouble(SimStateField.Fuel_Tank_Left_Aux_Quantity);
        this.fuelRightAuxQuantity = simState.getDouble(SimStateField.Fuel_Tank_Right_Aux_Quantity);
        this.fuelLeftTipQuantity = simState.getDouble(SimStateField.Fuel_Tank_Left_Tip_Quantity);
        this.fuelRightTipQuantity = simState.getDouble(SimStateField.Fuel_Tank_Right_Tip_Quantity);
        this.fuelExternal1Quantity = simState.getDouble(SimStateField.Fuel_Tank_External1_Quantity);
        this.fuelExternal2Quantity = simState.getDouble(SimStateField.Fuel_Tank_External2_Quantity);
    }

    public boolean isOnGround() {
        return onGround == 1;
    }

    public double getTotalFuelQuantity() {
        return fuelCenterQuantity + fuelCenter2Quantity + fuelCenter3Quantity
                + fuelLeftMainQuantity + fuelRightMainQuantity
                + fuelLeftAuxQuantity + fuelRightAuxQuantity
                + fuelLeftTipQuantity + fuelRightTipQuantity
                + fuelExternal1Quantity + fuelExternal2Quantity;
    }

    public static AircraftStateDefinition from(final RecvSimObjectDataByType received) {
        return new AircraftStateDefinition(
                Tools.readSimState(
                        received,
                        AircraftStateDefinition.fields));
    }
}
