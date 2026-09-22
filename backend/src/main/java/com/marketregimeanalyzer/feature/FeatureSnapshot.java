package com.marketregimeanalyzer.feature;

import java.math.BigDecimal;

public record FeatureSnapshot(
        BigDecimal trendStrength,
        BigDecimal volatility,
        BigDecimal efficiencyRatio,
        BigDecimal rangeStability,
        BigDecimal rangeStayRatio,
        int reversalCount,
        BigDecimal oscillation,
        int rangeBreakCount,
        BigDecimal breakoutRisk
) {
}
