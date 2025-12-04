package net.simforge.parkedaircraft2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import flightsim.simconnect.SimConnect;
import net.simforge.commons.io.IOHelper;
import net.simforge.commons.misc.Geo;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Logic {
    public static final double SAVED_POSITION_DELTA = 0.001;
    private static Logic logic;

    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private volatile boolean stopped = false;
    private State state = State.brandNew();

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
        final Thread thread = new Thread(() -> {
            while (!stopped) {
                final Runnable action;
                try {
                    action = queue.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException("interrupted", e);
                }

                synchronized (Logic.this) {
                    action.run();
                }
            }
        }, "LogicThread");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized State getState() {
        return state;
    }

    public void whenConnectedToSim() {
        queue.add(() -> {
            state = state.setSimStatus(SimStatus.MainScreen);
        });
    }

    public void whenDisconnectedFromSim() {
        queue.add(() -> {
            state = state.setSimStatus(SimStatus.NotStarted)
                    .setTrackingState(null)
                    .setSavedAircraftToRestore(null);
        });
    }

    public void whenAircraftStateReceived(final Mk1AircraftStateDefinition aircraftState) {
        queue.add(() -> {
            SimStatus newSimStatus = state.simStatus;
            TrackingState newTrackingState = new TrackingState(aircraftState);
            RestorationStatus newRestorationStatus = state.restorationStatus;
            SavedAircraft newSavedAircraftToRestore = state.savedAircraftToRestore;

            if (state.trackingState == null) {
                if (newTrackingState.inSimulation) {
                    newSavedAircraftToRestore = loadIfExists(newTrackingState.title);
                    newRestorationStatus = newSavedAircraftToRestore != null ? RestorationStatus.WaitForUserConfirmation : RestorationStatus.NothingToRestore;
                    newSimStatus = SimStatus.FullyReady;
                } else {
                    newSavedAircraftToRestore = loadIfExists(newTrackingState.title);
                    newRestorationStatus = newSavedAircraftToRestore != null ? RestorationStatus.WaitForSimReady : RestorationStatus.NothingToRestore;
                    newSimStatus = SimStatus.MainScreen;
                }
            } else if (state.trackingState.inSimulation != newTrackingState.inSimulation) {
                if (newTrackingState.inSimulation) {
                    newSavedAircraftToRestore = loadIfExists(newTrackingState.title);
                    newRestorationStatus = newSavedAircraftToRestore != null ? RestorationStatus.WaitForSimReady : RestorationStatus.NothingToRestore;
                    newSimStatus = SimStatus.Loading;
                } else { // user quits from simulation
                    if (state.restorationStatus == RestorationStatus.NothingToRestore) {
                        final SavedAircraft savedAircraftToSave = SavedAircraft.from(state.trackingState);
                        save(savedAircraftToSave);
                    }

                    newSavedAircraftToRestore = loadIfExists(newTrackingState.title);
                    newRestorationStatus = RestorationStatus.WaitForSimReady;
                    newSimStatus = SimStatus.MainScreen;
                }
            } else { // currentState.inSimulation == newTrackingState.inSimulation
                if (newTrackingState.inSimulation) {
                    if (state.savedAircraftToRestore != null) {
                        switch (state.simStatus) {
                            case Loading -> {
                                if (newTrackingState.aircraft.groundspeed > 0) {
                                    newSimStatus = SimStatus.ReadyToFly;
                                }
                            }
                            case ReadyToFly -> {
                                if (newTrackingState.aircraft.groundspeed == 0) {
                                    newSimStatus = SimStatus.FullyReady;
                                    newRestorationStatus = RestorationStatus.WaitForUserConfirmation;
                                    //newUserConfirmationInitiated = System.currentTimeMillis();
                                    // todo ak0 bring to front
                                }
                            }
                        }
                    }

                    // todo ak0 if aircraft just parked and engine off, then save it

                } else { // outside of simulation
                    if (!state.trackingState.title.equals(newTrackingState.title)) {
                        newSavedAircraftToRestore = loadIfExists(newTrackingState.title);
                    }
                    newSimStatus = SimStatus.MainScreen;
                }
            }

            state = state.setSimStatus(newSimStatus)
                    .setTrackingState(newTrackingState)
                    .setRestorationStatus(newRestorationStatus)
                    .setSavedAircraftToRestore(newSavedAircraftToRestore);
        });
    }

    public void whenUserRestores() {
        queue.add(() -> {
            // todo ak0 checks
            SimWorker.get().moveAircraft(
                    new MoveAircraftDefinition(
                            state.savedAircraftToRestore.latitude,
                            state.savedAircraftToRestore.longitude,
                            state.savedAircraftToRestore.altitude + 1,
                            state.savedAircraftToRestore.heading
                    ));

            state = state.setRestorationStatus(RestorationStatus.NothingToRestore)
                    .setSavedAircraftToRestore(null);
        });
    }

    public void whenUserCancelsRestoration() {
        queue.add(() -> {
            state = state.setRestorationStatus(RestorationStatus.NothingToRestore)
                    .setSavedAircraftToRestore(null);
        });
    }

    public enum SimStatus {
        NotStarted, MainScreen, Loading, ReadyToFly, FullyReady
    }

    public enum RestorationStatus {
        NothingToRestore, WaitForSimReady, WaitForUserConfirmation
    }

    public static class State {
        private final SimStatus simStatus;
        private final TrackingState trackingState;
        private final RestorationStatus restorationStatus;
        private final SavedAircraft savedAircraftToRestore;
        private final long userConfirmationInitiated = 0; // todo ak1

        private State() {
            this.simStatus = SimStatus.NotStarted;
            this.trackingState = null;
            this.restorationStatus = RestorationStatus.NothingToRestore;
            this.savedAircraftToRestore = null;
        }

        private State(final SimStatus simStatus,
                      final TrackingState trackingState,
                      final RestorationStatus restorationStatus,
                      final SavedAircraft savedAircraftToRestore) {
            this.simStatus = simStatus;
            this.trackingState = trackingState;
            this.restorationStatus = restorationStatus;
            this.savedAircraftToRestore = savedAircraftToRestore;
        }

        public static State brandNew() {
            return new State();
        }

        public SimStatus getSimStatus() {
            return simStatus;
        }

        public State setSimStatus(final SimStatus simStatus) {
            return new State(simStatus, trackingState, restorationStatus, savedAircraftToRestore);
        }

        public TrackingState getTrackingState() {
            return trackingState;
        }

        public State setTrackingState(TrackingState trackingState) {
            return new State(simStatus, trackingState, restorationStatus, savedAircraftToRestore);
        }

        public RestorationStatus getRestorationStatus() {
            return restorationStatus;
        }

        public State setRestorationStatus(RestorationStatus restorationStatus) {
            return new State(simStatus, trackingState, restorationStatus, savedAircraftToRestore);
        }

        public SavedAircraft getSavedAircraftToRestore() {
            return savedAircraftToRestore;
        }

        public State setSavedAircraftToRestore(SavedAircraft savedAircraftToRestore) {
            return new State(simStatus, trackingState, restorationStatus, savedAircraftToRestore);
        }

        public double getDistanceToSavedPosition() {
            if (savedAircraftToRestore == null || !trackingState.inSimulation) {
                return Double.NaN;
            }

            return Geo.distance(
                    Geo.coords(savedAircraftToRestore.latitude, savedAircraftToRestore.longitude),
                    Geo.coords(trackingState.aircraft.latitude, trackingState.aircraft.longitude));
        }

        public boolean isAircraftAtToSavedPosition() {
            return getDistanceToSavedPosition() < SAVED_POSITION_DELTA;
        }
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
