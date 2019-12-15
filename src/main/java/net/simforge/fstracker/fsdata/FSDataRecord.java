package net.simforge.fstracker.fsdata;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class FSDataRecord {
    public enum Status { NO_CONNECTION, INVALID_CONNECTION, READ_TIMEOUT, OK }

    private LocalDateTime dateTime;
    private Status status;
    private FSData fsData;

    public FSDataRecord(Status status) {
        this.dateTime = LocalDateTime.now(ZoneId.of("UTC"));
        this.status = status;
    }

    public FSDataRecord(Status status, FSData fsDataRecord) {
        this.dateTime = LocalDateTime.now(ZoneId.of("UTC"));
        this.status = status;
        this.fsData = fsDataRecord;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public Status getStatus() {
        return status;
    }

    public FSData getFsData() {
        return fsData;
    }

}
