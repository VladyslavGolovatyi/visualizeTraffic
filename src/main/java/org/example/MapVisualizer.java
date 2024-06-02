package org.example;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class MapVisualizer {
    public static void main(String[] args) throws Exception {
        // Load data
        CSVLoader loader = new CSVLoader();
        List<Segment> segments = loader.loadSegments("segments.csv");
        List<AvgSpeed> avgSpeeds = loader.loadAvgSpeeds("avg_speeds.csv");

        // Create a JXMapViewer
        JXMapViewer mapViewer = new JXMapViewer();

        // Setup tile factory
        TileFactoryInfo info = new TileFactoryInfo(
                1, 15, 17,
                256, true, true,
                "https://tile.openstreetmap.org",
                "x", "y", "z") {
            @Override
            public String getTileUrl(int x, int y, int zoom) {
                int tileZoom = getTotalMapZoom() - zoom;
                return String.format("https://tile.openstreetmap.org/%d/%d/%d.png", tileZoom, x, y);
            }
        };

        TileFactory tileFactory = new DefaultTileFactory(info);
        mapViewer.setTileFactory(tileFactory);

        // Set the focus
        GeoPosition center = new GeoPosition(49.826, 24.038);
        mapViewer.setZoom(10);  // Set zoom level here (higher values zoom in more)
        mapViewer.setAddressLocation(center);

        // Create painters for segments
        List<Painter<JXMapViewer>> painters = new ArrayList<>();
        int i = 0;
        for (Segment segment : segments) {
            List<GeoPosition> positions = parseWKTToPositions(segment.getGeometry());
            if (positions != null && !positions.isEmpty()) {
                Color segmentColor = getSegmentColor(segment.getId(), avgSpeeds);
                RoutePainter routePainter = new RoutePainter(positions, segmentColor);
                painters.add(routePainter);
            }
            System.out.println(++i + " segments processed");
        }

        // Combine painters
        CompoundPainter<JXMapViewer> compoundPainter = new CompoundPainter<>(painters);
        mapViewer.setOverlayPainter(compoundPainter);

        // Display the viewer in a JFrame
        JFrame frame = new JFrame("Map Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(mapViewer);
        frame.setSize(800, 600);
        frame.setVisible(true);
    }

    private static List<GeoPosition> parseWKTToPositions(String wkt) {
        // This function parses the WKT (Well-Known Text) geometry to a list of GeoPositions
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

    private static Color getSegmentColor(String segmentId, List<AvgSpeed> avgSpeeds) {
        for (AvgSpeed avgSpeed : avgSpeeds) {
            if (avgSpeed.getSegmentId().equals(segmentId)) {
                double speed = avgSpeed.getAvgSpeed();
                if (speed < 5) {
                    return Color.RED;
                } else if (speed < 10) {
                    return Color.ORANGE;
                } else {
                    return Color.GREEN;
                }
            }
        }
        return Color.BLACK;  // Default color if no avg speed is found
    }
}
