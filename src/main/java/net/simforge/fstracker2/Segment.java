package net.simforge.fstracker2;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Segment {
    private SegmentType type;
    private String aircraftTitle;
    private EdgeType startType;
    private String startIcao;
    private LocalDateTime startDt;
    private EdgeType finishType;
    private String finishIcao;
    private LocalDateTime finishDt;
    private long recordedTimeMs;
    private double recordedDistanceNm;
}
