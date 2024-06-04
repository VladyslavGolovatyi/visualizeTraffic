package org.example;

import com.opencsv.*;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CSVLoader {
    public List<Segment> loadSegments(String filePath) throws Exception {
        List<Segment> segments = new ArrayList<>();
        CSVParser parser = new CSVParserBuilder()
                .withSeparator('|')
                .withIgnoreQuotations(true)
                .build();

        Reader reader = Files.newBufferedReader(Path.of(filePath));

        try (CSVReader csvReader = new CSVReaderBuilder(reader)
                .withSkipLines(1)
                .withCSVParser(parser)
                .build()) {
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                // Parse each line into a Segment object
                Segment segment = new Segment();
                segment.setId(line[0]);
                segment.setGeometry(line[2]);
                segments.add(segment);
            }
        }
        return segments;
    }

    public List<SegmentPart> loadSegmentsParts(String filePath) throws IOException {
        List<SegmentPart> segments = new ArrayList<>();

        CSVParser parser = new CSVParserBuilder()
                .withSeparator('|')
                .withIgnoreQuotations(true)
                .build();

        Reader reader = Files.newBufferedReader(Path.of(filePath));

        try (CSVReader csvReader = new CSVReaderBuilder(reader)
                .withSkipLines(1)
                .withCSVParser(parser)
                .build()) {
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                String id = line[0];
                String segmentId = line[0];
                String geometry = line[13];
                segments.add(new SegmentPart(id, segmentId, geometry, 0.0));
            }
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }
        return segments;
    }

    public List<AvgSpeed> loadAvgSpeeds(String filePath) throws Exception {
        List<AvgSpeed> avgSpeeds = new ArrayList<>();

        CSVParser parser = new CSVParserBuilder()
                .withSeparator(',')
                .withIgnoreQuotations(true)
                .build();

        Reader reader = Files.newBufferedReader(Path.of(filePath));

        try (CSVReader csvReader = new CSVReaderBuilder(reader)
                .withSkipLines(1)
                .withCSVParser(parser)
                .build()) {
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                // Parse each line into an AvgSpeed object
                AvgSpeed avgSpeed = new AvgSpeed();
                avgSpeed.setSegmentId(line[0]);
                avgSpeed.setTime(line[1]);
                avgSpeed.setAvgSpeed(Double.parseDouble(line[2]));
                avgSpeeds.add(avgSpeed);
            }
        }
        return avgSpeeds;
    }

    public void writeSegmentParts(String filePath, List<SegmentPart> segmentParts) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath), '|', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END)) {
            writer.writeNext(new String[]{"id", "segmentId", "geometry"});
            for (SegmentPart segmentPart : segmentParts) {
                writer.writeNext(new String[]{segmentPart.getId(), segmentPart.getSegmentId(), segmentPart.getGeometry()});
            }
        }
    }
}

class Segment {
    private String id;
    private String geometry; // The WKT representation of the geometry

    // getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public String toString() {
        return "Segment{" +
                "id='" + id + '\'' +
                ", geometry='" + geometry + '\'' +
                '}';
    }
}

class AvgSpeed {
    private String segmentId;
    private String time;
    private double avgSpeed;


    public AvgSpeed(String segmentId, String time, double avgSpeed) {
        this.segmentId = segmentId;
        this.time = time;
        this.avgSpeed = avgSpeed;
    }

    public AvgSpeed() {
    }

    // getters and setters
    public String getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(String segmentId) {
        this.segmentId = segmentId;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public double getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(double avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public String toString() {
        return "AvgSpeed{" +
                "segmentId='" + segmentId + '\'' +
                ", time='" + time + '\'' +
                ", avgSpeed=" + avgSpeed +
                '}';
    }
}
