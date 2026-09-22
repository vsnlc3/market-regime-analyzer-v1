package com.marketregimeanalyzer.indicator;

import java.math.BigDecimal;

public record IndicatorSnapshot(
        BigDecimal adx,
        BigDecimal atr,
        BigDecimal atrPercent,
        BigDecimal ema20,
        BigDecimal ema50,
        BigDecimal bollingerMiddle,
        BigDecimal bollingerUpper,
        BigDecimal bollingerLower
) {
}
