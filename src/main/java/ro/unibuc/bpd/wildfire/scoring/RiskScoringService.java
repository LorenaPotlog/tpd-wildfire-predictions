package ro.unibuc.bpd.wildfire.scoring;

import ro.unibuc.bpd.wildfire.geo.GeoUtils;
import ro.unibuc.bpd.wildfire.model.FireHotspotEvent;
import ro.unibuc.bpd.wildfire.model.RiskPrediction;
import ro.unibuc.bpd.wildfire.model.WeatherObservation;
import ro.unibuc.bpd.wildfire.model.ZoneMatch;

import java.io.Serializable;
import java.time.Duration;

public final class RiskScoringService implements Serializable {

    public RiskPrediction score(
            FireHotspotEvent currentFire,
            FireHotspotEvent previousFire,
            WeatherObservation weather,
            ZoneMatch zone,
            boolean weatherFallbackUsed
    ) {
        double heatFactor = GeoUtils.clamp((currentFire.brightness() - 300.0) / 120.0, 0.0, 1.0);
        double windFactor = GeoUtils.clamp(weather.windSpeedMetersPerSecond() / 18.0, 0.0, 1.0);
        double drynessFactor = GeoUtils.clamp((100.0 - weather.humidity()) / 100.0, 0.0, 1.0);
        double confidenceFactor = GeoUtils.clamp(currentFire.confidence() / 100.0, 0.0, 1.0);

        double historicalBearing = weather.windDirectionDegrees();
        double estimatedVelocityKph = weather.windSpeedMetersPerSecond() * 3.6;

        if (previousFire != null) {
            historicalBearing = GeoUtils.bearing(
                    previousFire.latitude(),
                    previousFire.longitude(),
                    currentFire.latitude(),
                    currentFire.longitude()
            );

            long deltaSeconds = Math.max(1L, Duration.between(previousFire.acquisitionTime(), currentFire.acquisitionTime()).getSeconds());
            double distanceKm = GeoUtils.distanceKm(
                    previousFire.latitude(),
                    previousFire.longitude(),
                    currentFire.latitude(),
                    currentFire.longitude()
            );
            estimatedVelocityKph = Math.max(estimatedVelocityKph, distanceKm / (deltaSeconds / 3600.0));
        }

        double predictedSpreadBearing = GeoUtils.weightedAngle(
                weather.windDirectionDegrees(),
                historicalBearing,
                0.7,
                0.3
        );

        double threatScore = 100.0 * (
                0.40 * heatFactor
                        + 0.25 * windFactor
                        + 0.20 * drynessFactor
                        + 0.15 * confidenceFactor
        );

        estimatedVelocityKph = estimatedVelocityKph + heatFactor * 8.0;
        double confidenceIndex = GeoUtils.clamp((confidenceFactor * 0.6) + (weatherFallbackUsed ? 0.2 : 0.4), 0.0, 1.0);

        return new RiskPrediction(
                currentFire.acquisitionTime(),
                currentFire.latitude(),
                currentFire.longitude(),
                currentFire.cellId(),
                zone.zoneName(),
                zone.country(),
                round(threatScore),
                riskLevel(threatScore),
                round(predictedSpreadBearing),
                round(estimatedVelocityKph),
                round(confidenceIndex),
                weatherFallbackUsed
        );
    }

    private static String riskLevel(double threatScore) {
        // Calibrated to the current live FIRMS/OpenWeather score distribution so upper-end live events surface clearly.
        if (threatScore >= 52.0) {
            return "EXTREME";
        }
        if (threatScore >= 45.0) {
            return "HIGH";
        }
        if (threatScore >= 30.0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
