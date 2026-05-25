package ro.unibuc.bpd.wildfire.ingest;

import ro.unibuc.bpd.wildfire.config.AppConfig;
import ro.unibuc.bpd.wildfire.model.FireHotspotEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public final class FirmsApiClient {
    private final AppConfig config;
    private final HttpClient httpClient;

    public FirmsApiClient(AppConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public List<FireHotspotEvent> fetchHotspots() {
        ensureConfigured();
        String url = "%s/api/area/csv/%s/%s/%s/%d".formatted(
                config.firmsBaseUrl(),
                config.firmsMapKey(),
                config.firmsSource(),
                config.firmsArea(),
                config.firmsLookbackDays()
        );

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "text/csv")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("FIRMS request failed with status " + response.statusCode() + ": " + response.body());
            }

            try (BufferedReader reader = new BufferedReader(new StringReader(response.body()))) {
                return FirmsCsvParser.parse(reader);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("FIRMS request interrupted", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to fetch live FIRMS data", exception);
        }
    }

    private void ensureConfigured() {
        if (config.firmsMapKey() == null || config.firmsMapKey().isBlank()) {
            throw new IllegalStateException("FIRMS_MAP_KEY must be configured for live ingestion");
        }
    }
}
