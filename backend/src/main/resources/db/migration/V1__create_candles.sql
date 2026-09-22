CREATE TABLE candles (
    id BIGSERIAL PRIMARY KEY,
    exchange VARCHAR(32) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    timeframe VARCHAR(8) NOT NULL,
    open_time TIMESTAMPTZ NOT NULL,
    close_time TIMESTAMPTZ NOT NULL,
    open NUMERIC NOT NULL,
    high NUMERIC NOT NULL,
    low NUMERIC NOT NULL,
    close NUMERIC NOT NULL,
    volume NUMERIC NOT NULL,
    trade_count BIGINT NOT NULL,
    CONSTRAINT uk_candles_identity UNIQUE (exchange, symbol, timeframe, open_time)
);

CREATE INDEX idx_candles_symbol_timeframe_open_time
    ON candles (symbol, timeframe, open_time);
