package net.simforge.fstracker3;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectDataType;
import flightsim.simconnect.SimObjectType;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import flightsim.simconnect.recv.*;
import net.simforge.commons.misc.Geo;
import net.simforge.commons.misc.Str;
import net.simforge.refdata.airports.Airport;
import net.simforge.refdata.airports.Airports;

import java.io.IOException;
import java.text.DecimalFormat;

public class FSTracker3 implements EventHandler, OpenHandler, QuitHandler, SimObjectDataTypeHandler {
    private static final int DEFINITION_0 = 0;
    private static final int DEFINITION_1 = 1;

    private static final int REQUEST_1 = 1;

    private static final DecimalFormat coordFormat = new DecimalFormat("##0.000000");
    private static final DecimalFormat d1 = new DecimalFormat("0.0");

    private static final Airports airports = Airports.get();

    private SimState currentSimState;

    private TrackWriter trackWriter = new TrackWriter();

    public static void main(String[] args) throws IOException, ConfigurationNotFoundException, InterruptedException {
        SimConnect sc = new SimConnect("FSTracker3", 0);

        // Set up the data definition, but do not yet do anything with it
        sc.addToDataDefinition(DEFINITION_1, "Title", null, SimConnectDataType.STRING256);
        sc.addToDataDefinition(DEFINITION_1, "ATC Type", null, SimConnectDataType.STRING256);
        sc.addToDataDefinition(DEFINITION_1, "ATC Model", null, SimConnectDataType.STRING256);
        sc.addToDataDefinition(DEFINITION_1, "Plane Latitude", "degrees", SimConnectDataType.FLOAT64);
        sc.addToDataDefinition(DEFINITION_1, "Plane Longitude", "degrees", SimConnectDataType.FLOAT64);
        sc.addToDataDefinition(DEFINITION_1, "Plane Altitude", "feet", SimConnectDataType.FLOAT64);
        sc.addToDataDefinition(DEFINITION_1, "Sim On Ground", null, SimConnectDataType.INT32);
        sc.addToDataDefinition(DEFINITION_1, "Ground Velocity", null, SimConnectDataType.FLOAT64);
        sc.addToDataDefinition(DEFINITION_1, "Is User Sim", null, SimConnectDataType.INT32);
        sc.addToDataDefinition(DEFINITION_1, "Plane In Parking State", null, SimConnectDataType.INT32);
        sc.addToDataDefinition(DEFINITION_1, "Brake Parking Position", null, SimConnectDataType.INT32);


        // Request an event when the simulation starts
        sc.subscribeToSystemEvent(DEFINITION_0, "SimStart");
        sc.subscribeToSystemEvent(DEFINITION_1, "1sec");
        sc.subscribeToSystemEvent(DEFINITION_0, "SimStop");

        FSTracker3 fst2 = new FSTracker3();

        // dispatcher
        DispatcherTask dt = new DispatcherTask(sc);
        dt.addOpenHandler(fst2);
        dt.addQuitHandler(fst2);
        dt.addEventHandler(fst2);
        dt.addSimObjectDataTypeHandler(fst2);

        Thread thread = dt.createThread();
        thread.start();

        try {
            while (true) {
                Thread.sleep(1000);
            }
        } finally {
            dt.tryStop();

        }
    }

    public void handleOpen(SimConnect sender, RecvOpen e) {
        clearLine();
        System.out.println("Connected to : " + e.getApplicationName() + " "
                + e.getApplicationVersionMajor() + "."
                + e.getApplicationVersionMinor());
    }

    public void handleQuit(SimConnect sender, RecvQuit e) {
        clearLine();
        System.out.println("Disconnected from : " + e.toString());
    }

    public void handleEvent(SimConnect sender, RecvEvent e) {

        // Now the sim is running, request information on the user aircraft
        try {
            sender.requestDataOnSimObjectType(REQUEST_1, DEFINITION_1, 0, SimObjectType.USER);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void handleSimObjectType(SimConnect sender, RecvSimObjectDataByType e) {
        if (e.getRequestID() == REQUEST_1) {

            SimState state = new SimState();
            int objectId = e.getObjectID();
            state.setTitle(e.getDataString256());
            state.setAtcType(e.getDataString256());
            state.setAtcModel(e.getDataString256());
            state.setLatitude(e.getDataFloat64());
            state.setLongitude(e.getDataFloat64());
            state.setAltitude(e.getDataFloat64());
            state.setOnGround(e.getDataInt32());
            state.setGroundVelocity(e.getDataFloat64());
            state.setIsUserSim(e.getDataInt32());
            state.setPlaneInParkingState(e.getDataInt32());
            state.setBrakeParkingPosition(e.getDataInt32());

            currentSimState = state;

            try {
                trackWriter.append(currentSimState);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        printState();
    }

    private String findIcao(Geo.Coords coords) {
        Airport nearest = airports.findNearest(coords);
        if (nearest == null) {
            return null;
        }
        return nearest.getIcao();
    }

    private void printState() {
        clearLine();
        System.out.printf("    | %s : GND %s    Lat %s    Lon %s    DIR-TO-ZERO %s    G/S %s U/S %s P/S %s P/P %s  STATIONARY %s",
                Str.limit(currentSimState.getTitle(), 12) + "...",
                currentSimState.getOnGround(),
                coordFormat.format(currentSimState.getLatitude()),
                coordFormat.format(currentSimState.getLongitude()),
                d1.format(Geo.distance(Geo.coords(currentSimState.getLatitude(), currentSimState.getLongitude()), Geo.coords(0, 0))),
                d1.format(currentSimState.getGroundVelocity()),
                currentSimState.getIsUserSim(),
                currentSimState.getPlaneInParkingState(),
                currentSimState.getBrakeParkingPosition(),
                currentSimState.isStationary());
    }

    private void clearLine() {
        System.out.print("                                                                                                                                       ");
        System.out.print("\r");
    }
}
