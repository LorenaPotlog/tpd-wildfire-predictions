package ro.unibuc.bpd.wildfire.ingest;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import ro.unibuc.bpd.wildfire.config.AppConfig;
import ro.unibuc.bpd.wildfire.model.FireHotspotEvent;
import ro.unibuc.bpd.wildfire.model.WeatherObservation;
import ro.unibuc.bpd.wildfire.serialization.JsonSerde;

import java.util.Properties;

public final class LiveKafkaPublisher implements AutoCloseable {
    private final AppConfig config;
    private final KafkaProducer<String, String> producer;

    public LiveKafkaPublisher(AppConfig config) {
        this.config = config;
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        this.producer = new KafkaProducer<>(properties);
    }

    public void publishFire(FireHotspotEvent event) {
        producer.send(new ProducerRecord<>(config.fireTopic(), event.cellId(), JsonSerde.toJson(event)));
    }

    public void publishWeather(WeatherObservation observation) {
        producer.send(new ProducerRecord<>(config.weatherTopic(), observation.cellId(), JsonSerde.toJson(observation)));
    }

    public void flush() {
        producer.flush();
    }

    @Override
    public void close() {
        producer.close();
    }
}

