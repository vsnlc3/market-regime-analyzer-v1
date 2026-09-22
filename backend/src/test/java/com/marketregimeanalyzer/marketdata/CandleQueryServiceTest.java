package com.marketregimeanalyzer.marketdata;

import com.marketregimeanalyzer.api.ApiException;
import com.marketregimeanalyzer.api.ApiRequest;
import com.marketregimeanalyzer.model.Candle;
import com.marketregimeanalyzer.model.Timeframe;
import com.marketregimeanalyzer.repository.CandleEntity;
import com.marketregimeanalyzer.repository.CandleEntityMapper;
import com.marketregimeanalyzer.repository.CandleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CandleQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-22T03:00:00Z");

    private CandleRepository candleRepository;
    private MarketDataProvider marketDataProvider;
    private CandleQueryService service;

    @BeforeEach
    void setUp() {
        candleRepository = mock(CandleRepository.class);
        marketDataProvider = mock(MarketDataProvider.class);
        service = new CandleQueryService(
                marketDataProvider,
                candleRepository,
                new CandleEntityMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void usesEnoughConfirmedCandlesFromDatabaseWithoutCallingProvider() {
        ApiRequest request = new ApiRequest("BTC", Timeframe.ONE_HOUR, 50);
        List<CandleEntity> stored = entities(50, NOW.minusSeconds(50 * 3600L));
        when(candleRepository
                .findByExchangeAndSymbolAndTimeframeAndCloseTimeLessThanEqualOrderByOpenTimeDesc(
                        any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(stored);

        List<Candle> result = service.getConfirmedCandles(request);

        assertThat(result).hasSize(50).isSortedAccordingTo(java.util.Comparator.comparing(Candle::openTime));
        verifyNoInteractions(marketDataProvider);
    }

    @Test
    void fetchesAndPersistsCandlesWhenDatabaseDoesNotHaveEnoughData() {
        ApiRequest request = new ApiRequest("BTC", Timeframe.ONE_HOUR, 50);
        List<Candle> fetched = candles(50, NOW.minusSeconds(50 * 3600L));
        List<CandleEntity> persisted = entitiesFrom(fetched);
        when(candleRepository
                .findByExchangeAndSymbolAndTimeframeAndCloseTimeLessThanEqualOrderByOpenTimeDesc(
                        any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(), persisted);
        when(marketDataProvider.getCandles(any(), any(), any(), any())).thenReturn(fetched);
        when(candleRepository.findByExchangeAndSymbolAndTimeframeAndOpenTimeBetween(
                any(), any(), any(), any(), any())).thenReturn(List.of());

        List<Candle> result = service.getConfirmedCandles(request);

        assertThat(result).hasSize(50);
        verify(candleRepository).saveAllAndFlush(any());
        verify(marketDataProvider).getCandles(any(), any(), any(), any());
    }

    @Test
    void returnsNotFoundWhenNoMarketDataExists() {
        ApiRequest request = new ApiRequest("BTC", Timeframe.ONE_HOUR, 50);
        when(candleRepository
                .findByExchangeAndSymbolAndTimeframeAndCloseTimeLessThanEqualOrderByOpenTimeDesc(
                        any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());
        when(marketDataProvider.getCandles(any(), any(), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.getConfirmedCandles(request))
                .isInstanceOf(ApiException.class)
                .extracting("status", "code")
                .containsExactly(org.springframework.http.HttpStatus.NOT_FOUND, "NO_MARKET_DATA");
    }

    @Test
    void returnsInsufficientCandlesWhenMarketDataIsPresentButIncomplete() {
        ApiRequest request = new ApiRequest("BTC", Timeframe.ONE_HOUR, 50);
        List<Candle> fetched = candles(49, NOW.minusSeconds(49 * 3600L));
        when(candleRepository
                .findByExchangeAndSymbolAndTimeframeAndCloseTimeLessThanEqualOrderByOpenTimeDesc(
                        any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(), entitiesFrom(fetched));
        when(marketDataProvider.getCandles(any(), any(), any(), any())).thenReturn(fetched);
        when(candleRepository.findByExchangeAndSymbolAndTimeframeAndOpenTimeBetween(
                any(), any(), any(), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.getConfirmedCandles(request))
                .isInstanceOf(ApiException.class)
                .extracting("status", "code")
                .containsExactly(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_CANDLES");
    }

    @Test
    void mapsProviderFailureToBadGateway() {
        ApiRequest request = new ApiRequest("BTC", Timeframe.ONE_HOUR, 50);
        when(candleRepository
                .findByExchangeAndSymbolAndTimeframeAndCloseTimeLessThanEqualOrderByOpenTimeDesc(
                        any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());
        when(marketDataProvider.getCandles(any(), any(), any(), any()))
                .thenThrow(new MarketDataException("provider unavailable"));

        assertThatThrownBy(() -> service.getConfirmedCandles(request))
                .isInstanceOf(ApiException.class)
                .extracting("status", "code")
                .containsExactly(org.springframework.http.HttpStatus.BAD_GATEWAY, "MARKET_DATA_UNAVAILABLE");
    }

    private static List<CandleEntity> entities(int count, Instant firstOpenTime) {
        return entitiesFrom(candles(count, firstOpenTime));
    }

    private static List<CandleEntity> entitiesFrom(List<Candle> candles) {
        CandleEntityMapper mapper = new CandleEntityMapper();
        return candles.stream().map(mapper::toEntity).toList();
    }

    private static List<Candle> candles(int count, Instant firstOpenTime) {
        List<Candle> candles = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Instant openTime = firstOpenTime.plusSeconds(index * 3600L);
            candles.add(new Candle(
                    "HYPERLIQUID",
                    "BTC",
                    Timeframe.ONE_HOUR,
                    openTime,
                    openTime.plusSeconds(3599),
                    BigDecimal.valueOf(100 + index),
                    BigDecimal.valueOf(101 + index),
                    BigDecimal.valueOf(99 + index),
                    BigDecimal.valueOf(100 + index),
                    BigDecimal.TEN,
                    1L
            ));
        }
        return candles;
    }
}
