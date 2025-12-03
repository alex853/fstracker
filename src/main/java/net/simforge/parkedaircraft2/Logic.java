package net.simforge.parkedaircraft2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import flightsim.simconnect.SimConnect;
import net.simforge.commons.io.IOHelper;
import net.simforge.commons.misc.Geo;

import java.io.File;
import java.io.IOException;

public class Logic {
    public static final double SAVED_POSITION_DELTA = 0.001;
    private static Logic logic;

    private volatile SimStatus simStatus = SimStatus.NotStarted;
    private volatile TrackingState currentState;
    private volatile RestorationStatus restorationStatus = RestorationStatus.NothingToRestore;
    private volatile SavedAircraft savedAircraftToRestore;
    private long userConfirmationInitiated;

    private Logic() {

    }

    public static Logic get() {
        synchronized (SimConnect.class) {
            if (logic == null) {
                logic = new Logic();
            }
        }
        return logic;
    }

    public void start() {
        // noop
    }

    public SimStatus getSimStatus() {
        return simStatus;
    }

    public TrackingState getCurrentState() {
        return currentState;
    }

    public RestorationStatus getRestorationStatus() {
        return restorationStatus;
    }

    public synchronized void whenConnectedToSim() {
        simStatus = SimStatus.MainScreen;
    }

    public synchronized void whenDisconnectedFromSim() {
        simStatus = SimStatus.NotStarted;
        currentState = null;
        savedAircraftToRestore = null;
    }

    public synchronized void whenAircraftStateReceived(final Mk1AircraftStateDefinition aircraftState) {
        final TrackingState lastTrackingState = new TrackingState(aircraftState);

        if (currentState == null) {
            if (lastTrackingState.inSimulation) {
                savedAircraftToRestore = loadIfExists(lastTrackingState.title);
                restorationStatus = savedAircraftToRestore != null ? RestorationStatus.WaitForUserConfirmation : RestorationStatus.NothingToRestore;
                simStatus = SimStatus.FullyReady;
            } else {
                savedAircraftToRestore = loadIfExists(lastTrackingState.title);
                restorationStatus = savedAircraftToRestore != null ? RestorationStatus.WaitForSimReady : RestorationStatus.NothingToRestore;
                simStatus = SimStatus.MainScreen;
            }
            currentState = lastTrackingState;
        } else if (currentState.inSimulation != lastTrackingState.inSimulation) {
            if (lastTrackingState.inSimulation) {
                savedAircraftToRestore = loadIfExists(lastTrackingState.title);
                restorationStatus = savedAircraftToRestore != null ? RestorationStatus.WaitForSimReady : RestorationStatus.NothingToRestore;
                simStatus = SimStatus.Loading;
            } else {
                if (restorationStatus != RestorationStatus.NothingToRestore) {
                    final SavedAircraft savedAircraftToSave = SavedAircraft.from(currentState);
                    save(savedAircraftToSave);
                    simStatus = SimStatus.MainScreen;

                    savedAircraftToRestore = loadIfExists(lastTrackingState.title);
                    restorationStatus = RestorationStatus.WaitForSimReady;
                }
            }
            currentState = lastTrackingState;
        } else { // currentState.inSimulation == lastTrackingState.inSimulation
            if (lastTrackingState.inSimulation) {
                if (savedAircraftToRestore != null) {
                    switch (simStatus) {
                        case Loading -> {
                            if (lastTrackingState.aircraft.groundspeed > 0) {
                                simStatus = SimStatus.ReadyToFly;
                            }
                        }
                        case ReadyToFly -> {
                            if (lastTrackingState.aircraft.groundspeed == 0) {
                                simStatus = SimStatus.FullyReady;
                                restorationStatus = RestorationStatus.WaitForUserConfirmation;
                                userConfirmationInitiated = System.currentTimeMillis();
                                // todo ak0 bring to front
                            }
                        }
                    }
                }

                // todo ak0 if aircraft just parked and engine off, then save it

            } else { // outside of simulation
                if (!currentState.title.equals(lastTrackingState.title)) {
                    savedAircraftToRestore = loadIfExists(lastTrackingState.title);
                }
                simStatus = SimStatus.MainScreen;
            }

            currentState = lastTrackingState;
        }
    }

    public synchronized void whenUserRestores() {
        // todo ak0 checks
        SimWorker.get().moveAircraft(
                new MoveAircraftDefinition(
                        savedAircraftToRestore.latitude,
                        savedAircraftToRestore.longitude,
                        savedAircraftToRestore.altitude + 1,
                        savedAircraftToRestore.heading
                ));

        restorationStatus = RestorationStatus.NothingToRestore;
        savedAircraftToRestore = null;
    }

    public synchronized void whenUserCancelsRestoration() {
        restorationStatus = RestorationStatus.NothingToRestore;
        savedAircraftToRestore = null;
    }

    public synchronized double getDistanceToSavedPosition() {
        if (savedAircraftToRestore == null || !currentState.inSimulation) {
            return Double.NaN;
        }

        return Geo.distance(
                Geo.coords(savedAircraftToRestore.latitude, savedAircraftToRestore.longitude),
                Geo.coords(currentState.aircraft.latitude, currentState.aircraft.longitude));
    }

    public synchronized boolean isAircraftAtToSavedPosition() {
        return getDistanceToSavedPosition() < SAVED_POSITION_DELTA;
    }

    public enum SimStatus {
        NotStarted, MainScreen, Loading, ReadyToFly, FullyReady
    }

    public enum RestorationStatus {
        NothingToRestore, WaitForSimReady, WaitForUserConfirmation
    }

    public static class TrackingState {
        final boolean inSimulation;
        final String title;
        final Mk1AircraftStateDefinition aircraft;

        private TrackingState(final Mk1AircraftStateDefinition aircraftState) {
            this.inSimulation = !isOutsideSimulation(aircraftState.latitude, aircraftState.longitude);
            this.title = aircraftState.title;
            this.aircraft = aircraftState;
        }

        private static boolean isOutsideSimulation(double latitude, double longitude) {
            return Geo.distance(Geo.coords(latitude, longitude), Geo.coords(0, 0)) < 1;
        }
    }

    private static class SavedAircraft {
        final String title;
        final double latitude;
        final double longitude;
        final double altitude;
        final double heading;

        private SavedAircraft(String title, double latitude, double longitude, double altitude, double heading) {
            this.title = title;
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
            this.heading = heading;
        }

        public static SavedAircraft from(final TrackingState trackingState) {
            return new SavedAircraft(trackingState.title, trackingState.aircraft.latitude, trackingState.aircraft.longitude, trackingState.aircraft.altitude, trackingState.aircraft.heading);
        }
    }

    private static File makeFile(final String title) {
        return new File(System.getenv("LOCALAPPDATA") + "/simforge.net/Parked Aircraft/" + title + ".json");
    }

    private static void save(final SavedAircraft savedAircraft) {
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

    private static SavedAircraft loadIfExists(final String title) {
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
        return gson.fromJson(json, SavedAircraft.class);
    }
}
