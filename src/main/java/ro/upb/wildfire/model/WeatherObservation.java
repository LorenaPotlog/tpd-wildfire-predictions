package ro.upb.wildfire.model;

import java.io.Serializable;
import java.time.Instant;

public class WeatherObservation implements Serializable {
    private double latitude;
    private double longitude;
    private double windSpeedMetersPerSecond;
    private double windDirectionDegrees;
    private double humidity;
    private Instant observationTime;
    private String provider;
    private String cellId;

    public WeatherObservation() {
    }

    public WeatherObservation(
            double latitude,
            double longitude,
            double windSpeedMetersPerSecond,
            double windDirectionDegrees,
            double humidity,
            Instant observationTime,
            String provider,
            String cellId
    ) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.windSpeedMetersPerSecond = windSpeedMetersPerSecond;
        this.windDirectionDegrees = windDirectionDegrees;
        this.humidity = humidity;
        this.observationTime = observationTime;
        this.provider = provider;
        this.cellId = cellId;
    }

    public static WeatherObservation fallbackFor(FireHotspotEvent fireEvent) {
        return new WeatherObservation(
                fireEvent.latitude(),
                fireEvent.longitude(),
                2.0,
                0.0,
                35.0,
                fireEvent.acquisitionTime(),
                "synthetic-fallback",
                fireEvent.cellId()
        );
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    public double windSpeedMetersPerSecond() {
        return windSpeedMetersPerSecond;
    }

    public double windDirectionDegrees() {
        return windDirectionDegrees;
    }

    public double humidity() {
        return humidity;
    }

    public Instant observationTime() {
        return observationTime;
    }

    public String provider() {
        return provider;
    }

    public String cellId() {
        return cellId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getWindSpeedMetersPerSecond() {
        return windSpeedMetersPerSecond;
    }

    public void setWindSpeedMetersPerSecond(double windSpeedMetersPerSecond) {
        this.windSpeedMetersPerSecond = windSpeedMetersPerSecond;
    }

    public double getWindDirectionDegrees() {
        return windDirectionDegrees;
    }

    public void setWindDirectionDegrees(double windDirectionDegrees) {
        this.windDirectionDegrees = windDirectionDegrees;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public Instant getObservationTime() {
        return observationTime;
    }

    public void setObservationTime(Instant observationTime) {
        this.observationTime = observationTime;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCellId() {
        return cellId;
    }

    public void setCellId(String cellId) {
        this.cellId = cellId;
    }
}
