package org.example;

import com.opencsv.*;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SegmentPartsSpeedJoiner {

    public static void main(String[] args) {
        try {
            String segmentPartsFile = "segment_parts.csv";
            String avgSpeedsFile = "avg_speeds_per_hour.csv";
            String outputFile = "segment_parts_speeds.csv";

            joinSegmentPartsAndSpeeds(segmentPartsFile, avgSpeedsFile, outputFile);
            System.out.println("Segment parts and speeds have been successfully joined and written to " + outputFile);
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
    }

    public static void joinSegmentPartsAndSpeeds(String segmentPartsFile, String avgSpeedsFile, String outputFile) throws IOException, CsvValidationException {
        List<SegmentPart> segmentParts = loadSegmentParts(segmentPartsFile);
        List<AvgSpeed> avgSpeeds = loadAvgSpeeds(avgSpeedsFile);

        Map<String, List<AvgSpeed>> avgSpeedMap = avgSpeeds.stream()
                .collect(Collectors.groupingBy(AvgSpeed::getSegmentId));

        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            writer.writeNext(new String[]{"id", "segmentId", "geometry", "length", "time", "avgSpeed"});

            for (SegmentPart segmentPart : segmentParts) {
                List<AvgSpeed> segmentSpeeds = avgSpeedMap.get(segmentPart.getSegmentId());
                if (segmentSpeeds != null) {
                    for (AvgSpeed avgSpeed : segmentSpeeds) {
                        writer.writeNext(new String[]{
                                segmentPart.getId(),
                                segmentPart.getSegmentId(),
                                segmentPart.getGeometry(),
                                String.valueOf(segmentPart.getLength()),
                                avgSpeed.getTime(),
                                String.valueOf(avgSpeed.getAvgSpeed())
                        });
                    }
                }
            }
        }
    }

    public static List<SegmentPart> loadSegmentParts(String filePath) throws IOException, CsvValidationException {
        List<SegmentPart> segmentParts = new ArrayList<>();

        CSVParser parser = new CSVParserBuilder()
                .withSeparator('|')
                .withIgnoreQuotations(true)
                .build();

        try (CSVReader csvReader = new CSVReaderBuilder(Files.newBufferedReader(Paths.get(filePath)))
                .withSkipLines(1)
                .withCSVParser(parser)
                .build()) {
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                String id = line[0];
                String segmentId = line[1];
                String geometry = line[2];
                double length = Double.parseDouble(line[3].replace(",", "."));
                segmentParts.add(new SegmentPart(id, segmentId, geometry, length));
            }
        }
        return segmentParts;
    }

    public static List<AvgSpeed> loadAvgSpeeds(String filePath) throws IOException {
        List<AvgSpeed> avgSpeeds = new ArrayList<>();

        CSVParser parser = new CSVParserBuilder()
                .withSeparator(',')
                .withIgnoreQuotations(true)
                .build();

        try (CSVReader csvReader = new CSVReaderBuilder(Files.newBufferedReader(Paths.get(filePath)))
                .withSkipLines(1)
                .withCSVParser(parser)
                .build()) {
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                String segmentId = line[0];
                String time = line[1];
                double avgSpeed = Double.parseDouble(line[2].replace(",", "."));
                avgSpeeds.add(new AvgSpeed(segmentId, time, avgSpeed));
            }
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }
        return avgSpeeds;
    }
}
