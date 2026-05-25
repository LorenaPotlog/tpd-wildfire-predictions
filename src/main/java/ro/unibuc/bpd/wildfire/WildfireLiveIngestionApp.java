package ro.unibuc.bpd.wildfire;

import ro.unibuc.bpd.wildfire.config.AppConfig;
import ro.unibuc.bpd.wildfire.ingest.FirmsApiClient;
import ro.unibuc.bpd.wildfire.ingest.LiveKafkaPublisher;
import ro.unibuc.bpd.wildfire.ingest.OpenWeatherClient;
import ro.unibuc.bpd.wildfire.model.FireHotspotEvent;
import ro.unibuc.bpd.wildfire.model.WeatherObservation;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WildfireLiveIngestionApp {
    private static final int MAX_DEDUP_KEYS = 20_000;

    private WildfireLiveIngestionApp() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromArgs(args);
        FirmsApiClient firmsApiClient = new FirmsApiClient(config);
        OpenWeatherClient openWeatherClient = new OpenWeatherClient(config);
        try (LiveKafkaPublisher publisher = new LiveKafkaPublisher(config)) {
            Map<String, Instant> seenEvents = new LinkedHashMap<>(MAX_DEDUP_KEYS, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Instant> eldest) {
                    return size() > MAX_DEDUP_KEYS;
                }
            };

            while (true) {
                try {
                    pollOnce(firmsApiClient, openWeatherClient, publisher, seenEvents);
                } catch (Exception exception) {
                    System.err.println("Live ingestion poll failed: " + exception.getMessage());
                    exception.printStackTrace(System.err);
                }
                Thread.sleep(config.ingestPollSeconds() * 1000L);
            }
        }
    }

    private static void pollOnce(
            FirmsApiClient firmsApiClient,
            OpenWeatherClient openWeatherClient,
            LiveKafkaPublisher publisher,
            Map<String, Instant> seenEvents
    ) {
        List<FireHotspotEvent> fireEvents = firmsApiClient.fetchHotspots().stream()
                .sorted(Comparator.comparing(FireHotspotEvent::acquisitionTime))
                .toList();

        Map<String, WeatherObservation> weatherCache = new HashMap<>();
        for (FireHotspotEvent fireEvent : fireEvents) {
            String eventKey = eventKey(fireEvent);
            if (seenEvents.containsKey(eventKey)) {
                continue;
            }

            publisher.publishFire(fireEvent);
            seenEvents.put(eventKey, fireEvent.acquisitionTime());

            try {
                WeatherObservation observation = weatherCache.computeIfAbsent(
                        fireEvent.cellId(),
                        ignored -> openWeatherClient.fetchCurrentWeather(fireEvent.latitude(), fireEvent.longitude())
                );
                publisher.publishWeather(observation);
            } catch (Exception exception) {
                System.err.println("Skipping weather fetch for " + fireEvent.cellId() + ": " + exception.getMessage());
            }
        }

        publisher.flush();
    }

    private static String eventKey(FireHotspotEvent event) {
        return "%s|%s|%.4f|%.4f|%.1f".formatted(
                event.source(),
                event.acquisitionTime(),
                event.latitude(),
                event.longitude(),
                event.brightness()
        );
    }
}
