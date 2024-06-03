package org.example;

import org.jxmapviewer.viewer.GeoPosition;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class SegmentLengthCalculator {

    public static void main(String[] args) {
        try {
            String inputFile = "segments_test.csv";
            String outputFile = "segments_lengths.csv";

            List<SegmentLength> segmentLengths = calculateSegmentLengths(inputFile);
            writeSegmentLengthsToCSV(segmentLengths, outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<SegmentLength> calculateSegmentLengths(String inputFile) throws IOException {
        List<SegmentLength> segmentLengths = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(inputFile));

        for (String line : lines) {
            String[] columns = line.split("\\|");
            if (columns.length >= 10 && columns[13].startsWith("LINESTRING")) {
                String segmentId = columns[0];
                String geometry = columns[13];
                double length = calculateLengthFromWKT(geometry);

                segmentLengths.add(new SegmentLength(segmentId, geometry, length));
                System.out.printf("Processed segment %s with length %.2f meters%n", segmentId, length);
            }
        }

        return segmentLengths;
    }

    public static double calculateLengthFromWKT(String wkt) {
        List<GeoPosition> positions = parseWKTToPositions(wkt);
        double totalLength = 0.0;

        for (int i = 0; i < positions.size() - 1; i++) {
            totalLength += haversineDistance(positions.get(i), positions.get(i + 1));
        }

        return totalLength;
    }

    private static List<GeoPosition> parseWKTToPositions(String wkt) {
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

    private static double haversineDistance(GeoPosition pos1, GeoPosition pos2) {
        final int R = 6371000; // Radius of the Earth in meters
        double lat1 = Math.toRadians(pos1.getLatitude());
        double lon1 = Math.toRadians(pos1.getLongitude());
        double lat2 = Math.toRadians(pos2.getLatitude());
        double lon2 = Math.toRadians(pos2.getLongitude());

        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dlon / 2) * Math.sin(dlon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    public static void writeSegmentLengthsToCSV(List<SegmentLength> segmentLengths, String outputFile) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile))) {
            writer.write("id|geometry:1|length");
            writer.newLine();

            for (SegmentLength segmentLength : segmentLengths) {
                writer.write(String.format("%s|%s|%.2f", segmentLength.getId(), segmentLength.getGeometry(), segmentLength.getLength()));
                writer.newLine();
                System.out.printf("Written segment %s with length %.2f meters to CSV%n", segmentLength.getId(), segmentLength.getLength());
            }
        }
    }

    public static class SegmentLength {
        private String id;
        private String geometry;
        private double length;

        public SegmentLength(String id, String geometry, double length) {
            this.id = id;
            this.geometry = geometry;
            this.length = length;
        }

        public String getId() {
            return id;
        }

        public String getGeometry() {
            return geometry;
        }

        public double getLength() {
            return length;
        }
    }
}
