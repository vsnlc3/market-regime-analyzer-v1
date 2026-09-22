package com.marketregimeanalyzer.marketdata.hyperliquid;

import com.marketregimeanalyzer.marketdata.MarketDataException;
import com.marketregimeanalyzer.marketdata.MarketDataProvider;
import com.marketregimeanalyzer.model.Candle;
import com.marketregimeanalyzer.model.Timeframe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Component
public class HyperliquidMarketDataProvider implements MarketDataProvider {

    private final RestClient restClient;
    private final HyperliquidCandleMapper candleMapper;

    public HyperliquidMarketDataProvider(
            RestClient.Builder restClientBuilder,
            @Value("${hyperliquid.base-url}") String baseUrl,
            HyperliquidCandleMapper candleMapper
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.candleMapper = candleMapper;
    }

    @Override
    public List<Candle> getCandles(
            String symbol,
            Timeframe timeframe,
            Instant startTime,
            Instant endTime
    ) {
        String normalizedSymbol = normalizeSymbol(symbol);
        validateRange(startTime, endTime);

        HyperliquidCandleSnapshotRequest request = HyperliquidCandleSnapshotRequest.of(
                normalizedSymbol,
                timeframe.value(),
                startTime.toEpochMilli(),
                endTime.toEpochMilli()
        );

        try {
            List<HyperliquidCandleResponse> response = restClient.post()
                    .uri("/info")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null) {
                throw new MarketDataException("Hyperliquid returned an empty response body");
            }

            return response.stream()
                    .map(candle -> candleMapper.toCandle(candle, normalizedSymbol, timeframe))
                    .sorted(Comparator.comparing(Candle::openTime))
                    .toList();
        } catch (MarketDataException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new MarketDataException("Failed to retrieve candles from Hyperliquid", exception);
        }
    }

    private static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        return symbol.trim().toUpperCase();
    }

    private static void validateRange(Instant startTime, Instant endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
    }
}
