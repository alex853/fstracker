package net.simforge.fsdatafeeder;

import flightsim.simconnect.SimConnectDataType;

public enum SimStateField {
    Title("Title", null, SimConnectDataType.STRING256),
    ATC_Type("ATC Type", null, SimConnectDataType.STRING256),
    ATC_Model("ATC Model", null, SimConnectDataType.STRING256),
    Plane_Latitude("Plane Latitude", "degrees", SimConnectDataType.FLOAT64),
    Plane_Longitude("Plane Longitude", "degrees", SimConnectDataType.FLOAT64),
    Plane_Altitude("Plane Altitude", "feet", SimConnectDataType.FLOAT64),
    Sim_On_Ground("Sim On Ground", null, SimConnectDataType.INT32),
    Ground_Velocity("Ground Velocity", null, SimConnectDataType.FLOAT64),
    Is_User_Sim("Is User Sim", null, SimConnectDataType.INT32),
    Plane_In_Parking_State("Plane In Parking State", null, SimConnectDataType.INT32),
    Brake_Parking_Position("Brake Parking Position", null, SimConnectDataType.INT32),
    Eng_Combustion_1("Eng Combustion:1", null, SimConnectDataType.INT32);

    private final String title;
    private final String unitsName;

    private final SimConnectDataType dataType;

    SimStateField(String title, String unitsName, SimConnectDataType dataType) {
        this.title = title;
        this.unitsName = unitsName;
        this.dataType = dataType;
    }

    public String getTitle() {
        return title;
    }

    public String getUnitsName() {
        return unitsName;
    }

    public SimConnectDataType getDataType() {
        return dataType;
    }
}
