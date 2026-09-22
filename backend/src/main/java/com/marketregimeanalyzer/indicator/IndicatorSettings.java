package com.marketregimeanalyzer.indicator;

import java.math.BigDecimal;

public final class IndicatorSettings {

    public static final int ADX_PERIOD = 14;
    public static final int ATR_PERIOD = 14;
    public static final int EMA_SHORT_PERIOD = 20;
    public static final int EMA_LONG_PERIOD = 50;
    public static final int BOLLINGER_PERIOD = 20;
    public static final BigDecimal BOLLINGER_STANDARD_DEVIATION_MULTIPLIER = new BigDecimal("2.0");
    public static final int MINIMUM_CANDLE_COUNT = 50;

    private IndicatorSettings() {
    }
}
