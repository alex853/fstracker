package net.simforge.fsdatafeeder;

import flightsim.simconnect.SimConnectDataType;

public enum SimStateField {
    Title("Title", null, SimConnectDataType.STRING256),
    ATC_Type("ATC Type", null, SimConnectDataType.STRING256),
    ATC_Model("ATC Model", null, SimConnectDataType.STRING256),
    Plane_Latitude("Plane Latitude", "degrees", SimConnectDataType.FLOAT64),
    Plane_Longitude("Plane Longitude", "degrees", SimConnectDataType.FLOAT64),
    Plane_Altitude("Plane Altitude", "feet", SimConnectDataType.FLOAT64),
    Plane_Heading_Degrees_True("Plane Heading Degrees True", "radians", SimConnectDataType.FLOAT64),
    Sim_On_Ground("Sim On Ground", null, SimConnectDataType.INT32),
    Is_User_Sim("Is User Sim", null, SimConnectDataType.INT32),
    Plane_In_Parking_State("Plane In Parking State", null, SimConnectDataType.INT32),
    Brake_Parking_Position("Brake Parking Position", null, SimConnectDataType.INT32),
    Eng_Combustion_1("Eng Combustion:1", null, SimConnectDataType.INT32),
    Airspeed_Indicated("Airspeed Indicated", "knots", SimConnectDataType.FLOAT64),
    Airspeed_Mach("Airspeed Mach", "mach", SimConnectDataType.FLOAT64),
    Airspeed_True("Airspeed True", "knots", SimConnectDataType.FLOAT64),
    Ground_Velocity("Ground Velocity", "knots", SimConnectDataType.FLOAT64),
    Indicated_Altitude("Indicated Altitude", "feet", SimConnectDataType.FLOAT64),
    Vertical_Speed("Vertical Speed", "feet per second", SimConnectDataType.FLOAT64),
    Heading_Indicator("Heading Indicator", null, SimConnectDataType.FLOAT64);

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
