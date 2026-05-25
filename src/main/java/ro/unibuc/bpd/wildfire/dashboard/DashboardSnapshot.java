package ro.unibuc.bpd.wildfire.dashboard;

import ro.unibuc.bpd.wildfire.model.RiskPrediction;

import java.time.Instant;
import java.util.List;

public record DashboardSnapshot(
        int totalPredictions,
        double averageThreatScore,
        long extremeRiskCount,
        long highRiskCount,
        String topZoneName,
        String topCountry,
        double topThreatScore,
        Instant lastUpdate,
        List<RiskPrediction> predictions
) {
}

