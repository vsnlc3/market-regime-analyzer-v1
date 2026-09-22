package com.marketregimeanalyzer.marketdata;

import com.marketregimeanalyzer.model.Candle;
import com.marketregimeanalyzer.model.Timeframe;

import java.time.Instant;
import java.util.List;

public interface MarketDataProvider {

    List<Candle> getCandles(
            String symbol,
            Timeframe timeframe,
            Instant startTime,
            Instant endTime
    );
}
