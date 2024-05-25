package net.simforge.fstracker3;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectDataType;
import flightsim.simconnect.SimObjectType;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import flightsim.simconnect.recv.*;
import net.simforge.commons.misc.Geo;
import net.simforge.commons.misc.Str;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;

public class FSDataFeeder implements EventHandler, OpenHandler, QuitHandler, SimObjectDataTypeHandler, SystemStateHandler, ExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(FSDataFeeder.class);

    private static final int DEFINITION_0 = 0;
    private static final int DEFINITION_1 = 1;

    private static final int REQUEST_1 = 1;

    private static final DecimalFormat coordFormat = new DecimalFormat("##0.000000");
    private static final DecimalFormat d1 = new DecimalFormat("0.0");

    private SimState currentSimState;

    private final TrackWriter trackWriter = new TrackWriter();

    private boolean simQuit = false;

    public void connectAndRunCollectionCycle() throws IOException, ConfigurationNotFoundException, InterruptedException {
        SimConnect sc = new SimConnect("FSDataFeeder", 0);

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
        sc.addToDataDefinition(DEFINITION_1, "Eng Combustion:1", null, SimConnectDataType.INT32);

        // Request an event when the simulation starts
        sc.subscribeToSystemEvent(DEFINITION_0, "SimStart");
        sc.subscribeToSystemEvent(DEFINITION_1, "1sec");
        sc.subscribeToSystemEvent(DEFINITION_0, "SimStop");

        // dispatcher
        DispatcherTask dt = new DispatcherTask(sc);
        dt.addOpenHandler(this);
        dt.addQuitHandler(this);
        dt.addEventHandler(this);
        dt.addSimObjectDataTypeHandler(this);
        dt.addSystemStateHandler(this);
        dt.addExceptionHandler(this);

        Thread thread = dt.createThread();
        thread.start();

        try {
            while (!simQuit) {
                //noinspection BusyWait
                Thread.sleep(1000);
            }
        } finally {
            trackWriter.close();
            dt.tryStop();
        }
    }

    public void handleOpen(SimConnect sender, RecvOpen e) {
        log.warn("Connected to : " + e.getApplicationName() + " "
                + e.getApplicationVersionMajor() + "."
                + e.getApplicationVersionMinor());
    }

    public void handleQuit(SimConnect sender, RecvQuit e) {
        simQuit = true;
        log.warn("Disconnected from : " + e.toString());
    }

    public void handleEvent(SimConnect sender, RecvEvent e) {
        // Now the sim is running, request information on the user aircraft
        try {
            sender.requestDataOnSimObjectType(REQUEST_1, DEFINITION_1, 0, SimObjectType.USER);
        } catch (final IOException ex) {
            log.error("Error while reading", ex);
        }
    }

    public void handleSimObjectType(SimConnect sender, RecvSimObjectDataByType e) {
        if (e.getRequestID() == REQUEST_1) {
            final SimState state = new SimState();
            //noinspection unused
            final int objectId = e.getObjectID();

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
            state.setEngineCombustion1(e.getDataInt32());

            currentSimState = state;

            boolean outsideSimulation = Geo.distance(Geo.coords(state.getLatitude(), state.getLongitude()), Geo.coords(0, 0)) < 1;

            if (!outsideSimulation) {
                try {
                    trackWriter.append(currentSimState);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        printState();
    }

    @Override
    public void handleSystemState(SimConnect simConnect, RecvSystemState recvSystemState) {
        log.trace("recvSystemState { " + recvSystemState.getRequestID() + ", " + recvSystemState.getDataInteger() + ", " + recvSystemState.getDataFloat() + ", " + recvSystemState.getDataString() + " }");
    }

    @Override
    public void handleException(SimConnect simConnect, RecvException e) {
        log.trace("recvException = " + e + ", " + e.getException());
    }

    private void printState() {
        log.info(String.format("    | %s : GND %s    Lat %s    Lon %s    DIR-TO-ZERO %s    G/S %s U/S %s P/S %s P/P %s  STATIONARY %s ENG1 %s",
                Str.limit(currentSimState.getTitle(), 12) + "...",
                currentSimState.getOnGround(),
                coordFormat.format(currentSimState.getLatitude()),
                coordFormat.format(currentSimState.getLongitude()),
                d1.format(Geo.distance(Geo.coords(currentSimState.getLatitude(), currentSimState.getLongitude()), Geo.coords(0, 0))),
                d1.format(currentSimState.getGroundVelocity()),
                currentSimState.getIsUserSim(),
                currentSimState.getPlaneInParkingState(),
                currentSimState.getBrakeParkingPosition(),
                currentSimState.isStationary(),
                currentSimState.getEngineCombustion1()));
    }
}
