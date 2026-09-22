package com.marketregimeanalyzer.feature;

import com.marketregimeanalyzer.indicator.IndicatorSettings;

import java.math.BigDecimal;

public final class FeatureSettings {

    public static final int LOOKBACK = IndicatorSettings.MINIMUM_CANDLE_COUNT;
    public static final int EFFICIENCY_RATIO_PERIOD = 20;
    public static final int REFERENCE_RANGE_PERIOD = 20;
    public static final int EVALUATION_PERIOD = 20;
    public static final BigDecimal REVERSAL_ATR_MULTIPLIER = new BigDecimal("0.25");
    public static final BigDecimal REVERSAL_TARGET_COUNT = new BigDecimal("6");
    public static final BigDecimal RANGE_BREAK_TARGET_COUNT = new BigDecimal("4");
    public static final BigDecimal ADX_NORMALIZATION_LOWER = new BigDecimal("15");
    public static final BigDecimal ADX_NORMALIZATION_UPPER = new BigDecimal("40");
    public static final BigDecimal EMA_SEPARATION_NORMALIZATION = new BigDecimal("2.0");
    public static final BigDecimal RANGE_WIDTH_CV_LIMIT = new BigDecimal("0.5");
    public static final BigDecimal RANGE_CENTER_DRIFT_LIMIT = new BigDecimal("0.5");
    public static final BigDecimal RANGE_EXPANSION_LIMIT = new BigDecimal("0.5");

    public static final BigDecimal TREND_STRENGTH_ADX_WEIGHT = new BigDecimal("0.50");
    public static final BigDecimal TREND_STRENGTH_EFFICIENCY_WEIGHT = new BigDecimal("0.30");
    public static final BigDecimal TREND_STRENGTH_EMA_WEIGHT = new BigDecimal("0.20");
    public static final BigDecimal OSCILLATION_REVERSAL_WEIGHT = new BigDecimal("0.60");
    public static final BigDecimal OSCILLATION_RANGE_STAY_WEIGHT = new BigDecimal("0.40");
    public static final BigDecimal RANGE_STABILITY_WIDTH_WEIGHT = new BigDecimal("0.60");
    public static final BigDecimal RANGE_STABILITY_CENTER_WEIGHT = new BigDecimal("0.40");
    public static final BigDecimal BREAKOUT_RISK_BREAK_WEIGHT = new BigDecimal("0.50");
    public static final BigDecimal BREAKOUT_RISK_EDGE_WEIGHT = new BigDecimal("0.30");
    public static final BigDecimal BREAKOUT_RISK_EXPANSION_WEIGHT = new BigDecimal("0.20");

    private FeatureSettings() {
    }
}
