package com.marketregimeanalyzer.indicator;

import java.math.BigDecimal;

public record IndicatorPoint(
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
