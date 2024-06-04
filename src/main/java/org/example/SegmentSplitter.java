package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SegmentSplitter {

    public static List<SegmentPart> splitSegment(SegmentPart segment) {
        System.out.println(segment.getGeometry());
        List<GeoPosition> positions = parseWKTToPositions(segment.getGeometry());
        if (positions == null || positions.size() < 2) {
            System.out.println("Invalid segment geometry");
            return new ArrayList<>();
        }

        List<SegmentPart> splitSegments = new ArrayList<>();
        for (int i = 0; i < positions.size() - 1; i++) {
            List<GeoPosition> segmentPositions = new ArrayList<>();
            segmentPositions.add(positions.get(i));
            segmentPositions.add(positions.get(i + 1));
            String newSegmentGeometry = convertPositionsToWKT(segmentPositions);
            SegmentPart newSegment = new SegmentPart(generateSegmentId(), segment.getSegmentId(), newSegmentGeometry);
            splitSegments.add(newSegment);
        }
        return splitSegments;
    }

    private static String generateSegmentId() {
        return UUID.randomUUID().toString();
    }

    private static List<GeoPosition> parseWKTToPositions(String wkt) {
        if (wkt == null || !wkt.startsWith("LINESTRING")) {
            return null;
        }
        wkt = wkt.replace("LINESTRING (", "").replace(")", "");
        String[] coords = wkt.split(", ");
        List<GeoPosition> positions = new ArrayList<>();
        for (String coord : coords) {
            String[] latLon = coord.split(" ");
            double lat = Double.parseDouble(latLon[1]);
            double lon = Double.parseDouble(latLon[0]);
            positions.add(new GeoPosition(lat, lon));
        }
        return positions;
    }

    private static String convertPositionsToWKT(List<GeoPosition> positions) {
        StringBuilder sb = new StringBuilder("LINESTRING (");
        for (int i = 0; i < positions.size(); i++) {
            GeoPosition pos = positions.get(i);
            sb.append(String.format("%f %f", pos.getLongitude(), pos.getLatitude()));
            if (i < positions.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        // Load segments from CSV
        CSVLoader loader = new CSVLoader();
        List<SegmentPart> segments = loader.loadSegmentsParts("segments.csv");

        // Split each segment into smaller parts
        List<SegmentPart> splitSegments = new ArrayList<>();
        for (SegmentPart segment : segments) {
            splitSegments.addAll(splitSegment(segment));
        }

        // Write the split segments to a new CSV file
        loader.writeSegmentParts("segment_parts.csv", splitSegments);

        System.out.println("Segment parts have been successfully written to segment_parts.csv");
    }
}

class SegmentPart {
    private String id;
    private String segmentId;
    private String geometry;

    public SegmentPart(String id, String segmentId, String geometry) {
        this.id = id;
        this.segmentId = segmentId;
        this.geometry = geometry;
    }

    public String getId() {
        return id;
    }

    public String getSegmentId() {
        return segmentId;
    }

    public String getGeometry() {
        return geometry;
    }

    @Override
    public String toString() {
        return String.format("%s|%s|%s", id, segmentId, geometry);
    }
}

class GeoPosition {
    private double latitude;
    private double longitude;

    public GeoPosition(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
