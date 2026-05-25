package ro.unibuc.bpd.wildfire.dashboard;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import ro.unibuc.bpd.wildfire.config.AppConfig;
import ro.unibuc.bpd.wildfire.model.RiskPrediction;
import ro.unibuc.bpd.wildfire.serialization.JsonSerde;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;

public final class PredictionDashboardServer {

    private PredictionDashboardServer() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromArgs(args);
        DashboardState dashboardState = new DashboardState();

        startKafkaConsumer(config, dashboardState);
        startHttpServer(config, dashboardState);
    }

    private static void startKafkaConsumer(AppConfig config, DashboardState dashboardState) {
        Thread consumerThread = new Thread(() -> consumePredictions(config, dashboardState), "prediction-dashboard-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    private static void consumePredictions(AppConfig config, DashboardState dashboardState) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "wildfire-dashboard");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(List.of(config.predictionTopic()));
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> record : records) {
                    RiskPrediction prediction = JsonSerde.mapper().readValue(record.value(), RiskPrediction.class);
                    dashboardState.addPrediction(prediction);
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Dashboard Kafka consumer stopped", exception);
        }
    }

    private static void startHttpServer(AppConfig config, DashboardState dashboardState) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(config.dashboardPort()), 0);
        server.createContext("/api/predictions", exchange -> writeJson(exchange, JsonSerde.toJson(dashboardState.snapshot())));
        server.createContext("/api/health", exchange -> writeJson(exchange, "{\"status\":\"ok\"}"));
        server.createContext("/", PredictionDashboardServer::serveStaticAsset);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Dashboard available at http://localhost:" + config.dashboardPort());
    }

    private static void serveStaticAsset(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String resourcePath = switch (path) {
            case "/", "" -> "/ui/index.html";
            case "/styles.css" -> "/ui/styles.css";
            case "/app.js" -> "/ui/app.js";
            default -> null;
        };

        if (resourcePath == null) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        try (InputStream stream = PredictionDashboardServer.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            byte[] bytes = stream.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", contentType(resourcePath));
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        }
    }

    private static void writeJson(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static String contentType(String resourcePath) {
        if (resourcePath.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (resourcePath.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        return "text/html; charset=utf-8";
    }
}

