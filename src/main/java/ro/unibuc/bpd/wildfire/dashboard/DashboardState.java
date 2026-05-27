package ro.unibuc.bpd.wildfire.dashboard;

import ro.unibuc.bpd.wildfire.model.RiskPrediction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class DashboardState {
    private static final int MAX_PREDICTIONS = 1000;

    private final List<RiskPrediction> latestPredictions = new ArrayList<>();
    private Instant lastUpdate;

    public synchronized void addPrediction(RiskPrediction prediction) {
        latestPredictions.removeIf(existing -> samePrediction(existing, prediction));
        latestPredictions.add(prediction);
        latestPredictions.sort(Comparator.comparing(RiskPrediction::predictionTime).reversed());
        if (latestPredictions.size() > MAX_PREDICTIONS) {
            latestPredictions.subList(MAX_PREDICTIONS, latestPredictions.size()).clear();
        }
        lastUpdate = Instant.now();
    }

    public synchronized DashboardSnapshot snapshot() {
        List<RiskPrediction> copy = latestPredictions.stream()
                .map(DashboardState::normalizedPrediction)
                .toList();
        long extremeCount = copy.stream().filter(prediction -> "EXTREME".equals(prediction.riskLevel())).count();
        long highCount = copy.stream().filter(prediction -> "HIGH".equals(prediction.riskLevel())).count();
        double averageThreat = copy.stream()
                .mapToDouble(RiskPrediction::threatScore)
                .average()
                .orElse(0.0);

        RiskPrediction topPrediction = copy.stream()
                .max(Comparator.comparingDouble(RiskPrediction::threatScore))
                .orElse(null);

        return new DashboardSnapshot(
                copy.size(),
                round(averageThreat),
                extremeCount,
                highCount,
                topPrediction == null ? null : topPrediction.zoneName(),
                topPrediction == null ? null : topPrediction.country(),
                topPrediction == null ? 0.0 : round(topPrediction.threatScore()),
                lastUpdate,
                copy
        );
    }

    private static boolean samePrediction(RiskPrediction left, RiskPrediction right) {
        return left.cellId().equals(right.cellId())
                && left.predictionTime().equals(right.predictionTime());
    }

    private static RiskPrediction normalizedPrediction(RiskPrediction prediction) {
        return new RiskPrediction(
                prediction.predictionTime(),
                prediction.latitude(),
                prediction.longitude(),
                prediction.cellId(),
                prediction.zoneName(),
                prediction.country(),
                prediction.threatScore(),
                riskLevel(prediction.threatScore()),
                prediction.predictedSpreadBearing(),
                prediction.estimatedSpreadVelocityKph(),
                prediction.confidenceIndex(),
                prediction.weatherFallbackUsed()
        );
    }

    private static String riskLevel(double threatScore) {
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
        return Double.parseDouble(String.format(Locale.US, "%.1f", value));
    }
}
