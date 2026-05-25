# Wildfire Expansion and Risk Predictor

Streaming project for estimating wildfire risk and likely spread direction from live fire and weather data.

## Team

- Tatoiu Tea-Eliza
- Grigore Andreea-Isabela
- Potlog Lorena-Elena

## Overview

The pipeline turns external live APIs into Kafka streams, processes them with Apache Flink, and exposes the resulting risk predictions through a local dashboard.

```text
NASA FIRMS + OpenWeatherMap
        -> Kafka
        -> Apache Flink
        -> wildfire-risk-predictions
        -> local dashboard
```

## Data Sources

- NASA FIRMS active fire detections
- OpenWeatherMap current weather data
- Natural Earth shapefiles for geographic zone resolution

## Main Components

- `WildfireLiveIngestionApp`: polls NASA FIRMS, fetches weather, and publishes events to Kafka.
- `WildfireRiskJob`: consumes fire and weather streams in Flink and emits risk predictions.
- `PredictionDashboardServer`: reads prediction events from Kafka and serves the local UI.
- `WildfireSeederApp`: publishes bundled sample data for offline demos.

## Kafka Topics

- `firms-hotspots`
- `weather-observations`
- `wildfire-risk-predictions`

## Configuration

Copy `.env.example` or export the required variables manually:

```bash
export FIRMS_MAP_KEY="your-firms-map-key"
export OPENWEATHER_API_KEY="your-openweather-api-key"
export FIRMS_AREA="20,43,30,49;19,34,30,42;6,36,19,47;-10,35,5,44;12,47,25,56"
export KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
```

`FIRMS_AREA` uses:

```text
west,south,east,north
```

Multiple areas can be separated with `;`. The example above polls Romania, Greece, Italy, Spain/Portugal, and Central Europe.

## Run Live Pipeline

Start infrastructure:

```bash
docker compose up -d
```

Build the project:

```bash
mvn clean package
```

Submit the Flink job:

```bash
docker exec wildfire-jobmanager mkdir -p /opt/flink/usrlib
docker cp target/wildfire-streaming-1.0.0-jar-with-dependencies.jar wildfire-jobmanager:/opt/flink/usrlib/
docker exec wildfire-jobmanager flink run -d /opt/flink/usrlib/wildfire-streaming-1.0.0-jar-with-dependencies.jar --bootstrap-servers=kafka:29092
```

Start live ingestion:

```bash
java -cp target/wildfire-streaming-1.0.0-jar-with-dependencies.jar ro.unibuc.bpd.wildfire.WildfireLiveIngestionApp
```

Start the dashboard in another terminal:

```bash
java -cp target/wildfire-streaming-1.0.0-jar-with-dependencies.jar ro.unibuc.bpd.wildfire.dashboard.PredictionDashboardServer
```

Open:

```text
http://localhost:7070
```

## Offline Demo

Use the seeder when API keys are unavailable or live data is empty:

```bash
java -cp target/wildfire-streaming-1.0.0-jar-with-dependencies.jar ro.unibuc.bpd.wildfire.WildfireSeederApp
```

The seeder publishes sample fire and weather events to the same Kafka topics used by the live pipeline.

## Monitoring

- Flink UI: `http://localhost:8081`
- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`

## Requirements

- Java 17+
- Maven
- Docker and Docker Compose
- NASA FIRMS API key for live ingestion
- OpenWeatherMap API key for live ingestion

