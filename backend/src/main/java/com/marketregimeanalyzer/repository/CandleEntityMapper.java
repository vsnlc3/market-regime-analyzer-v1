package com.marketregimeanalyzer.repository;

import com.marketregimeanalyzer.model.Candle;
import org.springframework.stereotype.Component;

@Component
public class CandleEntityMapper {

    public CandleEntity toEntity(Candle candle) {
        return new CandleEntity(
                candle.exchange(),
                candle.symbol(),
                candle.timeframe(),
                candle.openTime(),
                candle.closeTime(),
                candle.open(),
                candle.high(),
                candle.low(),
                candle.close(),
                candle.volume(),
                candle.tradeCount()
        );
    }

    public Candle toModel(CandleEntity entity) {
        return new Candle(
                entity.getExchange(),
                entity.getSymbol(),
                entity.getTimeframe(),
                entity.getOpenTime(),
                entity.getCloseTime(),
                entity.getOpen(),
                entity.getHigh(),
                entity.getLow(),
                entity.getClose(),
                entity.getVolume(),
                entity.getTradeCount()
        );
    }
}
