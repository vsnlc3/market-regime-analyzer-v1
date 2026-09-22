package com.marketregimeanalyzer.regime;

import java.math.BigDecimal;

public final class RegimeSettings {

    public static final BigDecimal EXTREME_UNSTABLE_VOLATILITY_THRESHOLD = new BigDecimal("80");
    public static final BigDecimal EXTREME_UNSTABLE_BREAKOUT_RISK_THRESHOLD = new BigDecimal("85");
    public static final BigDecimal TREND_STRENGTH_THRESHOLD = new BigDecimal("65");
    public static final BigDecimal TREND_EFFICIENCY_RATIO_THRESHOLD = new BigDecimal("0.45");
    public static final BigDecimal RANGE_STABILITY_THRESHOLD = new BigDecimal("60");
    public static final BigDecimal RANGE_STAY_RATIO_THRESHOLD = new BigDecimal("70");
    public static final BigDecimal RANGE_OSCILLATION_THRESHOLD = new BigDecimal("50");
    public static final BigDecimal RANGE_MAX_TREND_STRENGTH = new BigDecimal("60");
    public static final BigDecimal RANGE_MAX_BREAKOUT_RISK = new BigDecimal("60");

    private RegimeSettings() {
    }
}
