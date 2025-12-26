package net.simforge.fstracker3;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import net.simforge.commons.misc.JavaTime;
import net.simforge.fstracker3.dynamodb.DynamoDB;
import net.simforge.fstracker3.dynamodb.FSLogRecord;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class TrackAnalyzer1AWS {
    public static void main(String[] args) throws IOException {
        final List<TrackEntry> trackData = TrackAnalyzer1.loadTrackDataAfter(LocalDateTime.of(2024, Month.JULY, 10, 0, 0));
        System.out.println(trackData.size());

        final List<TrackAnalyzer1.SegmentInfo> segments = TrackAnalyzer1.convertTrackDataToSegments(trackData);

        System.out.println();
        System.out.println();
        System.out.println();
        segments.stream()
                .filter(segmentInfo -> segmentInfo.isTakeoffLanding() && !segmentInfo.isBouncing())
                .forEach(s -> processSegment(s, segments));
    }

    private static void processSegment(TrackAnalyzer1.SegmentInfo s, List<TrackAnalyzer1.SegmentInfo> segments) {
        System.out.println();
        System.out.println(s);
        System.out.println();

        final TrackAnalyzer1.FlightInfo flightInfo = TrackAnalyzer1.recognizeFlight(s, segments);
        if (flightInfo == null) {
            return;
        }
        TrackAnalyzer1.printFlightInfo(flightInfo);

        final FSLogRecord ourRecord = convertFlightInfoIntoAWSRecord(flightInfo);
        final String flightRecordId = ourRecord.getBeginningDT();

        final LocalStorage.Record localRecord = LocalStorage.load(flightRecordId);
        if (localRecord != null) {
            switch (localRecord.getStatus()) {
                case Logged -> System.out.println("  <flight record is locally known as logged>");
                case Skipped -> System.out.println("  <flight record NOT logged however it was SKIPPED previously>");
            }
            return;
        }

        final FSLogRecord storedRecord = loadAwsRecord(flightRecordId);
        if (storedRecord != null) {
            LocalStorage.saveAsLogged(flightRecordId);
            System.out.println("  <flight record is stored in database, loaded and marked locally as logged>");
            return;
        }

        boolean decisionToSkip = false;
        boolean decisionToLog = false;
        while (!decisionToSkip && !decisionToLog) {
            System.out.print("[L]og or [S]kip the flight record: ");
            int choice = new Scanner(System.in).nextLine().charAt(0);
            decisionToLog = choice == 'l' || choice == 'L';
            decisionToSkip = choice == 's' || choice == 'S';
        }

        if (decisionToSkip) {
            LocalStorage.saveAsSkipped(flightRecordId);
            System.out.println("  !flight record SKIPPED and will be skipped in future!  ");
            return;
        }

        saveToAws(ourRecord);
        LocalStorage.saveAsLogged(flightRecordId);
        System.out.println("   !flight record just LOGGED!  ");
    }

    private static DynamoDBMapper awsMapper;

    private static void initAwsMapper() {
        if (awsMapper == null) {
            awsMapper = new DynamoDBMapper(DynamoDB.get());
        }
    }

    private static FSLogRecord loadAwsRecord(final String flightRecordId) {
        initAwsMapper();
        return awsMapper.load(FSLogRecord.class, DynamoDB.getUserId(), flightRecordId);
    }

    private static void saveToAws(final FSLogRecord record) {
        initAwsMapper();
        awsMapper.save(record);
    }

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static FSLogRecord convertFlightInfoIntoAWSRecord(final TrackAnalyzer1.FlightInfo flight) {
        FSLogRecord record = new FSLogRecord();
        record.setUserID(DynamoDB.getUserId());
        record.setBeginningDT(dateTimeFormatter.format(flight.getTimeOut()));
        record.setRecordID(UUID.randomUUID().toString());
        record.setDate(dateFormatter.format(flight.getTimeOut()));
        record.setType("flight");
        record.setComment("TODO - comment, tracked by fstracker"); // todo
        record.setRemarks("TODO - remarks, tracked by fstracker"); // todo

        record.setFlight(new FSLogRecord.Flight());
        record.getFlight().setDeparture(flight.getDepartureIcao());
        record.getFlight().setDestination(flight.getDestinationIcao());

        record.getFlight().setTimeOut(JavaTime.toHhmm(flight.getTimeOut().toLocalTime()));
        record.getFlight().setTimeOff(JavaTime.toHhmm(flight.getTimeOff().toLocalTime()));
        record.getFlight().setTimeOn(JavaTime.toHhmm(flight.getTimeOn().toLocalTime()));
        record.getFlight().setTimeIn(JavaTime.toHhmm(flight.getTimeIn().toLocalTime()));

        record.getFlight().setDistance((int) Math.round(flight.getTotalDistance()));
        record.getFlight().setTotalTime(JavaTime.toHhmm(flight.getTotalTime()));
        record.getFlight().setAirTime(JavaTime.toHhmm(flight.getAirTime()));

        record.getFlight().setCallsign(null);
        record.getFlight().setFlightNumber(null);
        record.getFlight().setAircraftType(null);
        record.getFlight().setAircraftRegistration(null);

        return record;
    }
}
