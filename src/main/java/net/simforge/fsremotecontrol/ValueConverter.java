package net.simforge.fsremotecontrol;

public interface ValueConverter {

    Object convert(Object value);

    ValueConverter roundDoubleToInt = (d) -> (int) Math.round((double) d);

    ValueConverter roundMachTo2digits = (d) -> Math.round((double) d * 100) / 100.0;

    ValueConverter radiansToDegrees = (r) -> (int) ((double) r * 360/(2*Math.PI));

    ValueConverter feetPerSecondToFeetPerMinute = (vs) -> (int) Math.round((double) vs * 60);
}
