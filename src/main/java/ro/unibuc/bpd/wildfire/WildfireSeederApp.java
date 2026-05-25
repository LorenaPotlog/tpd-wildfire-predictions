package ro.unibuc.bpd.wildfire;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import ro.unibuc.bpd.wildfire.config.AppConfig;
import ro.unibuc.bpd.wildfire.ingest.FirmsCsvParser;
import ro.unibuc.bpd.wildfire.ingest.OpenWeatherJsonParser;
import ro.unibuc.bpd.wildfire.model.FireHotspotEvent;
import ro.unibuc.bpd.wildfire.model.WeatherObservation;
import ro.unibuc.bpd.wildfire.serialization.JsonSerde;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

public final class WildfireSeederApp {

    private WildfireSeederApp() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromArgs(args);
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {
            seedFireEvents(producer, config.fireTopic());
            seedWeatherEvents(producer, config.weatherTopic());
            producer.flush();
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
}

