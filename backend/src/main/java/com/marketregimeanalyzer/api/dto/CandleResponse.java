package com.marketregimeanalyzer.api.dto;

import com.marketregimeanalyzer.model.Candle;

import java.time.Instant;

public record CandleResponse(
        Instant openTime,
        Instant closeTime,
        String open,
        String high,
        String low,
        String close,
        String volume
) {

    public static CandleResponse from(Candle candle) {
        return new CandleResponse(
                candle.openTime(),
                candle.closeTime(),
                candle.open().toPlainString(),
                candle.high().toPlainString(),
                candle.low().toPlainString(),
                candle.close().toPlainString(),
                candle.volume().toPlainString()
        );
    }
}
