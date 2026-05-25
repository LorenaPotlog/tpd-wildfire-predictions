package ro.unibuc.bpd.wildfire.model;

import java.io.Serializable;
import java.time.Instant;

public class RiskPrediction implements Serializable {
    private Instant predictionTime;
    private double latitude;
    private double longitude;
    private String cellId;
    private String zoneName;
    private String country;
    private double threatScore;
    private String riskLevel;
    private double predictedSpreadBearing;
    private double estimatedSpreadVelocityKph;
    private double confidenceIndex;
    private boolean weatherFallbackUsed;

    public RiskPrediction() {
    }

    public RiskPrediction(
            Instant predictionTime,
            double latitude,
            double longitude,
            String cellId,
            String zoneName,
            String country,
            double threatScore,
            String riskLevel,
            double predictedSpreadBearing,
            double estimatedSpreadVelocityKph,
            double confidenceIndex,
            boolean weatherFallbackUsed
    ) {
        this.predictionTime = predictionTime;
        this.latitude = latitude;
        this.longitude = longitude;
        this.cellId = cellId;
        this.zoneName = zoneName;
        this.country = country;
        this.threatScore = threatScore;
        this.riskLevel = riskLevel;
        this.predictedSpreadBearing = predictedSpreadBearing;
        this.estimatedSpreadVelocityKph = estimatedSpreadVelocityKph;
        this.confidenceIndex = confidenceIndex;
        this.weatherFallbackUsed = weatherFallbackUsed;
    }

    public Instant predictionTime() {
        return predictionTime;
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    public String cellId() {
        return cellId;
    }

    public String zoneName() {
        return zoneName;
    }

    public String country() {
        return country;
    }

    public double threatScore() {
        return threatScore;
    }

    public String riskLevel() {
        return riskLevel;
    }

    public double predictedSpreadBearing() {
        return predictedSpreadBearing;
    }

    public double estimatedSpreadVelocityKph() {
        return estimatedSpreadVelocityKph;
    }

    public double confidenceIndex() {
        return confidenceIndex;
    }

    public boolean weatherFallbackUsed() {
        return weatherFallbackUsed;
    }

    public Instant getPredictionTime() {
        return predictionTime;
    }

    public void setPredictionTime(Instant predictionTime) {
        this.predictionTime = predictionTime;
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

    public String getCellId() {
        return cellId;
    }

    public void setCellId(String cellId) {
        this.cellId = cellId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public double getThreatScore() {
        return threatScore;
    }

    public void setThreatScore(double threatScore) {
        this.threatScore = threatScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public double getPredictedSpreadBearing() {
        return predictedSpreadBearing;
    }

    public void setPredictedSpreadBearing(double predictedSpreadBearing) {
        this.predictedSpreadBearing = predictedSpreadBearing;
    }

    public double getEstimatedSpreadVelocityKph() {
        return estimatedSpreadVelocityKph;
    }

    public void setEstimatedSpreadVelocityKph(double estimatedSpreadVelocityKph) {
        this.estimatedSpreadVelocityKph = estimatedSpreadVelocityKph;
    }

    public double getConfidenceIndex() {
        return confidenceIndex;
    }

    public void setConfidenceIndex(double confidenceIndex) {
        this.confidenceIndex = confidenceIndex;
    }

    public boolean isWeatherFallbackUsed() {
        return weatherFallbackUsed;
    }

    public void setWeatherFallbackUsed(boolean weatherFallbackUsed) {
        this.weatherFallbackUsed = weatherFallbackUsed;
    }
}
