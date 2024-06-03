package org.example;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCenter;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactory;
import org.jxmapviewer.viewer.TileFactoryInfo;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MapVisualizer {
    private static List<Segment> segments;
    private static List<TimePeriodAvgSpeed> avgSpeeds;
    private static JXMapViewer mapViewer;
    private static List<Painter<JXMapViewer>> painters;

    public static void main(String[] args) throws Exception {
        // Load data
        CSVLoader loader = new CSVLoader();
        segments = loader.loadSegments("segments.csv");
        List<AvgSpeed> rawAvgSpeeds = loader.loadAvgSpeeds("avg_speeds.csv");

        // Compute average speeds for each segment and time period
        avgSpeeds = computeAverageSpeeds(rawAvgSpeeds);

        // Create a JXMapViewer
        mapViewer = new JXMapViewer();

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

        // Enable interactions
        PanMouseInputListener panMouseInputListener = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(panMouseInputListener);
        mapViewer.addMouseMotionListener(panMouseInputListener);
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCenter(mapViewer));

        // Set the focus
        GeoPosition center = new GeoPosition(49.826, 24.038);
        mapViewer.setZoom(10);
        mapViewer.setAddressLocation(center);

        // Create painters for segments
        painters = createPaintersForTimePeriod(LocalTime.MIN);
        updateMapPainters();

        // Add zoom controls
        JPanel zoomPanel = new JPanel();
        JButton zoomInButton = new JButton("+");
        zoomInButton.addActionListener(e -> mapViewer.setZoom(mapViewer.getZoom() - 1));
        JButton zoomOutButton = new JButton("-");
        zoomOutButton.addActionListener(e -> mapViewer.setZoom(mapViewer.getZoom() + 1));
        zoomPanel.add(zoomInButton);
        zoomPanel.add(zoomOutButton);

        // Add time slider
        LocalTime minTime = avgSpeeds.stream().map(TimePeriodAvgSpeed::getTime).map(MapVisualizer::parseTime).min(LocalTime::compareTo).orElse(LocalTime.MIN);
        LocalTime maxTime = avgSpeeds.stream().map(TimePeriodAvgSpeed::getTime).map(MapVisualizer::parseTime).max(LocalTime::compareTo).orElse(LocalTime.MAX);
        int maxHours = (int) minTime.until(maxTime, java.time.temporal.ChronoUnit.HOURS);

        JSlider timeSlider = new JSlider(0, maxHours, 0);
        timeSlider.setMajorTickSpacing(1); // One hour intervals
        timeSlider.setPaintTicks(true);
        timeSlider.setPaintLabels(true);
        timeSlider.setLabelTable(createSliderLabels(minTime, maxHours));

        timeSlider.addChangeListener(e -> {
            int selectedHours = timeSlider.getValue();
            LocalTime selectedTime = minTime.plusHours(selectedHours);
            painters = createPaintersForTimePeriod(selectedTime);
            updateMapPainters();
        });

        // Display the viewer in a JFrame
        JFrame frame = new JFrame("Map Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(mapViewer, BorderLayout.CENTER);
        frame.add(zoomPanel, BorderLayout.SOUTH);
        frame.add(timeSlider, BorderLayout.NORTH);
        frame.setSize(800, 600);
        frame.setVisible(true);
    }

    private static List<TimePeriodAvgSpeed> computeAverageSpeeds(List<AvgSpeed> rawAvgSpeeds) {
        Map<String, Map<String, List<Double>>> groupedSpeeds = new HashMap<>();

        for (AvgSpeed avgSpeed : rawAvgSpeeds) {
            String segmentId = avgSpeed.getSegmentId();
            String time = avgSpeed.getTime().substring(11, 13) + ":00:00"; // Extract only the hour part

            groupedSpeeds.putIfAbsent(segmentId, new HashMap<>());
            groupedSpeeds.get(segmentId).putIfAbsent(time, new ArrayList<>());
            groupedSpeeds.get(segmentId).get(time).add(avgSpeed.getAvgSpeed());
        }

        List<TimePeriodAvgSpeed> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<Double>>> segmentEntry : groupedSpeeds.entrySet()) {
            String segmentId = segmentEntry.getKey();
            for (Map.Entry<String, List<Double>> timeEntry : segmentEntry.getValue().entrySet()) {
                String time = timeEntry.getKey();
                double avgSpeed = timeEntry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                result.add(new TimePeriodAvgSpeed(segmentId, time, avgSpeed));
            }
        }

        return result;
    }

    private static List<Painter<JXMapViewer>> createPaintersForTimePeriod(LocalTime timePeriod) {
        List<Painter<JXMapViewer>> painters = new ArrayList<>();
        List<TimePeriodAvgSpeed> filteredSpeeds = avgSpeeds.stream()
                .filter(avgSpeed -> parseTime(avgSpeed.getTime()).equals(timePeriod))
                .collect(Collectors.toList());

        for (Segment segment : segments) {
            List<GeoPosition> positions = parseWKTToPositions(segment.getGeometry());
            if (positions != null && !positions.isEmpty()) {
                Color segmentColor = getSegmentColor(segment.getId(), filteredSpeeds);
                if (segmentColor != null) { // Only add segments with avg speed
                    RoutePainter routePainter = new RoutePainter(positions, segmentColor);
                    painters.add(routePainter);
                }
            }
        }

        return painters;
    }

    private static void updateMapPainters() {
        CompoundPainter<JXMapViewer> compoundPainter = new CompoundPainter<>(painters);
        mapViewer.setOverlayPainter(compoundPainter);
        mapViewer.repaint();
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

    private static Color getSegmentColor(String segmentId, List<TimePeriodAvgSpeed> avgSpeeds) {
        for (TimePeriodAvgSpeed avgSpeed : avgSpeeds) {
            if (avgSpeed.getSegmentId().equals(segmentId)) {
                double speed = avgSpeed.getAvgSpeed();
                if (speed < 3) {
                    return Color.RED;
                } else if (speed < 6) {
                    return Color.YELLOW;
                } else {
                    return Color.GREEN;
                }
            }
        }
        return null;  // Return null if no avg speed is found
    }

    private static LocalTime parseTime(String time) {
        return LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private static Hashtable<Integer, JLabel> createSliderLabels(LocalTime startTime, int maxHours) {
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        for (int i = 0; i <= maxHours; i++) {  // Create labels at hourly intervals
            LocalTime time = startTime.plusHours(i);
            labelTable.put(i, new JLabel(time.format(formatter)));
        }
        return labelTable;
    }
}
