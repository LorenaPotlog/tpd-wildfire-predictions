# Wildfire Expansion and Risk Predictor

Streaming project for estimating **where a wildfire is going next**, not only where it has already been detected.

## Team

- Tatoiu Tea-Eliza
- Grigore Andreea-Isabela
- Potlog Lorena-Elena

## What is actually streaming now

The project now has two live components:

1. `WildfireLiveIngestionApp`
   - polls **NASA FIRMS** for real hotspots
   - fetches real weather from **OpenWeather**
   - publishes both continuously to Kafka
2. `WildfireRiskJob`
   - consumes Kafka streams in Flink
   - joins fire and weather by geo-cell
   - emits continuous risk predictions

This means the pipeline is:

`FIRMS API -> OpenWeather API -> Kafka -> Flink -> risk predictions`

The ingestion side is not webhook-based because the upstream datasets are API / downloadable feeds, so the project implements **continuous polling ingestion** and turns those feeds into streams.

## Data sources used

- NASA FIRMS active fire data
- OpenWeather current weather API
- Natural Earth Admin 0 / Admin 1 shapefiles for real zone resolution

## Output fields

Each prediction includes:

- `threat_score`
- `risk_level`
- `predicted_spread_bearing`
- `estimated_spread_velocity_kph`
- `zone_name`
- `country`
- `weather_fallback_used`

## Architecture

### Ingestion

- `FirmsApiClient` fetches live hotspot CSV data
- `OpenWeatherClient` fetches current wind and humidity for hotspot coordinates
- `LiveKafkaPublisher` pushes normalized JSON events to Kafka topics
- `WildfireLiveIngestionApp` deduplicates repeated FIRMS observations across polling rounds

### Stream processing

- `WildfireRiskJob` consumes `firms-hotspots` and `weather-observations`
- Flink keeps latest weather per cell and previous fire event per cell
- `RiskScoringService` computes spread direction, spread speed, score, and severity

### Geospatial zone resolution

- `ZoneResolver` loads real Natural Earth shapefiles when configured
- resolution order is `Admin 1 -> Admin 0 -> fallback bundled boxes`
- fallback exists only so the job can still start if shapefiles are missing

## Environment

Copy `.env.example` and set real credentials:

- `FIRMS_MAP_KEY`
- `OPENWEATHER_API_KEY`
- `FIRMS_AREA`
- `NATURAL_EARTH_ADMIN0_PATH`
- `NATURAL_EARTH_ADMIN1_PATH`
- `DASHBOARD_PORT`

Example `FIRMS_AREA` for Romania:

```text
20,43,30,49
```

## Run flow

### 1. Download Natural Earth shapefiles

```bash
bash scripts/download-natural-earth.sh
```

### 2. Start local infrastructure

```bash
docker compose up -d
```

### 3. Build the jar

```bash
mvn clean package
```

### 4. Start live ingestion

```bash
java -cp target/wildfire-streaming-1.0.0-jar-with-dependencies.jar ro.unibuc.bpd.wildfire.WildfireLiveIngestionApp
```

### 5. Run the Flink job

```bash
docker cp target/wildfire-streaming-1.0.0-jar-with-dependencies.jar wildfire-jobmanager:/opt/flink/usrlib/
docker exec wildfire-jobmanager flink run /opt/flink/usrlib/wildfire-streaming-1.0.0-jar-with-dependencies.jar
```

### 6. Start the dashboard

```bash
java -cp target/wildfire-streaming-1.0.0-jar-with-dependencies.jar ro.unibuc.bpd.wildfire.dashboard.PredictionDashboardServer
```

Open:

```text
http://localhost:7070
```

## Topics

- `firms-hotspots`
- `weather-observations`
- `wildfire-risk-predictions`

## Important note on “real streaming”

This is **real streaming inside your platform** and **live-source ingestion at the edge**.

- FIRMS and OpenWeather are queried continuously from official live APIs.
- Kafka turns those live pulls into append-only event streams.
- Flink processes those streams continuously.

That is the correct architecture when upstream providers expose APIs instead of native Kafka topics.

## Optional fallback

`WildfireSeederApp` still exists for demos without API keys, but it is no longer the primary ingestion path.

## What is needed to run it end-to-end

- Java 17 or newer
- Maven installed locally
- Docker and Docker Compose
- a valid NASA FIRMS `MAP_KEY`
- a valid OpenWeather API key
- Natural Earth shapefiles downloaded locally

Without those, the architecture and code are in place, but the live pipeline cannot be executed against real data sources.
