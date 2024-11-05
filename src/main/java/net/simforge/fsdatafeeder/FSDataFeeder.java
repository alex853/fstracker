package net.simforge.fsdatafeeder;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimObjectType;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import flightsim.simconnect.recv.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

public class FSDataFeeder implements EventHandler, OpenHandler, QuitHandler, SimObjectDataTypeHandler, SystemStateHandler, ExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(FSDataFeeder.class);

    private static final int DEFINITION_0 = 0;
    private static final int DEFINITION_1 = 1;

    private static final int REQUEST_1 = 1;

    private final SimStateConsumer simStateConsumer;
    private final SimStateField[] simStateFields;

    private SimState currentSimState;

    private boolean simQuit = false;

    public FSDataFeeder(final SimStateConsumer simStateConsumer,
                        final SimStateField[] simStateFields) {
        this.simStateConsumer = simStateConsumer;
        this.simStateFields = simStateFields;
    }

    public void connectAndRunCollectionCycle() throws IOException, ConfigurationNotFoundException, InterruptedException {
        SimConnect sc = new SimConnect("FSDataFeeder", 0);

        // Set up the data definition, but do not yet do anything with it
        Arrays.stream(simStateFields).forEach(f -> {
            try {
                sc.addToDataDefinition(DEFINITION_1, f.getTitle(), f.getUnitsName(), f.getDataType());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

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

            Arrays.stream(simStateFields).forEach(f -> {
                switch (f.getDataType()) {
                    case INT32:
                        state.set(f, e.getDataInt32());
                        break;
                    case FLOAT64:
                        state.set(f, e.getDataFloat64());
                        break;
                    case STRING256:
                        state.set(f, e.getDataString256());
                        break;
                    default:
                        throw new RuntimeException("unsupported data type " + f.getDataType());
                }
            });

            currentSimState = state;

            simStateConsumer.consume(currentSimState);
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
        log.info(currentSimState.getState().toString());
    }
}
