package com.marketregimeanalyzer.api.dto;

import com.marketregimeanalyzer.feature.FeatureSnapshot;
import com.marketregimeanalyzer.indicator.IndicatorSnapshot;

import java.math.BigDecimal;

public record AnalysisIndicatorsResponse(
        BigDecimal adx,
        BigDecimal atrPct,
        BigDecimal efficiencyRatio,
        String ema20,
        String ema50,
        BigDecimal rangeStayRatio,
        int reversalCount,
        int rangeBreakCount
) {

    public static AnalysisIndicatorsResponse from(
            IndicatorSnapshot indicators,
            FeatureSnapshot features
    ) {
        return new AnalysisIndicatorsResponse(
                indicators.adx(),
                indicators.atrPercent(),
                features.efficiencyRatio(),
                indicators.ema20().toPlainString(),
                indicators.ema50().toPlainString(),
                features.rangeStayRatio(),
                features.reversalCount(),
                features.rangeBreakCount()
        );
    }
}
