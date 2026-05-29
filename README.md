# Wildfire Expansion and Risk Predictor

Streaming project for estimating wildfire risk and likely spread direction from fire and weather data.

## Team

- Tatoiu Tea-Eliza
- Grigore Andreea-Isabela
- Potlog Lorena-Elena

## Demo 

Two screen recordings of the dashboard are stored in [`src/main/resources/docs/demo/`](src/main/resources/docs/demo/):

| Title | What it shows | File |
|-------|---------------|------|
| **Live Data Demo** | Real-time pipeline: NASA FIRMS + OpenWeather → Kafka → Flink → dashboard | [`demo-real-data.mp4`](src/main/resources/docs/demo/demo-real-data.mp4) |
| **Mock Seeder Demo** | Scripted demo with `WildfireSeederApp` — no API keys, predictable risk transitions | [`demo-seeder.mp4`](src/main/resources/docs/demo/demo-seeder.mp4) |


## What This Project Does

The project has three main runtime pieces:

- `WildfireLiveIngestionApp`
  Polls NASA FIRMS and OpenWeather, then writes live fire and weather events into Kafka.
- `WildfireRiskJob`
  Runs in Flink, consumes Kafka fire/weather events, calculates wildfire risk predictions, and publishes them to the `wildfire-risk-predictions` Kafka topic.
- `PredictionDashboardServer`
  Reads prediction events from Kafka and serves the local dashboard UI on `http://localhost:7070`.

There is also one demo tool:

- `WildfireSeederApp`
  Can publish fake/demo data when you do not want to depend on live external APIs.

## End-To-End Flow

### Live mode

```text
NASA FIRMS + OpenWeatherMap
        -> Kafka
        -> Apache Flink
        -> wildfire-risk-predictions
        -> local dashboard
```

### Mock/demo mode

```text
WildfireSeederApp
        -> wildfire-risk-predictions
        -> local dashboard
```

## Kafka Topics

- `firms-hotspots`
- `weather-observations`
- `wildfire-risk-predictions`

## Configuration

Copy `.env.example` to `.env`, then fill in the live keys if you want real data:

```bash
cp .env.example .env
```

Important values:

- `FIRMS_MAP_KEY`
  NASA FIRMS API key for live ingestion.
- `OPENWEATHER_API_KEY`
  OpenWeather API key for live ingestion.
- `FIRMS_AREA`
  One or more FIRMS bounding boxes.
- `INGEST_POLL_SECONDS`
  How often live ingestion polls external APIs.
- `DASHBOARD_PORT`
  Local dashboard port.

### `FIRMS_AREA` format

Each area uses:

```text
west,south,east,north
```

Multiple areas are separated with `;`.

Example:

```env
FIRMS_AREA="-10,35,5,44;5,41,20,48;19,34,30,49;12,47,25,56;-125,32,-113,42.5"
```

That means the app will poll multiple regions, for example parts of Europe and the western United States.

## Requirements

- Java 17+
- Maven
- Docker and Docker Compose
- NASA FIRMS API key for live ingestion
- OpenWeatherMap API key for live ingestion

## Common Setup

### 1. Start infrastructure

```bash
docker compose up -d
```

### 2. Build the project

```bash
mvn clean package
```

### 3. Open the dashboard

The dashboard URL is:

```text
http://localhost:7070
```

## Live Streaming Runbook

Use this when you want real live data.

### 1. Start infrastructure

```bash
docker compose up -d
```

### 2. Build the jar

```bash
mvn clean package
```

### 3. Copy Natural Earth boundary data into the Flink containers

This improves zone/country matching.

```bash
docker exec wildfire-jobmanager mkdir -p /tmp/natural-earth
docker exec wildfire-taskmanager mkdir -p /tmp/natural-earth
docker cp data/natural-earth/. wildfire-jobmanager:/tmp/natural-earth/
docker cp data/natural-earth/. wildfire-taskmanager:/tmp/natural-earth/
```

### 4. Copy the jar to Flink

```bash
docker exec wildfire-jobmanager mkdir -p /opt/flink/usrlib
docker cp target/wildfire-streaming-1.0.0-jar-with-dependencies.jar wildfire-jobmanager:/opt/flink/usrlib/
```

### 5. Submit the Flink job

```bash
docker exec wildfire-jobmanager flink run -d \
  /opt/flink/usrlib/wildfire-streaming-1.0.0-jar-with-dependencies.jar \
  --bootstrap-servers=kafka:29092 \
  --natural-earth-admin0-path=/tmp/natural-earth/admin0/ne_10m_admin_0_countries.shp \
  --natural-earth-admin1-path=/tmp/natural-earth/admin1/ne_10m_admin_1_states_provinces.shp
```

### 6. Start the dashboard

```bash
java -cp target/wildfire-streaming-1.0.0-jar-with-dependencies.jar \
  ro.unibuc.bpd.wildfire.dashboard.PredictionDashboardServer
```

### 7. Start live ingestion

```bash
set -a
source .env
set +a
java -cp target/wildfire-streaming-1.0.0-jar-with-dependencies.jar \
  ro.unibuc.bpd.wildfire.WildfireLiveIngestionApp
```

### Notes

- The UI refreshes every 5 seconds.
- Live ingestion frequency is controlled by `INGEST_POLL_SECONDS`.
- If the dashboard looks static, it may still be refreshing correctly while waiting for fresh external events.


## Monitoring

- Flink UI: `http://localhost:8081`
- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`

## Troubleshooting

### Dashboard is up but empty

Possible causes:

- live ingestion is not running
- Flink job is not running
- the seeder mode does not match the rest of the stack
- there are no new live events for the selected region

### Dashboard refreshes but `Last dashboard update` does not move

This usually means:

- the page is refreshing
- but no new prediction event has arrived yet

