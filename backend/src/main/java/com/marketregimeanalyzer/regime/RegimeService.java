package com.marketregimeanalyzer.regime;

import com.marketregimeanalyzer.feature.FeatureSnapshot;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RegimeService {

    public MarketRegime determine(FeatureSnapshot features) {
        if (features == null) {
            throw new IllegalArgumentException("FeatureSnapshot must not be null");
        }
        if (isExtremeUnstable(features)) {
            return MarketRegime.UNSTABLE;
        }
        if (isTrend(features)) {
            return MarketRegime.TREND;
        }
        if (isRange(features)) {
            return MarketRegime.RANGE;
        }
        return MarketRegime.UNSTABLE;
    }

    private boolean isExtremeUnstable(FeatureSnapshot features) {
        return greaterThanOrEqual(
                features.volatility(),
                RegimeSettings.EXTREME_UNSTABLE_VOLATILITY_THRESHOLD
        ) && greaterThanOrEqual(
                features.breakoutRisk(),
                RegimeSettings.EXTREME_UNSTABLE_BREAKOUT_RISK_THRESHOLD
        );
    }

    private boolean isTrend(FeatureSnapshot features) {
        return greaterThanOrEqual(features.trendStrength(), RegimeSettings.TREND_STRENGTH_THRESHOLD)
                && greaterThanOrEqual(
                features.efficiencyRatio(),
                RegimeSettings.TREND_EFFICIENCY_RATIO_THRESHOLD
        );
    }

    private boolean isRange(FeatureSnapshot features) {
        return greaterThanOrEqual(features.rangeStability(), RegimeSettings.RANGE_STABILITY_THRESHOLD)
                && greaterThanOrEqual(features.rangeStayRatio(), RegimeSettings.RANGE_STAY_RATIO_THRESHOLD)
                && greaterThanOrEqual(features.oscillation(), RegimeSettings.RANGE_OSCILLATION_THRESHOLD)
                && features.trendStrength().compareTo(RegimeSettings.RANGE_MAX_TREND_STRENGTH) < 0
                && features.breakoutRisk().compareTo(RegimeSettings.RANGE_MAX_BREAKOUT_RISK) < 0;
    }

    private boolean greaterThanOrEqual(BigDecimal value, BigDecimal threshold) {
        return value.compareTo(threshold) >= 0;
    }
}
