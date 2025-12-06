package net.simforge.parkedaircraft;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimObjectType;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import flightsim.simconnect.recv.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SimWorker implements EventHandler, OpenHandler, QuitHandler, SimObjectDataTypeHandler, SystemStateHandler, ExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(SimWorker.class);

    public enum Definition {
        SimStart,
        SimStop,
        AircraftState,
        MoveAircraft,
        SetFuelQuantities
    }

    private static SimWorker simWorker;

    private boolean started;
    private SimConnect simConnect;

    private volatile boolean connected;
    private volatile boolean simQuit;

    private SimWorker() {

    }

    public static SimWorker get() {
        synchronized (SimWorker.class) {
            if (simWorker == null) {
                simWorker = new SimWorker();
            }
        }
        return simWorker;
    }

    public boolean isConnected() {
        return connected;
    }

    public void start() throws RuntimeException {
        synchronized (this) {
            if (started) {
                throw new IllegalStateException("already started");
            }
            started = true;
        }

        final Thread starterThread = new Thread(() -> {
            while (true) {
                try {
                    doSimConnectStuff();
                } catch (final IOException e) {
                    log.warn("Connection unsuccessful: " + e.getMessage());
                } catch (final ConfigurationNotFoundException e) {
                    log.error("Configuration not found", e);
                    break;
                } catch (InterruptedException e) {
                    log.error("Interrupted", e);
                    break;
                } finally {
                    simConnect = null;
                    simQuit = false;
                }

                try {
                    //noinspection BusyWait
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "SimWorkerStarter");

        starterThread.setDaemon(true);
        starterThread.start();
    }

    private void doSimConnectStuff() throws IOException, ConfigurationNotFoundException, InterruptedException {
        simConnect = new SimConnect("ParkedAircraft", 0);

        simConnect.subscribeToSystemEvent(Definition.SimStart.ordinal(), "SimStart");
        simConnect.subscribeToSystemEvent(Definition.SimStop.ordinal(), "SimStop");

        Tools.addDefinition(simConnect, Definition.AircraftState.ordinal(), AircraftStateDefinition.fields);
        simConnect.subscribeToSystemEvent(Definition.AircraftState.ordinal(), "1sec");

        Tools.addDefinition(simConnect, Definition.MoveAircraft.ordinal(), MoveAircraftDefinition.fields);
        Tools.addDefinition(simConnect, Definition.SetFuelQuantities.ordinal(), SetFuelQuantitiesDefinition.fields);

        // dispatcher
        final DispatcherTask dt = new DispatcherTask(simConnect);
        dt.addOpenHandler(this);
        dt.addQuitHandler(this);
        dt.addEventHandler(this);
        dt.addSimObjectDataTypeHandler(this);
        dt.addSystemStateHandler(this);
        dt.addExceptionHandler(this);

        final Thread thread = dt.createThread();
        thread.start();

        connected = true;
        Logic.get().whenConnectedToSim();

        try {
            while (!simQuit) {
                //noinspection BusyWait
                Thread.sleep(1000);
            }
        } finally {
            connected = false;
            Logic.get().whenDisconnectedFromSim();
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
            sender.requestDataOnSimObjectType(Definition.AircraftState, Definition.AircraftState, 0, SimObjectType.USER);
        } catch (final IOException ex) {
            log.error("Error while reading", ex);
        }
    }

    public void handleSimObjectType(SimConnect sender, RecvSimObjectDataByType e) {
        if (e.getRequestID() == Definition.AircraftState.ordinal()) {
            Logic.get().whenAircraftStateReceived(AircraftStateDefinition.from(e));
        }
    }

    @Override
    public void handleSystemState(SimConnect simConnect, RecvSystemState recvSystemState) {
        log.info("recvSystemState { " + recvSystemState.getRequestID() + ", " + recvSystemState.getDataInteger() + ", " + recvSystemState.getDataFloat() + ", " + recvSystemState.getDataString() + " }");
    }

    @Override
    public void handleException(SimConnect simConnect, RecvException e) {
        log.error("recvException = " + e + ", " + e.getException());
    }

    public void moveAircraft(final MoveAircraftDefinition moveAircraft) {
        moveAircraft.apply(simConnect);
    }

    public void setFuelQuantities(final SetFuelQuantitiesDefinition setFuelQuantities) {
        setFuelQuantities.apply(simConnect);
    }
}
