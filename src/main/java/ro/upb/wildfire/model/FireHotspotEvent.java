package ro.upb.wildfire.model;

import java.io.Serializable;
import java.time.Instant;

public class FireHotspotEvent implements Serializable {
    private double latitude;
    private double longitude;
    private double brightness;
    private double confidence;
    private Instant acquisitionTime;
    private String source;
    private String cellId;

    public FireHotspotEvent() {
    }

    public FireHotspotEvent(
            double latitude,
            double longitude,
            double brightness,
            double confidence,
            Instant acquisitionTime,
            String source,
            String cellId
    ) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.brightness = brightness;
        this.confidence = confidence;
        this.acquisitionTime = acquisitionTime;
        this.source = source;
        this.cellId = cellId;
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    public double brightness() {
        return brightness;
    }

    public double confidence() {
        return confidence;
    }

    public Instant acquisitionTime() {
        return acquisitionTime;
    }

    public String source() {
        return source;
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
}
