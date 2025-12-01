package net.simforge.parkedaircraft2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import flightsim.simconnect.SimConnect;
import net.simforge.commons.io.IOHelper;
import net.simforge.commons.misc.Geo;

import java.io.File;
import java.io.IOException;

public class Logic {
    private static Logic logic;

    private SimStartupSequence simStartupSequence = SimStartupSequence.NotStarted;
    private TrackingState currentState;
    private SavedAircraft statusToRestore;

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

    public SimStartupSequence getSimStartupSequence() {
        return simStartupSequence;
    }

    public TrackingState getCurrentState() {
        return currentState;
    }

    public Object getStatusToRestore() {
        return statusToRestore;
    }

    public void newAircraftStateReceived(final Mk1AircraftStateDefinition aircraftState) {
        final TrackingState lastTrackingState = new TrackingState(aircraftState);

        if (currentState == null) {
            if (lastTrackingState.inSimulation) {
                // todo ak1 statusToRestore = loadIfExists(lastTrackingState.title); it is not required?
                simStartupSequence = SimStartupSequence.FullyReady;
            } else {
                statusToRestore = loadIfExists(lastTrackingState.title);
                simStartupSequence = SimStartupSequence.MainScreen;
            }
            currentState = lastTrackingState;
        } else if (currentState.inSimulation != lastTrackingState.inSimulation) {
            if (lastTrackingState.inSimulation) {
                statusToRestore = loadIfExists(lastTrackingState.title);
                simStartupSequence = SimStartupSequence.Loading;
            } else {
                final SavedAircraft savedAircraftToSave = SavedAircraft.from(currentState);
                save(savedAircraftToSave);
                simStartupSequence = SimStartupSequence.MainScreen;
                statusToRestore = loadIfExists(lastTrackingState.title);
            }
            currentState = lastTrackingState;
        } else { // currentState.inSimulation == lastTrackingState.inSimulation
            if (lastTrackingState.inSimulation) {
                if (statusToRestore != null) {
                    switch (simStartupSequence) {
                        case Loading -> {
                            if (lastTrackingState.aircraft.groundspeed > 0) {
                                simStartupSequence = SimStartupSequence.ReadyToFly;
                            }
                        }
                        case ReadyToFly -> {
                            if (lastTrackingState.aircraft.groundspeed == 0) {
                                simStartupSequence = SimStartupSequence.FullyReady;
                            }
                        }
                    }
                    System.out.println("  +++ Sim " + simStartupSequence);

                    if (simStartupSequence == SimStartupSequence.FullyReady) {
                        final double distance = Geo.distance(
                                Geo.coords(statusToRestore.latitude, statusToRestore.longitude),
                                Geo.coords(lastTrackingState.aircraft.latitude, lastTrackingState.aircraft.longitude));
                        if (distance < 0.001) {
                            System.out.println("                          Aircraft is at LAST SEEN POSITION");
                        } else {
                            System.out.println("                          Aircraft is " + distance + " nm AWAY from LAST SEEN POSITION");
                            SimWorker.get().moveAircraft(
                                    new MoveAircraftDefinition(
                                            statusToRestore.latitude,
                                            statusToRestore.longitude,
                                            statusToRestore.altitude + 1,
                                            statusToRestore.heading
                                    ));
                        }
                        statusToRestore = null;
                    }
                }

                // todo ak0 if aircraft just parked and engine off, then save it

            } else { // outside of simulation
                if (!currentState.title.equals(lastTrackingState.title)) {
                    statusToRestore = loadIfExists(lastTrackingState.title);
                }
                simStartupSequence = SimStartupSequence.MainScreen;
            }

            currentState = lastTrackingState;
        }
    }

    public enum SimStartupSequence {
        NotStarted, MainScreen, Loading, ReadyToFly, FullyReady
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

    private static void save(final SavedAircraft savedAircraft) {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final String json = gson.toJson(savedAircraft);
        try {
            final File file = new File("Parked Aircraft/" + savedAircraft.title + ".json");
            file.getParentFile().mkdirs();
            IOHelper.saveFile(file, json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static SavedAircraft loadIfExists(String title) {
        final File file = new File("Parked Aircraft/" + title + ".json");
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
