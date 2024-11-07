package net.simforge.fsremotecontrol;

import net.simforge.fsdatafeeder.SimStateField;

import java.util.Arrays;

public enum JsonConversion {
    ias(SimStateField.Airspeed_Indicated, ValueConverter.roundDoubleToInt),
    mach(SimStateField.Airspeed_Mach, ValueConverter.roundMachTo2digits),
    tas(SimStateField.Airspeed_True, ValueConverter.roundDoubleToInt),
    gs(SimStateField.Ground_Velocity, ValueConverter.roundDoubleToInt),
    alt(SimStateField.Indicated_Altitude, ValueConverter.roundDoubleToInt),
    vs(SimStateField.Vertical_Speed, ValueConverter.feetPerSecondToFeetPerMinute),
    hdg(SimStateField.Heading_Indicator, ValueConverter.radiansToDegrees),
    ;

    private final SimStateField field;
    private final ValueConverter converter;

    JsonConversion(SimStateField field, ValueConverter converter) {
        this.field = field;
        this.converter = converter;
    }

    public static JsonConversion findByField(final SimStateField field) {
        return Arrays.stream(JsonConversion.values())
                .filter(c -> c.field == field)
                .findFirst()
                .orElseThrow();
    }

    public ValueConverter getConverter() {
        return converter;
    }

}
