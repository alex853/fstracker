package net.simforge.fstracker2;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectDataType;
import flightsim.simconnect.SimObjectType;
import flightsim.simconnect.config.ConfigurationNotFoundException;
import flightsim.simconnect.recv.*;
import net.simforge.commons.io.IOHelper;
import net.simforge.commons.misc.Geo;
import net.simforge.commons.misc.JavaTime;
import net.simforge.commons.misc.Str;
import net.simforge.refdata.airports.Airport;
import net.simforge.refdata.airports.Airports;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class FSTracker2 implements EventHandler, OpenHandler, QuitHandler, SimObjectDataTypeHandler {
    private static final int DEFINITION_0 = 0;
    private static final int DEFINITION_1 = 1;

    private static final int REQUEST_1 = 1;

    private static final DecimalFormat coordFormat = new DecimalFormat("##0.000000");
    private static final DecimalFormat d1 = new DecimalFormat("0.0");

    private static final Airports airports = Airports.get();

    private Segment currentSegment;
    private SimState previousSimState;
    private SimState currentSimState;

    public static void main(String[] args) throws IOException, ConfigurationNotFoundException, InterruptedException {
        SimConnect sc = new SimConnect("FSTracker2", 0);

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

        FSTracker2 fst2 = new FSTracker2();

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

            previousSimState = currentSimState;
            currentSimState = state;

            analyzeSegment();
        }

        printState();
    }

    private void analyzeSegment() {
        Geo.Coords currentCoords = Geo.coords(currentSimState.getLatitude(), currentSimState.getLongitude());

        boolean outsideSimulation = Geo.distance(currentCoords, Geo.coords(0, 0)) < 1;

        if (previousSimState == null) {
            currentSegment = startSegment(
                    detectSegmentType(currentSimState.isOnGround(), outsideSimulation),
                    currentSimState.getTitle(),
                    EdgeType.RecordingStart,
                    currentCoords);
            return;
        }

        Geo.Coords previousCoords = Geo.coords(previousSimState.getLatitude(), previousSimState.getLongitude());

        int timeChange = 1000;
        double distanceChange = Geo.distance(
                currentCoords,
                previousCoords);
        boolean jumpDetected = distanceChange > 4000.0 / 3600.0;


        if (jumpDetected) {
            finishSegment(currentSegment, EdgeType.Jump, currentCoords);
            printSegment(currentSegment);
            saveSegment(currentSegment);

            currentSegment = startSegment(
                    detectSegmentType(currentSimState.isOnGround(), outsideSimulation),
                    currentSegment.getAircraftTitle(),
                    EdgeType.Jump,
                    currentCoords);
            return;
        }

        boolean takeoff = currentSimState.isInAir() && previousSimState.isOnGround();
        boolean landing = currentSimState.isOnGround() && previousSimState.isInAir();

        if (!outsideSimulation) {
            if (takeoff) {
                finishSegment(currentSegment, EdgeType.Takeoff, currentCoords);
                printSegment(currentSegment);
                saveSegment(currentSegment);

                currentSegment = startSegment(
                        SegmentType.InAir,
                        currentSegment.getAircraftTitle(),
                        EdgeType.Takeoff,
                        currentCoords);
                currentSegment.setRecordedTimeMs(timeChange);
                currentSegment.setRecordedDistanceNm(distanceChange);
                return;
            }

            if (landing) {
                finishSegment(currentSegment, EdgeType.Landing, currentCoords);
                printSegment(currentSegment);
                saveSegment(currentSegment);

                currentSegment = startSegment(
                        SegmentType.OnGround,
                        currentSegment.getAircraftTitle(),
                        EdgeType.Landing,
                        currentCoords);
                currentSegment.setRecordedTimeMs(timeChange);
                currentSegment.setRecordedDistanceNm(distanceChange);
                return;
            }
        }

        currentSegment.setRecordedTimeMs(currentSegment.getRecordedTimeMs() + timeChange);
        currentSegment.setRecordedDistanceNm(currentSegment.getRecordedDistanceNm() + distanceChange);
    }

    private SegmentType detectSegmentType(final boolean onGround, final boolean outsideSimulation) {
        if (outsideSimulation) {
            return SegmentType.OutsideSimulation;
        }

        return onGround
                ? SegmentType.OnGround
                : SegmentType.InAir;
    }

    private void saveSegment(final Segment segment) {
        final String filename = String.format("Segments/%s/Segment___%s___%s___%s-%s___%s-%s.txt",
                JavaTime.yMd.format(segment.getStartDt()).replace(':', '-').replace(' ', '_'),
                JavaTime.yMdHms.format(segment.getStartDt()).replace(':', '-').replace(' ', '_'),
                segment.getType(),
                segment.getStartIcao(),
                segment.getStartType(),
                segment.getFinishIcao(),
                segment.getFinishType());

        final String content = String.format(
                "Aircraft             %s\n" +
                "Type                 %s\n" +
                "Route                %s - %s\n" +
                "Distance, nm         %s\n" +
                "Time                 %s\n" +
                "\n" +
                "Start type           %s\n" +
                "Start date/time      %s\n" +
                "Finish type          %s\n" +
                "Finish date/time     %s\n",
                segment.getAircraftTitle(),
                segment.getType(),
                segment.getStartIcao(),
                segment.getFinishIcao(),
                d1.format(segment.getRecordedDistanceNm()),
                JavaTime.toHhmm(Duration.of(segment.getRecordedTimeMs(), ChronoUnit.MILLIS)),
                segment.getStartType(),
                JavaTime.yMdHms.format(segment.getStartDt()),
                segment.getFinishType(),
                JavaTime.yMdHms.format(segment.getFinishDt()));

        File file = new File(filename);
        file.getParentFile().mkdirs();

        try {
            IOHelper.saveFile(file, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Segment startSegment(SegmentType type, String aircraftTitle, EdgeType startType, Geo.Coords coords) {
        Segment segment = new Segment();
        segment.setType(type);
        segment.setAircraftTitle(aircraftTitle);
        segment.setStartType(startType);
        segment.setStartDt(JavaTime.nowUtc());
        segment.setStartIcao(findIcao(coords));
        return segment;
    }

    private void finishSegment(Segment segment, EdgeType finishType, Geo.Coords coords) {
        segment.setFinishType(finishType);
        segment.setFinishDt(JavaTime.nowUtc());
        segment.setFinishIcao(findIcao(coords));

        JavaTime.nowUtc();
    }

    private String findIcao(Geo.Coords coords) {
        Airport nearest = airports.findNearest(coords);
        if (nearest == null) {
            return null;
        }
        return nearest.getIcao();
    }

    private void printSegment(Segment segment) {
        clearLine();
        _printSegment(segment);
        System.out.println();
    }

    private void printState() {
        clearLine();
        _printSegment(currentSegment);
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

    private static void _printSegment(Segment segment) {
        System.out.printf("%s [%s/%s - %s/%s] Time %s mins, Dist %s nm",
                segment.getType(),
                segment.getStartIcao(),
                segment.getStartType(),
                segment.getFinishIcao(),
                segment.getFinishType(),
                d1.format(segment.getRecordedTimeMs() / 60000.0),
                d1.format(segment.getRecordedDistanceNm()));
    }

    private void clearLine() {
        System.out.print("                                                                                                                                       ");
        System.out.print("\r");
    }
}
