package org.example;

import org.jxmapviewer.viewer.GeoPosition;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class SegmentPartLengthCalculator {

    public static void main(String[] args) {
        try {
            String inputFile = "segment_parts.csv";

            // Calculate the lengths and update the CSV
            updateSegmentLengths(inputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateSegmentLengths(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        List<String> lines = Files.readAllLines(path);
        List<String> updatedLines = new ArrayList<>();

        // Add header
        String header = lines.get(0) + "|length";
        updatedLines.add(header);

        // Process each line
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] columns = line.split("\\|");
            String geometry = columns[2];
            double length = calculateLengthFromWKT(geometry);
            String updatedLine = line + "|" + length;
            updatedLines.add(updatedLine);
        }

        // Write back to the same file
        Files.write(path, updatedLines);
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
            double lat = Double.parseDouble(latLon[1].replace(",", "."));
            double lon = Double.parseDouble(latLon[0].replace(",", "."));
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
}
