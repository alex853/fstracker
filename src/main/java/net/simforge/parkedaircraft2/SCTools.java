package net.simforge.parkedaircraft2;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.recv.RecvSimObjectDataByType;
import net.simforge.fsdatafeeder.SimState;
import net.simforge.fsdatafeeder.SimStateField;

import java.io.IOException;
import java.util.Arrays;

public class SCTools {

    public static void addDefinition(final SimConnect simConnect, final int definitionId, final SimStateField[] fields) {
        Arrays.stream(fields).forEach(f -> {
            try {
                simConnect.addToDataDefinition(definitionId, f.getTitle(), f.getUnitsName(), f.getDataType());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static SimState readSimState(final RecvSimObjectDataByType received, final SimStateField[] fields) {
        final SimState state = new SimState();

        Arrays.stream(fields).forEach(f -> {
            switch (f.getDataType()) {
                case INT32 -> state.set(f, received.getDataInt32());
                case FLOAT64 -> state.set(f, received.getDataFloat64());
                case STRING256 -> state.set(f, received.getDataString256());
                default -> throw new RuntimeException("unsupported data type " + f.getDataType());
            }
        });

        return state;
    }

}
