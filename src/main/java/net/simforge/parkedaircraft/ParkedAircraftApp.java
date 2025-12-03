package net.simforge.parkedaircraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import net.simforge.commons.io.IOHelper;
import net.simforge.commons.misc.Geo;
import net.simforge.fsdatafeeder.FSDataFeeder;
import net.simforge.fsdatafeeder.SimState;
import net.simforge.fsdatafeeder.SimStateConsumer;
import net.simforge.fsdatafeeder.SimStateField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ParkedAircraftApp {
    private static final Logger log = LoggerFactory.getLogger(ParkedAircraftApp.class);
    private static final SimStateField[] fields = {
            SimStateField.Title,
            SimStateField.ATC_Type,
            SimStateField.ATC_Model,
            SimStateField.Plane_Latitude,
            SimStateField.Plane_Longitude,
            SimStateField.Plane_Altitude,
            SimStateField.Plane_Heading_Degrees_True,
            SimStateField.Sim_On_Ground,
            SimStateField.Ground_Velocity,
            SimStateField.Is_User_Sim,
            SimStateField.Plane_In_Parking_State,
            SimStateField.Brake_Parking_Position,
            SimStateField.Eng_Combustion_1
    };

    public static void main(String[] args) {
        log.info("Starting parked aircraft tracker");

        final TrackingState[] currentState = new TrackingState[1];
        final SavedAircraft[] savedAircraft = new SavedAircraft[1];
        final SimStartupSequence[] simStartupSequence = new SimStartupSequence[1];
        final boolean positionRegistered[] = new boolean[1];

        final int MOVE_AIRCRAFT = 10;

        final SimStateConsumer consumer = (simState, simConnect) -> {

            if (!positionRegistered[0]) {
                try {
                    simConnect.addToDataDefinition(MOVE_AIRCRAFT, SimStateField.Plane_Latitude.getTitle(), SimStateField.Plane_Latitude.getUnitsName(), SimStateField.Plane_Latitude.getDataType());
                    simConnect.addToDataDefinition(MOVE_AIRCRAFT, SimStateField.Plane_Longitude.getTitle(), SimStateField.Plane_Longitude.getUnitsName(), SimStateField.Plane_Longitude.getDataType());
                    simConnect.addToDataDefinition(MOVE_AIRCRAFT, SimStateField.Plane_Altitude.getTitle(), SimStateField.Plane_Altitude.getUnitsName(), SimStateField.Plane_Altitude.getDataType());
                    simConnect.addToDataDefinition(MOVE_AIRCRAFT, SimStateField.Plane_Heading_Degrees_True.getTitle(), SimStateField.Plane_Heading_Degrees_True.getUnitsName(), SimStateField.Plane_Heading_Degrees_True.getDataType());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                positionRegistered[0] = true;
            }

            final TrackingState instantTrackingState = TrackingState.from(AircraftState.from(simState));

            if (currentState[0] == null) {
                currentState[0] = instantTrackingState;

                System.out.println("  !!! App started         Aircraft '" + instantTrackingState.title + "'");

                if (instantTrackingState.inSimulation) {
                    savedAircraft[0] = loadIfExists(instantTrackingState.title);
                    if (savedAircraft[0] == null) {
                        System.out.println("                          No aircraft state found");
                    } else {
                        System.out.println("                          Aircraft state LOADED");
                    }
                    simStartupSequence[0] = SimStartupSequence.FullyReady;
                }

                return;
            }

            if (instantTrackingState.inSimulation != currentState[0].inSimulation) {
                if (instantTrackingState.inSimulation) {

                    System.out.println("  >>> Simulator STARTED   Aircraft '" + AircraftState.from(simState).title + "'");
                    savedAircraft[0] = loadIfExists(instantTrackingState.title);
                    if (savedAircraft[0] == null) {
                        System.out.println("                          No aircraft state found");
                    } else {
                        System.out.println("                          Aircraft state LOADED");
                    }
                    simStartupSequence[0] = SimStartupSequence.Loading;

                } else {

                    final SavedAircraft savedAircraftToSave = SavedAircraft.from(currentState[0]);
                    save(savedAircraftToSave);

                    System.out.println("  <<< Simulator stopped   Aircraft '" + savedAircraftToSave.title + "' saved");

                }
            } else {
                if (instantTrackingState.inSimulation) {

                    if (savedAircraft[0] != null) {
                        switch (simStartupSequence[0]) {
                            case Loading:
                                if (AircraftState.from(simState).groundspeed > 0) {
                                    simStartupSequence[0] = SimStartupSequence.ReadyToFly;
                                }
                                break;
                            case ReadyToFly:
                                if (AircraftState.from(simState).groundspeed == 0) {
                                    simStartupSequence[0] = SimStartupSequence.FullyReady;
                                }
                                break;
                        }
                        System.out.println("  +++ Sim " + simStartupSequence[0]);

                        if (simStartupSequence[0] == SimStartupSequence.FullyReady) {
                            final double distance = Geo.distance(
                                    Geo.coords(savedAircraft[0].latitude, savedAircraft[0].longitude),
                                    Geo.coords(instantTrackingState.aircraft.latitude, instantTrackingState.aircraft.longitude));
                            if (distance < 0.001) {
                                System.out.println("                          Aircraft is at LAST SEEN POSITION");
                            } else {
                                System.out.println("                          Aircraft is " + distance + " nm AWAY from LAST SEEN POSITION");
                                try {
                                    final byte[] bytes = new byte[32];
                                    final ByteBuffer buf = ByteBuffer.wrap(bytes);
                                    buf.order(ByteOrder.LITTLE_ENDIAN);
                                    buf.putDouble(savedAircraft[0].latitude);
                                    buf.putDouble(savedAircraft[0].longitude);
                                    buf.putDouble(savedAircraft[0].altitude + 1);
                                    buf.putDouble(savedAircraft[0].heading);
                                    simConnect.setDataOnSimObject(MOVE_AIRCRAFT, 0, false, 1, bytes);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            savedAircraft[0] = null;
                        }
                    }
                } else { // outside of simulation

                    if (!currentState[0].title.equals(instantTrackingState.title)) {
                        System.out.println("  !!! Aircraft CHANGED    Aircraft '" + instantTrackingState.title + "'");
                    }

                }
            }

            currentState[0] = instantTrackingState;

        };

        final FSDataFeeder feeder = new FSDataFeeder(consumer, fields);

        while (true) {
            try {
                feeder.connectAndRunCollectionCycle();
            } catch (final IOException e) {
                log.warn("Connection unsuccessful: " + e.getMessage());
            } catch (final ConfigurationNotFoundException e) {
                log.error("Configuration not found", e);
                break;
            } catch (final InterruptedException e) {
                break;
            }

            try {
                //noinspection BusyWait
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private enum SimStartupSequence {
        Loading, ReadyToFly, FullyReady
    }

    private static boolean isOutsideSimulation(double latitude, double longitude) {
        return Geo.distance(Geo.coords(latitude, longitude), Geo.coords(0, 0)) < 1;
    }

    private static class TrackingState {
        final boolean inSimulation;
        final String title;
        final AircraftState aircraft;

        private TrackingState(AircraftState aircraftState) {
            this.inSimulation = !isOutsideSimulation(aircraftState.latitude, aircraftState.longitude);
            this.title = aircraftState.title;
            this.aircraft = aircraftState;
        }

        public static TrackingState from(AircraftState aircraftState) {
            return new TrackingState(aircraftState);
        }
    }

    private static class AircraftState {
        final String title;
        final double latitude;
        final double longitude;
        final double altitude;
        final double heading;
        final int onGround;
        final double groundspeed;
        final int parkingBrake;
        final int eng1running;

        private AircraftState(String title, double latitude, double longitude, double altitude, double heading, int onGround, double groundspeed, int parkingBrake, int eng1running) {
            this.title = title;
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
            this.heading = heading;
            this.onGround = onGround;
            this.groundspeed = groundspeed;
            this.parkingBrake = parkingBrake;
            this.eng1running = eng1running;
        }

        public static AircraftState from(SimState simState) {
            return new AircraftState(
                    simState.getString(SimStateField.Title),
                    simState.getDouble(SimStateField.Plane_Latitude),
                    simState.getDouble(SimStateField.Plane_Longitude),
                    simState.getDouble(SimStateField.Plane_Altitude),
                    simState.getDouble(SimStateField.Plane_Heading_Degrees_True),
                    simState.getInt(SimStateField.Sim_On_Ground),
                    simState.getDouble(SimStateField.Ground_Velocity),
                    simState.getInt(SimStateField.Brake_Parking_Position),
                    simState.getInt(SimStateField.Eng_Combustion_1));
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
