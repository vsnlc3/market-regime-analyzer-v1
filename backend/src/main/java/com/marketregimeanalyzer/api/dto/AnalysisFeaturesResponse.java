package com.marketregimeanalyzer.api.dto;

import com.marketregimeanalyzer.feature.FeatureSnapshot;

import java.math.BigDecimal;

public record AnalysisFeaturesResponse(
        BigDecimal trendStrength,
        BigDecimal volatility,
        BigDecimal rangeStability,
        BigDecimal oscillation,
        BigDecimal breakoutRisk
) {

    public static AnalysisFeaturesResponse from(FeatureSnapshot features) {
        return new AnalysisFeaturesResponse(
                features.trendStrength(),
                features.volatility(),
                features.rangeStability(),
                features.oscillation(),
                features.breakoutRisk()
        );
    }
}
