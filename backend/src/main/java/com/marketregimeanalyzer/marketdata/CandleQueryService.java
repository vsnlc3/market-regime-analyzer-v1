package com.marketregimeanalyzer.marketdata;

import com.marketregimeanalyzer.api.ApiException;
import com.marketregimeanalyzer.api.ApiRequest;
import com.marketregimeanalyzer.model.Candle;
import com.marketregimeanalyzer.repository.CandleEntity;
import com.marketregimeanalyzer.repository.CandleEntityMapper;
import com.marketregimeanalyzer.repository.CandleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class CandleQueryService {

    private static final String EXCHANGE = "HYPERLIQUID";

    private final MarketDataProvider marketDataProvider;
    private final CandleRepository candleRepository;
    private final CandleEntityMapper candleEntityMapper;
    private final Clock clock;

    @Autowired
    public CandleQueryService(
            MarketDataProvider marketDataProvider,
            CandleRepository candleRepository,
            CandleEntityMapper candleEntityMapper
    ) {
        this(marketDataProvider, candleRepository, candleEntityMapper, Clock.systemUTC());
    }

    CandleQueryService(
            MarketDataProvider marketDataProvider,
            CandleRepository candleRepository,
            CandleEntityMapper candleEntityMapper,
            Clock clock
    ) {
        this.marketDataProvider = marketDataProvider;
        this.candleRepository = candleRepository;
        this.candleEntityMapper = candleEntityMapper;
        this.clock = clock;
    }

    @Transactional
    public List<Candle> getConfirmedCandles(ApiRequest request) {
        Instant analysisAsOf = clock.instant();
        List<Candle> storedCandles = loadStoredCandles(request, analysisAsOf);
        if (storedCandles.size() >= request.window()) {
            return storedCandles;
        }

        List<Candle> fetchedCandles;
        try {
            Instant endTime = analysisAsOf;
            Instant startTime = endTime.minus(
                    request.timeframe().duration().multipliedBy(request.window())
            );
            fetchedCandles = marketDataProvider.getCandles(
                    request.symbol(),
                    request.timeframe(),
                    startTime,
                    endTime
            );
        } catch (MarketDataException exception) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "MARKET_DATA_UNAVAILABLE",
                    "Market data is currently unavailable",
                    exception
            );
        }

        List<Candle> confirmedFetchedCandles = fetchedCandles == null
                ? List.of()
                : fetchedCandles.stream()
                .filter(Objects::nonNull)
                .filter(candle -> candle.closeTime() != null
                        && !candle.closeTime().isAfter(analysisAsOf))
                .sorted(Comparator.comparing(Candle::openTime))
                .toList();
        saveNewCandles(confirmedFetchedCandles);

        List<Candle> availableCandles = loadStoredCandles(request, analysisAsOf);
        if (availableCandles.isEmpty()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "NO_MARKET_DATA",
                    "No market data is available for the requested query"
            );
        }
        if (availableCandles.size() < request.window()) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INSUFFICIENT_CANDLES",
                    "Insufficient confirmed candles for the requested window"
            );
        }
        return availableCandles;
    }

    private List<Candle> loadStoredCandles(ApiRequest request, Instant analysisAsOf) {
        return candleRepository
                .findByExchangeAndSymbolAndTimeframeAndCloseTimeLessThanEqualOrderByOpenTimeDesc(
                        EXCHANGE,
                        request.symbol(),
                        request.timeframe(),
                        analysisAsOf,
                        PageRequest.of(0, request.window())
                )
                .stream()
                .map(candleEntityMapper::toModel)
                .sorted(Comparator.comparing(Candle::openTime))
                .toList();
    }

    private void saveNewCandles(List<Candle> candles) {
        if (candles.isEmpty()) {
            return;
        }

        Instant firstOpenTime = candles.getFirst().openTime();
        Instant lastOpenTime = candles.getLast().openTime();
        List<CandleEntity> existing = candleRepository
                .findByExchangeAndSymbolAndTimeframeAndOpenTimeBetween(
                        EXCHANGE,
                        candles.getFirst().symbol(),
                        candles.getFirst().timeframe(),
                        firstOpenTime,
                        lastOpenTime
                );
        Set<Instant> existingOpenTimes = new HashSet<>();
        for (CandleEntity entity : existing) {
            existingOpenTimes.add(entity.getOpenTime());
        }

        List<CandleEntity> newEntities = candles.stream()
                .filter(candle -> !existingOpenTimes.contains(candle.openTime()))
                .map(candleEntityMapper::toEntity)
                .toList();
        if (!newEntities.isEmpty()) {
            candleRepository.saveAllAndFlush(newEntities);
        }
    }
}
