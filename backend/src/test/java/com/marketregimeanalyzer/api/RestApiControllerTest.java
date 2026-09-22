package com.marketregimeanalyzer.api;

import com.marketregimeanalyzer.analysis.AnalysisService;
import com.marketregimeanalyzer.api.dto.AnalysisFeaturesResponse;
import com.marketregimeanalyzer.api.dto.AnalysisIndicatorsResponse;
import com.marketregimeanalyzer.api.dto.AnalysisResponse;
import com.marketregimeanalyzer.marketdata.CandleQueryService;
import com.marketregimeanalyzer.model.Candle;
import com.marketregimeanalyzer.model.Timeframe;
import com.marketregimeanalyzer.grid.GridSuitabilityLevel;
import com.marketregimeanalyzer.regime.MarketRegime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestApiControllerTest {

    private AnalysisService analysisService;
    private CandleQueryService candleQueryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        analysisService = mock(AnalysisService.class);
        candleQueryService = mock(CandleQueryService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new AnalysisController(new ApiRequestParser(), analysisService),
                        new CandleController(new ApiRequestParser(), candleQueryService)
                )
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void analysisUsesDefaultTimeframeAndWindowAndReturnsDto() throws Exception {
        ApiRequest request = new ApiRequest("BTC", Timeframe.ONE_HOUR, 168);
        when(analysisService.analyze(request)).thenReturn(analysisResponse());

        mockMvc.perform(get("/api/v1/analysis/btc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BTC"))
                .andExpect(jsonPath("$.timeframe").value("1h"))
                .andExpect(jsonPath("$.window").value(168))
                .andExpect(jsonPath("$.currentPrice").value("112430.00"))
                .andExpect(jsonPath("$.dataAsOf").value("2026-09-22T02:30:00Z"))
                .andExpect(jsonPath("$.features.trendStrength").value(18.0))
                .andExpect(jsonPath("$.indicators.ema20").value("112430.00"));

        verify(analysisService).analyze(request);
    }

    @Test
    void candleApiNormalizesSymbolAndReturnsChartDto() throws Exception {
        ApiRequest request = new ApiRequest("ETH", Timeframe.FIFTEEN_MINUTES, 50);
        when(candleQueryService.getConfirmedCandles(request)).thenReturn(List.of(candle()));

        mockMvc.perform(get("/api/v1/candles/eTh")
                        .queryParam("timeframe", "15m")
                        .queryParam("window", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].openTime").value("2026-09-22T02:00:00Z"))
                .andExpect(jsonPath("$[0].closeTime").value("2026-09-22T02:14:59Z"))
                .andExpect(jsonPath("$[0].open").value("112400.00"))
                .andExpect(jsonPath("$[0].volume").value("12.3456"));

        verify(candleQueryService).getConfirmedCandles(request);
    }

    @Test
    void rejectsUnsupportedSymbolWithBadRequestError() throws Exception {
        mockMvc.perform(get("/api/v1/analysis/DOGE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SYMBOL"))
                .andExpect(jsonPath("$.message").value("Unsupported symbol: DOGE"));

        verifyNoInteractions(analysisService);
    }

    @Test
    void rejectsUnsupportedTimeframeWithBadRequestError() throws Exception {
        mockMvc.perform(get("/api/v1/analysis/BTC")
                        .queryParam("timeframe", "5m"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIMEFRAME"));

        verifyNoInteractions(analysisService);
    }

    @Test
    void rejectsWindowOutsideSupportedRangeWithBadRequestError() throws Exception {
        mockMvc.perform(get("/api/v1/candles/BTC")
                        .queryParam("window", "49"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WINDOW"));

        verifyNoInteractions(candleQueryService);
    }

    @Test
    void rejectsNonNumericWindowWithBadRequestError() throws Exception {
        mockMvc.perform(get("/api/v1/candles/BTC")
                        .queryParam("window", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WINDOW"));

        verifyNoInteractions(candleQueryService);
    }

    @Test
    void mapsMarketDataErrorsToDefinedStatusAndCode() throws Exception {
        when(candleQueryService.getConfirmedCandles(any()))
                .thenThrow(new ApiException(
                        org.springframework.http.HttpStatus.BAD_GATEWAY,
                        "MARKET_DATA_UNAVAILABLE",
                        "Market data is currently unavailable"
                ));

        mockMvc.perform(get("/api/v1/candles/BTC"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("MARKET_DATA_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Market data is currently unavailable"));
    }

    private static AnalysisResponse analysisResponse() {
        return new AnalysisResponse(
                "BTC",
                "1h",
                168,
                "112430.00",
                Instant.parse("2026-09-22T02:30:00Z"),
                MarketRegime.RANGE,
                86,
                GridSuitabilityLevel.HIGH,
                new AnalysisFeaturesResponse(
                        new BigDecimal("18.0"),
                        new BigDecimal("64.0"),
                        new BigDecimal("84.0"),
                        new BigDecimal("91.0"),
                        new BigDecimal("21.0")
                ),
                new AnalysisIndicatorsResponse(
                        new BigDecimal("16.4"),
                        new BigDecimal("1.2"),
                        new BigDecimal("0.18"),
                        "112430.00",
                        "112180.00",
                        new BigDecimal("91.0"),
                        14,
                        2
                ),
                List.of("Market regime is RANGE, which is favorable for grid trading.")
        );
    }

    private static Candle candle() {
        return new Candle(
                "HYPERLIQUID",
                "ETH",
                Timeframe.FIFTEEN_MINUTES,
                Instant.parse("2026-09-22T02:00:00Z"),
                Instant.parse("2026-09-22T02:14:59Z"),
                new BigDecimal("112400.00"),
                new BigDecimal("112500.00"),
                new BigDecimal("112300.00"),
                new BigDecimal("112450.00"),
                new BigDecimal("12.3456"),
                10L
        );
    }
}
