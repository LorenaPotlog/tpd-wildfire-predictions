package ro.upb.wildfire.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import ro.upb.wildfire.geo.GeoUtils;
import ro.upb.wildfire.model.WeatherObservation;
import ro.upb.wildfire.serialization.JsonSerde;

import java.time.Instant;

public final class OpenWeatherJsonParser {

    private OpenWeatherJsonParser() {
    }

    public static WeatherObservation parse(String json) {
        try {
            JsonNode root = JsonSerde.mapper().readTree(json);
            JsonNode coord = root.path("coord");
            JsonNode wind = root.path("wind");
            JsonNode main = root.path("main");

            double latitude = coord.path("lat").asDouble();
            double longitude = coord.path("lon").asDouble();
            double windSpeed = wind.path("speed").asDouble();
            double windDirection = wind.path("deg").asDouble();
            double humidity = main.path("humidity").asDouble(35.0);
            Instant observationTime = Instant.ofEpochSecond(root.path("dt").asLong());

            return new WeatherObservation(
                    latitude,
                    longitude,
                    windSpeed,
                    windDirection,
                    humidity,
                    observationTime,
                    "OpenWeatherMap",
                    GeoUtils.toCellId(latitude, longitude)
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to parse OpenWeather JSON payload", exception);
        }
    }
}

