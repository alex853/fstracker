package net.simforge.parkedaircraft2;

import flightsim.simconnect.SimConnect;
import net.simforge.fsdatafeeder.SimStateField;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SetFuelQuantitiesDefinition {
    public static final SimStateField[] fields = {
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

    final double center;
    final double center2;
    final double center3;
    final double leftMain;
    final double rightMain;
    final double leftAux;
    final double rightAux;
    final double leftTip;
    final double rightTip;
    final double external1;
    final double external2;

    public SetFuelQuantitiesDefinition(final Logic.SavedAircraft savedAircraft) {
        this.center = savedAircraft.fuel.center;
        this.center2 = savedAircraft.fuel.center2;
        this.center3 = savedAircraft.fuel.center3;
        this.leftMain = savedAircraft.fuel.leftMain;
        this.rightMain = savedAircraft.fuel.rightMain;
        this.leftAux = savedAircraft.fuel.leftAux;
        this.rightAux = savedAircraft.fuel.rightAux;
        this.leftTip = savedAircraft.fuel.leftTip;
        this.rightTip = savedAircraft.fuel.rightTip;
        this.external1 = savedAircraft.fuel.external1;
        this.external2 = savedAircraft.fuel.external2;
    }

    public void apply(final SimConnect simConnect) {
        try {
            final byte[] bytes = new byte[11*8];
            final ByteBuffer buf = ByteBuffer.wrap(bytes);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.putDouble(this.center);
            buf.putDouble(this.center2);
            buf.putDouble(this.center3);
            buf.putDouble(this.leftMain);
            buf.putDouble(this.rightMain);
            buf.putDouble(this.leftAux);
            buf.putDouble(this.rightAux);
            buf.putDouble(this.leftTip);
            buf.putDouble(this.rightTip);
            buf.putDouble(this.external1);
            buf.putDouble(this.external2);
            simConnect.setDataOnSimObject(SimWorker.Definition.SetFuelQuantities.ordinal(), 0, false, 1, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
