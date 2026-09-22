package com.marketregimeanalyzer.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Candle(
        String exchange,
        String symbol,
        Timeframe timeframe,
        Instant openTime,
        Instant closeTime,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume,
        long tradeCount
) {
}
