package net.simforge.parkedaircraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import flightsim.simconnect.SimConnect;
import flightsim.simconnect.recv.RecvSimObjectDataByType;
import net.simforge.commons.io.IOHelper;
import net.simforge.fsdatafeeder.SimState;
import net.simforge.fsdatafeeder.SimStateField;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Tools {

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

    private static File makeFile(final String title) {
        return new File(System.getenv("LOCALAPPDATA") + "/simforge.net/Parked Aircraft/" + title + ".json");
    }

    public static void save(final Logic.SavedAircraft savedAircraft) {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final String json = gson.toJson(savedAircraft);
        try {
            final File file = makeFile(savedAircraft.title);
            file.getParentFile().mkdirs();
            IOHelper.saveFile(file, json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Logic.SavedAircraft loadIfExists(final String title) {
        final File file = makeFile(title);
        if (!file.exists()) {
            return null;
        }
        final String json;
        try {
            json = IOHelper.loadFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.fromJson(json, Logic.SavedAircraft.class);
    }
}
