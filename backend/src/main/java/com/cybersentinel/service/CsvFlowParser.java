package com.cybersentinel.service;

import com.cybersentinel.model.NetworkFlow;
import com.opencsv.CSVReader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates and parses an uploaded network-flow CSV into NetworkFlow
 * objects. Invalid individual rows are skipped and reported as warnings
 * rather than silently dropped or crashing the whole upload.
 */
@Service
public class CsvFlowParser {

    private static final List<String> REQUIRED_COLUMNS = List.of(
            "sourceip", "destinationip", "protocol", "sourceport",
            "destinationport", "flowduration", "packetcount", "bytecount"
    );
    private static final int MAX_ROWS = 20000;

    public static class ParseResult {
        public final List<NetworkFlow> flows = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();
    }

    public ParseResult parse(MultipartFile file) {
        ParseResult result = new ParseResult();

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Only .csv files are accepted.");
        }

        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {

            String[] header = csvReader.readNext();
            if (header == null) {
                throw new IllegalArgumentException("The CSV file is empty.");
            }

            Map<String, Integer> colIndex = new HashMap<>();
            for (int i = 0; i < header.length; i++) {
                colIndex.put(header[i].trim().toLowerCase(), i);
            }

            List<String> missing = new ArrayList<>();
            for (String required : REQUIRED_COLUMNS) {
                if (!colIndex.containsKey(required)) {
                    missing.add(required);
                }
            }
            if (!missing.isEmpty()) {
                throw new IllegalArgumentException(
                        "CSV is missing required column(s): " + String.join(", ", missing) +
                        ". Expected columns: sourceIP, destinationIP, protocol, sourcePort, " +
                        "destinationPort, flowDuration, packetCount, byteCount " +
                        "(packetsPerSecond, bytesPerSecond, timestamp are optional).");
            }

            String[] row;
            int rowNum = 1;
            while ((row = csvReader.readNext()) != null) {
                rowNum++;
                if (result.flows.size() >= MAX_ROWS) {
                    result.warnings.add("File exceeds " + MAX_ROWS + " rows; remaining rows were ignored.");
                    break;
                }
                try {
                    NetworkFlow flow = parseRow(row, colIndex);
                    result.flows.add(flow);
                } catch (Exception e) {
                    result.warnings.add("Row " + rowNum + " skipped: " + e.getMessage());
                }
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read CSV file: " + e.getMessage());
        }

        if (result.flows.isEmpty()) {
            throw new IllegalArgumentException("No valid flow records could be parsed from this file.");
        }

        return result;
    }

    private NetworkFlow parseRow(String[] row, Map<String, Integer> colIndex) {
        String sourceIP = get(row, colIndex, "sourceip");
        String destIP = get(row, colIndex, "destinationip");
        String protocol = get(row, colIndex, "protocol").toUpperCase();

        if (sourceIP.isBlank() || destIP.isBlank()) {
            throw new IllegalArgumentException("sourceIP/destinationIP cannot be empty");
        }
        if (!protocol.equals("TCP") && !protocol.equals("UDP") && !protocol.equals("ICMP")) {
            throw new IllegalArgumentException("unsupported protocol '" + protocol + "'");
        }

        double flowDuration = parseDouble(get(row, colIndex, "flowduration"));
        long packetCount = (long) parseDouble(get(row, colIndex, "packetcount"));
        long byteCount = (long) parseDouble(get(row, colIndex, "bytecount"));

        if (flowDuration <= 0) throw new IllegalArgumentException("flowDuration must be > 0");
        if (packetCount <= 0) throw new IllegalArgumentException("packetCount must be > 0");
        if (byteCount < 0) throw new IllegalArgumentException("byteCount cannot be negative");

        Double pps = has(colIndex, "packetspersecond") ? tryParseDouble(get(row, colIndex, "packetspersecond")) : null;
        Double bps = has(colIndex, "bytespersecond") ? tryParseDouble(get(row, colIndex, "bytespersecond")) : null;

        Instant timestamp = Instant.now();
        if (has(colIndex, "timestamp")) {
            String ts = get(row, colIndex, "timestamp");
            try {
                if (!ts.isBlank()) timestamp = Instant.parse(ts);
            } catch (Exception ignored) {
                // keep default "now" if the timestamp format is unparseable
            }
        }

        return NetworkFlow.builder()
                .sourceIP(sourceIP)
                .destinationIP(destIP)
                .protocol(protocol)
                .sourcePort((int) parseDouble(get(row, colIndex, "sourceport")))
                .destinationPort((int) parseDouble(get(row, colIndex, "destinationport")))
                .flowDuration(flowDuration)
                .packetCount(packetCount)
                .byteCount(byteCount)
                .packetsPerSecond(pps != null ? pps : packetCount / Math.max(flowDuration, 0.001))
                .bytesPerSecond(bps != null ? bps : byteCount / Math.max(flowDuration, 0.001))
                .timestamp(timestamp)
                .build();
    }

    private String get(String[] row, Map<String, Integer> colIndex, String col) {
        Integer idx = colIndex.get(col);
        if (idx == null || idx >= row.length) return "";
        return row[idx] == null ? "" : row[idx].trim();
    }

    private boolean has(Map<String, Integer> colIndex, String col) {
        return colIndex.containsKey(col);
    }

    private double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid numeric value '" + s + "'");
        }
    }

    private Double tryParseDouble(String s) {
        try {
            return s == null || s.isBlank() ? null : Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }
}
