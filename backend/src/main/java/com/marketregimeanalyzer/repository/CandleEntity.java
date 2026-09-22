package com.marketregimeanalyzer.repository;

import com.marketregimeanalyzer.model.Timeframe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "candles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_candles_identity",
                columnNames = {"exchange", "symbol", "timeframe", "open_time"}
        ),
        indexes = @Index(
                name = "idx_candles_symbol_timeframe_open_time",
                columnList = "symbol, timeframe, open_time"
        )
)
public class CandleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String exchange;

    @Column(nullable = false, length = 32)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Timeframe timeframe;

    @Column(name = "open_time", nullable = false)
    private Instant openTime;

    @Column(name = "close_time", nullable = false)
    private Instant closeTime;

    @Column(nullable = false, columnDefinition = "NUMERIC")
    private BigDecimal open;

    @Column(nullable = false, columnDefinition = "NUMERIC")
    private BigDecimal high;

    @Column(nullable = false, columnDefinition = "NUMERIC")
    private BigDecimal low;

    @Column(nullable = false, columnDefinition = "NUMERIC")
    private BigDecimal close;

    @Column(nullable = false, columnDefinition = "NUMERIC")
    private BigDecimal volume;

    @Column(name = "trade_count", nullable = false)
    private long tradeCount;

    protected CandleEntity() {
    }

    CandleEntity(
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
        this.exchange = exchange;
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.tradeCount = tradeCount;
    }

    public Long getId() {
        return id;
    }

    public String getExchange() {
        return exchange;
    }

    public String getSymbol() {
        return symbol;
    }

    public Timeframe getTimeframe() {
        return timeframe;
    }

    public Instant getOpenTime() {
        return openTime;
    }

    public Instant getCloseTime() {
        return closeTime;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public BigDecimal getClose() {
        return close;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public long getTradeCount() {
        return tradeCount;
    }
}
