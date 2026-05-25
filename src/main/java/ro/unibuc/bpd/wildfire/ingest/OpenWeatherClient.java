package ro.unibuc.bpd.wildfire.ingest;

import ro.unibuc.bpd.wildfire.config.AppConfig;
import ro.unibuc.bpd.wildfire.model.WeatherObservation;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class OpenWeatherClient {
    private final AppConfig config;
    private final HttpClient httpClient;

    public OpenWeatherClient(AppConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public WeatherObservation fetchCurrentWeather(double latitude, double longitude) {
        ensureConfigured();
        String url = "%s/data/2.5/weather?lat=%s&lon=%s&appid=%s&units=metric".formatted(
                config.openWeatherBaseUrl(),
                latitude,
                longitude,
                config.openWeatherApiKey()
        );

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("OpenWeather request failed with status " + response.statusCode() + ": " + response.body());
            }
            return OpenWeatherJsonParser.parse(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenWeather request interrupted", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to fetch OpenWeather data", exception);
        }
    }

    private void ensureConfigured() {
        if (config.openWeatherApiKey() == null || config.openWeatherApiKey().isBlank()) {
            throw new IllegalStateException("OPENWEATHER_API_KEY must be configured for live ingestion");
        }
    }
}
