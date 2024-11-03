package net.simforge.fstracker3;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
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
                .forEach(s -> {
                    System.out.println();
                    System.out.println(s);
                    System.out.println();

                    final TrackAnalyzer1.FlightInfo flightInfo = TrackAnalyzer1.recognizeFlight(s, segments);
                    if (flightInfo == null) {
                        return;
                    }
                    TrackAnalyzer1.printFlightInfo(flightInfo);

                    final FSLogRecord awsRecord = convertFlightInfoIntoAWSRecord(flightInfo);

                    AmazonDynamoDB amazonDynamoDB = DynamoDB.get();
                    DynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDB);
                    String flightRecordId = awsRecord.getBeginningDT();
                    FSLogRecord existingAwsRecord = mapper.load(FSLogRecord.class, DynamoDB.getUserId(), flightRecordId);

                    if (existingAwsRecord != null) {
                        System.out.println("  <flight record already logged>  ");
                        return;
                    }

                    if (TrackStorage.isFlightRecordSkipped(flightRecordId)) {
                        System.out.println("  <flight record NOT logged however it was SKIPPED previously>  ");
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
                        TrackStorage.markFlightRecordSkipped(flightRecordId);
                        System.out.println("  !flight record SKIPPED and will be skipped in future!  ");
                        return;
                    }

                    mapper.save(awsRecord);
                    System.out.println("   !flight record just LOGGED!  ");
                });
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

    static int readSingleCharacter() {
        try {
            int input = System.in.read();
            if (input == '\n') {
                return 0;
            }
            return input;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
