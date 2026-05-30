package ro.upb.wildfire.ingest;

import ro.upb.wildfire.geo.GeoUtils;
import ro.upb.wildfire.model.FireHotspotEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FirmsCsvParser {

    private FirmsCsvParser() {
    }

    public static List<FireHotspotEvent> parseResource(String resourcePath) throws IOException {
        try (InputStream stream = FirmsCsvParser.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Missing resource: " + resourcePath);
            }
            return parse(new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
        }
    }

    public static List<FireHotspotEvent> parse(BufferedReader reader) throws IOException {
        String header = reader.readLine();
        if (header == null) {
            return List.of();
        }

        Map<String, Integer> index = indexHeader(header);
        List<FireHotspotEvent> events = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            String[] columns = splitCsvLine(line);
            double latitude = parseDouble(value(index, columns, "latitude"));
            double longitude = parseDouble(value(index, columns, "longitude"));
            double brightness = parseBrightness(index, columns);
            double confidence = parseConfidence(value(index, columns, "confidence"));
            Instant acquisitionTime = parseTimestamp(
                    value(index, columns, "acq_date"),
                    value(index, columns, "acq_time")
            );
            String source = value(index, columns, "instrument");
            if (source.isBlank()) {
                source = value(index, columns, "satellite");
            }

            events.add(new FireHotspotEvent(
                    latitude,
                    longitude,
                    brightness,
                    confidence,
                    acquisitionTime,
                    source.isBlank() ? "FIRMS" : source,
                    GeoUtils.toCellId(latitude, longitude)
            ));
        }

        return events;
    }

    private static Map<String, Integer> indexHeader(String headerLine) {
        String[] columns = splitCsvLine(headerLine);
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < columns.length; i++) {
            index.put(columns[i].trim().toLowerCase(), i);
        }
        return index;
    }

    private static String[] splitCsvLine(String line) {
        return line.split(",", -1);
    }

    private static String value(Map<String, Integer> index, String[] columns, String key) {
        Integer position = index.get(key);
        if (position == null || position >= columns.length) {
            return "";
        }
        return columns[position].trim();
    }

    private static double parseBrightness(Map<String, Integer> index, String[] columns) {
        String[] candidates = {"bright_ti4", "brightness", "bright_t31"};
        for (String candidate : candidates) {
            String value = value(index, columns, candidate);
            if (!value.isBlank()) {
                return parseDouble(value);
            }
        }
        return 300.0;
    }

    private static double parseConfidence(String raw) {
        if (raw == null || raw.isBlank()) {
            return 50.0;
        }
        return switch (raw.toLowerCase()) {
            case "l" -> 35.0;
            case "n" -> 60.0;
            case "h" -> 85.0;
            case "low" -> 35.0;
            case "nominal" -> 60.0;
            case "high" -> 85.0;
            default -> parseDouble(raw);
        };
    }

    private static Instant parseTimestamp(String date, String time) {
        String paddedTime = String.format("%04d", Integer.parseInt(time));
        int hours = Integer.parseInt(paddedTime.substring(0, 2));
        int minutes = Integer.parseInt(paddedTime.substring(2, 4));
        LocalDateTime timestamp = LocalDateTime.parse(date + "T" + String.format("%02d:%02d:00", hours, minutes));
        return timestamp.toInstant(ZoneOffset.UTC);
    }

    private static double parseDouble(String value) {
        return Double.parseDouble(value);
    }
}
