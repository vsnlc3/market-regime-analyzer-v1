package com.marketregimeanalyzer.analysis;

import com.marketregimeanalyzer.api.ApiRequest;
import com.marketregimeanalyzer.api.dto.AnalysisResponse;
import com.marketregimeanalyzer.feature.FeatureService;
import com.marketregimeanalyzer.feature.FeatureSnapshot;
import com.marketregimeanalyzer.grid.GridSuitabilityResult;
import com.marketregimeanalyzer.grid.GridSuitabilityService;
import com.marketregimeanalyzer.indicator.IndicatorService;
import com.marketregimeanalyzer.indicator.IndicatorSnapshot;
import com.marketregimeanalyzer.marketdata.CandleQueryService;
import com.marketregimeanalyzer.model.Candle;
import com.marketregimeanalyzer.regime.MarketRegime;
import com.marketregimeanalyzer.regime.RegimeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalysisService {

    private final CandleQueryService candleQueryService;
    private final IndicatorService indicatorService;
    private final FeatureService featureService;
    private final RegimeService regimeService;
    private final GridSuitabilityService gridSuitabilityService;

    public AnalysisService(
            CandleQueryService candleQueryService,
            IndicatorService indicatorService,
            FeatureService featureService,
            RegimeService regimeService,
            GridSuitabilityService gridSuitabilityService
    ) {
        this.candleQueryService = candleQueryService;
        this.indicatorService = indicatorService;
        this.featureService = featureService;
        this.regimeService = regimeService;
        this.gridSuitabilityService = gridSuitabilityService;
    }

    public AnalysisResponse analyze(ApiRequest request) {
        List<Candle> candles = candleQueryService.getConfirmedCandles(request);
        Candle latestCandle = candles.getLast();

        IndicatorSnapshot indicators = indicatorService.calculate(candles, latestCandle.closeTime());
        FeatureSnapshot features = featureService.calculate(candles, latestCandle.closeTime());
        MarketRegime regime = regimeService.determine(features);
        GridSuitabilityResult gridSuitability = gridSuitabilityService.calculate(features, regime);

        return AnalysisResponse.from(
                request.symbol(),
                request.timeframe(),
                request.window(),
                latestCandle,
                indicators,
                features,
                regime,
                gridSuitability
        );
    }
}
