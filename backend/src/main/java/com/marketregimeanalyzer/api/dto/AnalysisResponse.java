package com.marketregimeanalyzer.api.dto;

import com.marketregimeanalyzer.feature.FeatureSnapshot;
import com.marketregimeanalyzer.grid.GridSuitabilityLevel;
import com.marketregimeanalyzer.grid.GridSuitabilityResult;
import com.marketregimeanalyzer.indicator.IndicatorSnapshot;
import com.marketregimeanalyzer.model.Candle;
import com.marketregimeanalyzer.model.Timeframe;
import com.marketregimeanalyzer.regime.MarketRegime;

import java.time.Instant;
import java.util.List;

public record AnalysisResponse(
        String symbol,
        String timeframe,
        int window,
        String currentPrice,
        Instant dataAsOf,
        MarketRegime regime,
        int gridSuitability,
        GridSuitabilityLevel gridSuitabilityLevel,
        AnalysisFeaturesResponse features,
        AnalysisIndicatorsResponse indicators,
        List<String> reasons
) {

    public AnalysisResponse {
        reasons = List.copyOf(reasons);
    }

    public static AnalysisResponse from(
            String symbol,
            Timeframe timeframe,
            int window,
            Candle latestCandle,
            IndicatorSnapshot indicators,
            FeatureSnapshot features,
            MarketRegime regime,
            GridSuitabilityResult gridSuitability
    ) {
        return new AnalysisResponse(
                symbol,
                timeframe.value(),
                window,
                latestCandle.close().toPlainString(),
                latestCandle.closeTime(),
                regime,
                gridSuitability.score(),
                gridSuitability.level(),
                AnalysisFeaturesResponse.from(features),
                AnalysisIndicatorsResponse.from(indicators, features),
                gridSuitability.reasons()
        );
    }
}
