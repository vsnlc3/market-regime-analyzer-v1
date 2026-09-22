package com.marketregimeanalyzer.grid;

import java.math.BigDecimal;

public final class GridSuitabilitySettings {

    public static final BigDecimal RANGE_STABILITY_WEIGHT = new BigDecimal("0.25");
    public static final BigDecimal RANGE_STAY_WEIGHT = new BigDecimal("0.20");
    public static final BigDecimal OSCILLATION_WEIGHT = new BigDecimal("0.20");
    public static final BigDecimal TREND_SAFETY_WEIGHT = new BigDecimal("0.15");
    public static final BigDecimal BREAKOUT_SAFETY_WEIGHT = new BigDecimal("0.15");
    public static final BigDecimal VOLATILITY_FITNESS_WEIGHT = new BigDecimal("0.05");

    public static final BigDecimal VOLATILITY_LOWER_ZERO = new BigDecimal("20");
    public static final BigDecimal VOLATILITY_OPTIMAL_LOWER = new BigDecimal("40");
    public static final BigDecimal VOLATILITY_OPTIMAL_UPPER = new BigDecimal("70");
    public static final BigDecimal VOLATILITY_UPPER_ZERO = new BigDecimal("90");

    public static final int RANGE_SCORE_CAP = 100;
    public static final int UNSTABLE_SCORE_CAP = 59;
    public static final int TREND_SCORE_CAP = 39;

    public static final BigDecimal RANGE_STABILITY_REASON_POSITIVE = new BigDecimal("70");
    public static final BigDecimal RANGE_STABILITY_REASON_NEGATIVE = new BigDecimal("50");
    public static final BigDecimal RANGE_STAY_REASON_POSITIVE = new BigDecimal("80");
    public static final BigDecimal RANGE_STAY_REASON_NEGATIVE = new BigDecimal("60");
    public static final BigDecimal OSCILLATION_REASON_POSITIVE = new BigDecimal("60");
    public static final BigDecimal OSCILLATION_REASON_NEGATIVE = new BigDecimal("40");
    public static final BigDecimal TREND_REASON_NEGATIVE = new BigDecimal("65");
    public static final BigDecimal TREND_REASON_POSITIVE = new BigDecimal("40");
    public static final BigDecimal BREAKOUT_REASON_NEGATIVE = new BigDecimal("60");
    public static final BigDecimal BREAKOUT_REASON_POSITIVE = new BigDecimal("40");

    private GridSuitabilitySettings() {
    }
}
