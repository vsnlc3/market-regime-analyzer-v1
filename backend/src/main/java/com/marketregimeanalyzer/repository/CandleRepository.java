package com.marketregimeanalyzer.repository;

import com.marketregimeanalyzer.model.Timeframe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CandleRepository extends JpaRepository<CandleEntity, Long> {

    Optional<CandleEntity> findByExchangeAndSymbolAndTimeframeAndOpenTime(
            String exchange,
            String symbol,
            Timeframe timeframe,
            Instant openTime
    );

    List<CandleEntity> findByExchangeAndSymbolAndTimeframeAndCloseTimeLessThanEqualOrderByOpenTimeDesc(
            String exchange,
            String symbol,
            Timeframe timeframe,
            Instant closeTime,
            Pageable pageable
    );

    List<CandleEntity> findByExchangeAndSymbolAndTimeframeAndOpenTimeBetween(
            String exchange,
            String symbol,
            Timeframe timeframe,
            Instant startTime,
            Instant endTime
    );
}
