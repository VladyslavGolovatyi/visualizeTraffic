package org.example;

public class TimePeriodAvgSpeed {
    private final String segmentId;
    private final String time;
    private final double avgSpeed;

    public TimePeriodAvgSpeed(String segmentId, String time, double avgSpeed) {
        this.segmentId = segmentId;
        this.time = time;
        this.avgSpeed = avgSpeed;
    }

    public String getSegmentId() {
        return segmentId;
    }

    public String getTime() {
        return time;
    }

    public double getAvgSpeed() {
        return avgSpeed;
    }

    public String toString() {
        return "TimePeriodAvgSpeed{" +
                "segmentId='" + segmentId + '\'' +
                ", time='" + time + '\'' +
                ", avgSpeed=" + avgSpeed +
                '}';
    }
}
