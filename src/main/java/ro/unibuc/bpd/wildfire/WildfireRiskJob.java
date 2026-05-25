package ro.unibuc.bpd.wildfire;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import ro.unibuc.bpd.wildfire.config.AppConfig;
import ro.unibuc.bpd.wildfire.geo.ZoneResolver;
import ro.unibuc.bpd.wildfire.model.FireHotspotEvent;
import ro.unibuc.bpd.wildfire.model.RiskPrediction;
import ro.unibuc.bpd.wildfire.model.WeatherObservation;
import ro.unibuc.bpd.wildfire.serialization.FireEventJsonMapper;
import ro.unibuc.bpd.wildfire.serialization.JsonSerde;
import ro.unibuc.bpd.wildfire.serialization.WeatherObservationJsonMapper;
import ro.unibuc.bpd.wildfire.streaming.RiskPredictionCoProcessFunction;

public final class WildfireRiskJob {

    private WildfireRiskJob() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromArgs(args);

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(config.parallelism());
        env.getConfig().disableClosureCleaner();

        KafkaSource<String> fireSource = KafkaSource.<String>builder()
                .setBootstrapServers(config.bootstrapServers())
                .setTopics(config.fireTopic())
                .setGroupId("wildfire-fire-consumer")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new org.apache.flink.api.common.serialization.SimpleStringSchema())
                .build();

        KafkaSource<String> weatherSource = KafkaSource.<String>builder()
                .setBootstrapServers(config.bootstrapServers())
                .setTopics(config.weatherTopic())
                .setGroupId("wildfire-weather-consumer")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new org.apache.flink.api.common.serialization.SimpleStringSchema())
                .build();

        DataStream<FireHotspotEvent> fireEvents = env.fromSource(
                        fireSource,
                        WatermarkStrategy.noWatermarks(),
                        "fire-kafka-source")
                .map(new FireEventJsonMapper())
                .name("parse-fire-events");

        DataStream<WeatherObservation> weatherEvents = env.fromSource(
                        weatherSource,
                        WatermarkStrategy.noWatermarks(),
                        "weather-kafka-source")
                .map(new WeatherObservationJsonMapper())
                .name("parse-weather-events");

        DataStream<RiskPrediction> predictions = fireEvents
                .keyBy(FireHotspotEvent::cellId)
                .connect(weatherEvents.keyBy(WeatherObservation::cellId))
                .process(new RiskPredictionCoProcessFunction(config))
                .name("compute-risk-predictions");

        KafkaSink<String> predictionSink = KafkaSink.<String>builder()
                .setBootstrapServers(config.bootstrapServers())
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic(config.predictionTopic())
                        .setValueSerializationSchema(new org.apache.flink.api.common.serialization.SimpleStringSchema())
                        .build())
                .build();

        predictions
                .map(JsonSerde::toJson)
                .sinkTo(predictionSink)
                .name("prediction-kafka-sink");

        predictions.print("wildfire-prediction");

        env.execute("Wildfire Expansion And Risk Predictor");
    }
}
