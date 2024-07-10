package net.simforge.fstracker3.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "fslog-record")
public class FSLogRecord {
    @DynamoDBHashKey(attributeName = "UserID")
    private String userID;
    @DynamoDBRangeKey(attributeName = "BeginningDT")
    private String beginningDT;

    @DynamoDBAttribute(attributeName = "RecordID")
    private String recordID;

    @DynamoDBAttribute(attributeName = "Date")
    private String date;
    @DynamoDBAttribute(attributeName = "Type")
    private String type;

    @DynamoDBAttribute(attributeName = "Comment")
    private String comment;
    @DynamoDBAttribute(attributeName = "Remarks")
    private String remarks;

    @DynamoDBAttribute(attributeName = "Flight")
    private Flight flight;
    @DynamoDBAttribute(attributeName = "Transfer")
    private Transfer transfer;
    @DynamoDBAttribute(attributeName = "Discontinuity")
    private Discontinuity discontinuity;

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getBeginningDT() {
        return beginningDT;
    }

    public void setBeginningDT(String beginningDT) {
        this.beginningDT = beginningDT;
    }

    public String getRecordID() {
        return recordID;
    }

    public void setRecordID(String recordID) {
        this.recordID = recordID;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Flight getFlight() {
        return flight;
    }

    public void setFlight(Flight flight) {
        this.flight = flight;
    }

    public Transfer getTransfer() {
        return transfer;
    }

    public void setTransfer(Transfer transfer) {
        this.transfer = transfer;
    }

    public Discontinuity getDiscontinuity() {
        return discontinuity;
    }

    public void setDiscontinuity(Discontinuity discontinuity) {
        this.discontinuity = discontinuity;
    }

    @DynamoDBDocument
    public static class Flight {
        @DynamoDBAttribute(attributeName = "Departure")
        private String departure;
        @DynamoDBAttribute(attributeName = "Destination")
        private String destination;

        @DynamoDBAttribute(attributeName = "TimeOut")
        private String timeOut;
        @DynamoDBAttribute(attributeName = "TimeOff")
        private String timeOff;
        @DynamoDBAttribute(attributeName = "TimeOn")
        private String timeOn;
        @DynamoDBAttribute(attributeName = "TimeIn")
        private String timeIn;

        @DynamoDBAttribute(attributeName = "Distance")
        private Integer distance;
        @DynamoDBAttribute(attributeName = "TotalTime")
        private String totalTime;
        @DynamoDBAttribute(attributeName = "AirTime")
        private String airTime;

        @DynamoDBAttribute(attributeName = "Callsign")
        private String callsign;
        @DynamoDBAttribute(attributeName = "FlightNumber")
        private String flightNumber;
        @DynamoDBAttribute(attributeName = "AircraftType")
        private String aircraftType;
        @DynamoDBAttribute(attributeName = "AircraftRegistration")
        private String aircraftRegistration;

        public String getDeparture() {
            return departure;
        }

        public void setDeparture(String departure) {
            this.departure = departure;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public String getTimeOut() {
            return timeOut;
        }

        public void setTimeOut(String timeOut) {
            this.timeOut = timeOut;
        }

        public String getTimeOff() {
            return timeOff;
        }

        public void setTimeOff(String timeOff) {
            this.timeOff = timeOff;
        }

        public String getTimeOn() {
            return timeOn;
        }

        public void setTimeOn(String timeOn) {
            this.timeOn = timeOn;
        }

        public String getTimeIn() {
            return timeIn;
        }

        public void setTimeIn(String timeIn) {
            this.timeIn = timeIn;
        }

        public Integer getDistance() {
            return distance;
        }

        public void setDistance(Integer distance) {
            this.distance = distance;
        }

        public String getTotalTime() {
            return totalTime;
        }

        public void setTotalTime(String totalTime) {
            this.totalTime = totalTime;
        }

        public String getAirTime() {
            return airTime;
        }

        public void setAirTime(String airTime) {
            this.airTime = airTime;
        }

        public String getCallsign() {
            return callsign;
        }

        public void setCallsign(String callsign) {
            this.callsign = callsign;
        }

        public String getFlightNumber() {
            return flightNumber;
        }

        public void setFlightNumber(String flightNumber) {
            this.flightNumber = flightNumber;
        }

        public String getAircraftType() {
            return aircraftType;
        }

        public void setAircraftType(String aircraftType) {
            this.aircraftType = aircraftType;
        }

        public String getAircraftRegistration() {
            return aircraftRegistration;
        }

        public void setAircraftRegistration(String aircraftRegistration) {
            this.aircraftRegistration = aircraftRegistration;
        }
    }

    @DynamoDBDocument
    public static class Transfer {
        @DynamoDBAttribute(attributeName = "Departure")
        private String departure;
        @DynamoDBAttribute(attributeName = "Destination")
        private String destination;

        @DynamoDBAttribute(attributeName = "TimeOut")
        private String timeOut;
        @DynamoDBAttribute(attributeName = "TimeIn")
        private String timeIn;

        @DynamoDBAttribute(attributeName = "Method")
        private String method;

        public String getDeparture() {
            return departure;
        }

        public void setDeparture(String departure) {
            this.departure = departure;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public String getTimeOut() {
            return timeOut;
        }

        public void setTimeOut(String timeOut) {
            this.timeOut = timeOut;
        }

        public String getTimeIn() {
            return timeIn;
        }

        public void setTimeIn(String timeIn) {
            this.timeIn = timeIn;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }
    }

    @DynamoDBDocument
    public static class Discontinuity {
        @DynamoDBAttribute(attributeName = "Time")
        private String time;

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }
    }
}
