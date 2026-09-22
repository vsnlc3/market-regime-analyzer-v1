package com.marketregimeanalyzer.regime;

import com.marketregimeanalyzer.feature.FeatureSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RegimeServiceTest {

    private final RegimeService regimeService = new RegimeService();

    @Test
    void extremeUnstableHasPriorityOverTrend() {
        FeatureSnapshot features = features(80, 90, 90, 0.70, 80, 80, 80);

        assertThat(regimeService.determine(features)).isEqualTo(MarketRegime.UNSTABLE);
    }

    @Test
    void classifiesTrendWhenTrendGatesAreMet() {
        FeatureSnapshot features = features(65, 20, 20, 0.45, 20, 20, 20);

        assertThat(regimeService.determine(features)).isEqualTo(MarketRegime.TREND);
    }

    @Test
    void classifiesRangeWhenAllRangeConditionsAreMet() {
        FeatureSnapshot features = features(59.99, 20, 20, 0.30, 60, 70, 50);

        assertThat(regimeService.determine(features)).isEqualTo(MarketRegime.RANGE);
    }

    @Test
    void fallsBackToUnstableForAmbiguousFeatures() {
        FeatureSnapshot features = features(60, 20, 20, 0.30, 80, 80, 50);

        assertThat(regimeService.determine(features)).isEqualTo(MarketRegime.UNSTABLE);
    }

    @Test
    void rangeUsesExclusiveUpperBoundsForTrendAndBreakoutRisk() {
        FeatureSnapshot trendBoundary = features(60, 20, 20, 0.30, 60, 70, 50);
        FeatureSnapshot breakoutBoundary = features(59.99, 20, 60, 0.30, 60, 70, 50);

        assertThat(regimeService.determine(trendBoundary)).isEqualTo(MarketRegime.UNSTABLE);
        assertThat(regimeService.determine(breakoutBoundary)).isEqualTo(MarketRegime.UNSTABLE);
    }

    @Test
    void usesInclusiveThresholdsForExtremeUnstableAndTrend() {
        FeatureSnapshot extremeBoundary = features(65, 80, 85, 0.45, 60, 70, 50);
        FeatureSnapshot trendBoundary = features(65, 20, 20, 0.45, 20, 20, 20);

        assertThat(regimeService.determine(extremeBoundary)).isEqualTo(MarketRegime.UNSTABLE);
        assertThat(regimeService.determine(trendBoundary)).isEqualTo(MarketRegime.TREND);
    }

    private static FeatureSnapshot features(
            double trendStrength,
            double volatility,
            double breakoutRisk,
            double efficiencyRatio,
            double rangeStability,
            double rangeStayRatio,
            double oscillation
    ) {
        return new FeatureSnapshot(
                BigDecimal.valueOf(trendStrength),
                BigDecimal.valueOf(volatility),
                BigDecimal.valueOf(efficiencyRatio),
                BigDecimal.valueOf(rangeStability),
                BigDecimal.valueOf(rangeStayRatio),
                0,
                BigDecimal.valueOf(oscillation),
                0,
                BigDecimal.valueOf(breakoutRisk)
        );
    }
}
