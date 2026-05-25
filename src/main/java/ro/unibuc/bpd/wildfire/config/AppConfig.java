package ro.unibuc.bpd.wildfire.config;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public record AppConfig(
        String bootstrapServers,
        String fireTopic,
        String weatherTopic,
        String predictionTopic,
        int dashboardPort,
        int parallelism,
        String firmsMapKey,
        String firmsBaseUrl,
        String firmsSource,
        String firmsArea,
        int firmsLookbackDays,
        int ingestPollSeconds,
        String openWeatherApiKey,
        String openWeatherBaseUrl,
        String naturalEarthAdmin0Path,
        String naturalEarthAdmin1Path
) implements Serializable {

    public static AppConfig fromArgs(String[] args) {
        Map<String, String> overrides = parseArgs(args);
        return new AppConfig(
                overrides.getOrDefault("bootstrap-servers", envOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")),
                overrides.getOrDefault("fire-topic", envOrDefault("FIRE_TOPIC", "firms-hotspots")),
                overrides.getOrDefault("weather-topic", envOrDefault("WEATHER_TOPIC", "weather-observations")),
                overrides.getOrDefault("prediction-topic", envOrDefault("PREDICTION_TOPIC", "wildfire-risk-predictions")),
                Integer.parseInt(overrides.getOrDefault("dashboard-port", envOrDefault("DASHBOARD_PORT", "7070"))),
                Integer.parseInt(overrides.getOrDefault("parallelism", envOrDefault("FLINK_PARALLELISM", "1"))),
                overrides.getOrDefault("firms-map-key", envOrDefault("FIRMS_MAP_KEY", "")),
                overrides.getOrDefault("firms-base-url", envOrDefault("FIRMS_BASE_URL", "https://firms.modaps.eosdis.nasa.gov")),
                overrides.getOrDefault("firms-source", envOrDefault("FIRMS_SOURCE", "VIIRS_SNPP_NRT")),
                overrides.getOrDefault("firms-area", envOrDefault("FIRMS_AREA", "world")),
                Integer.parseInt(overrides.getOrDefault("firms-lookback-days", envOrDefault("FIRMS_LOOKBACK_DAYS", "1"))),
                Integer.parseInt(overrides.getOrDefault("ingest-poll-seconds", envOrDefault("INGEST_POLL_SECONDS", "60"))),
                overrides.getOrDefault("openweather-api-key", envOrDefault("OPENWEATHER_API_KEY", "")),
                overrides.getOrDefault("openweather-base-url", envOrDefault("OPENWEATHER_BASE_URL", "https://api.openweathermap.org")),
                overrides.getOrDefault("natural-earth-admin0-path", envOrDefault("NATURAL_EARTH_ADMIN0_PATH", "")),
                overrides.getOrDefault("natural-earth-admin1-path", envOrDefault("NATURAL_EARTH_ADMIN1_PATH", ""))
        );
    }

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
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
}
