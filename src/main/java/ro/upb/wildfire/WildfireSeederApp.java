package ro.upb.wildfire;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import ro.upb.wildfire.config.AppConfig;
import ro.upb.wildfire.ingest.FirmsCsvParser;
import ro.upb.wildfire.ingest.OpenWeatherJsonParser;
import ro.upb.wildfire.model.FireHotspotEvent;
import ro.upb.wildfire.model.RiskPrediction;
import ro.upb.wildfire.model.WeatherObservation;
import ro.upb.wildfire.serialization.JsonSerde;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class WildfireSeederApp {
    private static final int DEFAULT_DEMO_DURATION_SECONDS = 210;
    private static final int DEFAULT_DEMO_STEP_SECONDS = 10;

    private static final List<DemoIncident> DEMO_INCIDENTS = List.of(
            new DemoIncident(
                    "ro-low",
                    "Romania",
                    "Romania",
                    45.7600,
                    24.6500,
                    0.010,
                    0.020,
                    65.0,
                    8.0,
                    0.58,
                    new double[][]{{0, 18.0}, {8, 24.0}, {20, 21.0}}
            ),
            new DemoIncident(
                    "es-medium",
                    "Spain",
                    "Spain",
                    39.4700,
                    -1.4200,
                    0.015,
                    -0.018,
                    120.0,
                    15.0,
                    0.74,
                    new double[][]{{0, 36.0}, {6, 44.0}, {12, 39.0}, {20, 47.0}}
            ),
            new DemoIncident(
                    "us-escalating",
                    "California",
                    "United States",
                    34.4200,
                    -118.5800,
                    0.012,
                    -0.014,
                    210.0,
                    19.0,
                    0.84,
                    new double[][]{{0, 28.0}, {5, 42.0}, {10, 58.0}, {14, 67.0}, {18, 81.0}, {20, 72.0}}
            ),
            new DemoIncident(
                    "za-escalating",
                    "South Africa",
                    "South Africa",
                    -29.0200,
                    26.1800,
                    0.018,
                    0.012,
                    145.0,
                    17.0,
                    0.81,
                    new double[][]{{0, 34.0}, {5, 48.0}, {10, 63.0}, {15, 78.0}, {20, 84.0}}
            ),
            new DemoIncident(
                    "ke-rising",
                    "East Africa",
                    "Kenya/Tanzania",
                    -2.5200,
                    36.8200,
                    0.020,
                    0.016,
                    90.0,
                    12.0,
                    0.69,
                    new double[][]{{0, 22.0}, {6, 31.0}, {12, 43.0}, {18, 54.0}, {20, 49.0}}
            ),
            new DemoIncident(
                    "pt-flare",
                    "Portugal",
                    "Portugal",
                    39.7800,
                    -8.0700,
                    0.013,
                    -0.010,
                    280.0,
                    16.0,
                    0.77,
                    new double[][]{{0, 26.0}, {4, 39.0}, {8, 63.0}, {11, 76.0}, {15, 58.0}, {20, 41.0}}
            )
    );

    private WildfireSeederApp() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromArgs(args);
        Map<String, String> options = parseArgs(args);
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {
            String demoMode = options.getOrDefault("demo-mode", "scripted");
            if ("sample-inputs".equalsIgnoreCase(demoMode)) {
                seedFireEvents(producer, config.fireTopic());
                seedWeatherEvents(producer, config.weatherTopic());
            } else {
                int durationSeconds = parseInt(options.get("demo-duration-seconds"), DEFAULT_DEMO_DURATION_SECONDS);
                int stepSeconds = parseInt(options.get("demo-step-seconds"), DEFAULT_DEMO_STEP_SECONDS);
                runScriptedPredictionDemo(producer, config.predictionTopic(), durationSeconds, stepSeconds);
            }
            producer.flush();
        }
    }

    private static void runScriptedPredictionDemo(
            KafkaProducer<String, String> producer,
            String topic,
            int durationSeconds,
            int stepSeconds
    ) throws InterruptedException {
        int totalSteps = Math.max(1, durationSeconds / Math.max(1, stepSeconds));
        Instant baseTime = Instant.now();

        for (int step = 0; step <= totalSteps; step++) {
            Instant tickTime = baseTime.plusSeconds((long) step * stepSeconds);
            for (DemoIncident incident : DEMO_INCIDENTS) {
                RiskPrediction prediction = incident.predictionAt(step, tickTime);
                producer.send(new ProducerRecord<>(topic, prediction.cellId(), JsonSerde.toJson(prediction)));
            }
            producer.flush();

            if (step < totalSteps) {
                Thread.sleep(stepSeconds * 1000L);
            }
        }
    }

    private static void seedFireEvents(KafkaProducer<String, String> producer, String topic) throws IOException {
        List<FireHotspotEvent> events = FirmsCsvParser.parseResource("/sample/firms_hotspots.csv");
        for (FireHotspotEvent event : events) {
            producer.send(new ProducerRecord<>(topic, event.cellId(), JsonSerde.toJson(event)));
        }
    }

    private static void seedWeatherEvents(KafkaProducer<String, String> producer, String topic) throws IOException {
        try (BufferedReader reader = resourceReader("/sample/weather_observations.jsonl")) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                WeatherObservation observation = OpenWeatherJsonParser.parse(line);
                producer.send(new ProducerRecord<>(topic, observation.cellId(), JsonSerde.toJson(observation)));
            }
        }
    }

    private static BufferedReader resourceReader(String path) {
        InputStream stream = WildfireSeederApp.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Missing resource: " + path);
        }
        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> values = new HashMap<>();
        for (String arg : args) {
            if (!arg.startsWith("--") || !arg.contains("=")) {
                continue;
            }
            String[] parts = arg.substring(2).split("=", 2);
            values.put(parts[0], parts[1]);
        }
        return values;
    }

    private static int parseInt(String rawValue, int fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(rawValue);
    }

    private static String riskLevel(double threatScore) {
        if (threatScore >= 75.0) {
            return "EXTREME";
        }
        if (threatScore >= 60.0) {
            return "HIGH";
        }
        if (threatScore >= 35.0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static double interpolate(double[][] keyframes, int step) {
        if (step <= keyframes[0][0]) {
            return keyframes[0][1];
        }
        for (int index = 1; index < keyframes.length; index++) {
            double[] previous = keyframes[index - 1];
            double[] current = keyframes[index];
            if (step <= current[0]) {
                double span = current[0] - previous[0];
                double progress = span == 0.0 ? 1.0 : (step - previous[0]) / span;
                return previous[1] + (current[1] - previous[1]) * progress;
            }
        }
        return keyframes[keyframes.length - 1][1];
    }

    private record DemoIncident(
            String incidentId,
            String zoneName,
            String country,
            double baseLatitude,
            double baseLongitude,
            double latitudeDrift,
            double longitudeDrift,
            double baseBearing,
            double baseVelocityKph,
            double confidenceIndex,
            double[][] threatKeyframes
    ) {

        private RiskPrediction predictionAt(int step, Instant predictionTime) {
            double threatScore = round(interpolate(threatKeyframes, step));
            double latitude = round(baseLatitude + (latitudeDrift * step));
            double longitude = round(baseLongitude + (longitudeDrift * step));
            double spreadBearing = round((baseBearing + (step * 11.0)) % 360.0);
            double spreadVelocityKph = round(baseVelocityKph + (Math.max(0.0, threatScore - 20.0) * 0.55));
            String cellId = "%s-%02d".formatted(incidentId, step);

            return new RiskPrediction(
                    predictionTime,
                    latitude,
                    longitude,
                    cellId,
                    zoneName,
                    country,
                    threatScore,
                    riskLevel(threatScore),
                    spreadBearing,
                    spreadVelocityKph,
                    confidenceIndex,
                    false
            );
        }

        private double round(double value) {
            return Math.round(value * 10.0) / 10.0;
        }
    }
}
