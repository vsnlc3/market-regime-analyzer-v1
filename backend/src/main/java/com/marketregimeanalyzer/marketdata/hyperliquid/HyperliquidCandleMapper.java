package com.marketregimeanalyzer.marketdata.hyperliquid;

import com.marketregimeanalyzer.marketdata.MarketDataException;
import com.marketregimeanalyzer.model.Candle;
import com.marketregimeanalyzer.model.Timeframe;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class HyperliquidCandleMapper {

    public Candle toCandle(
            HyperliquidCandleResponse response,
            String requestedSymbol,
            Timeframe requestedTimeframe
    ) {
        if (response == null) {
            throw new MarketDataException("Hyperliquid returned a null candle");
        }
        if (!requestedSymbol.equals(response.symbol())) {
            throw new MarketDataException("Hyperliquid returned an unexpected symbol");
        }
        if (!requestedTimeframe.value().equals(response.interval())) {
            throw new MarketDataException("Hyperliquid returned an unexpected interval");
        }

        try {
            Instant openTime = Instant.ofEpochMilli(required(response.openTime(), "openTime"));
            Instant closeTime = Instant.ofEpochMilli(required(response.closeTime(), "closeTime"));
            if (!closeTime.isAfter(openTime)) {
                throw new MarketDataException("Candle closeTime must be after openTime");
            }

            return new Candle(
                    "HYPERLIQUID",
                    requestedSymbol,
                    requestedTimeframe,
                    openTime,
                    closeTime,
                    decimal(response.open(), "open"),
                    decimal(response.high(), "high"),
                    decimal(response.low(), "low"),
                    decimal(response.close(), "close"),
                    decimal(response.volume(), "volume"),
                    required(response.tradeCount(), "tradeCount")
            );
        } catch (NumberFormatException exception) {
            throw new MarketDataException("Hyperliquid returned a non-numeric candle value", exception);
        }
    }

    private static long required(Long value, String field) {
        if (value == null) {
            throw new MarketDataException("Hyperliquid candle is missing " + field);
        }
        return value;
    }

    private static BigDecimal decimal(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new MarketDataException("Hyperliquid candle is missing " + field);
        }
        return new BigDecimal(value);
    }
}
