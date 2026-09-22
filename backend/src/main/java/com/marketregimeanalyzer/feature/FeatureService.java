package com.marketregimeanalyzer.feature;

import com.marketregimeanalyzer.indicator.IndicatorPoint;
import com.marketregimeanalyzer.indicator.IndicatorSeries;
import com.marketregimeanalyzer.indicator.IndicatorCalculationException;
import com.marketregimeanalyzer.indicator.IndicatorService;
import com.marketregimeanalyzer.model.Candle;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class FeatureService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

    private final IndicatorService indicatorService;

    public FeatureService(IndicatorService indicatorService) {
        this.indicatorService = indicatorService;
    }

    public FeatureSnapshot calculate(List<Candle> candles, Instant analysisAsOf) {
        validateInput(candles);
        IndicatorSeries indicatorSeries;
        try {
            indicatorSeries = indicatorService.calculateSeries(candles, analysisAsOf);
        } catch (IndicatorCalculationException exception) {
            throw new FeatureCalculationException(exception.getMessage());
        }

        int lookbackStart = candles.size() - FeatureSettings.LOOKBACK;
        List<Candle> lookbackCandles = candles.subList(lookbackStart, candles.size());
        List<IndicatorPoint> lookbackIndicators = indicatorSeries.points()
                .subList(lookbackStart, indicatorSeries.points().size());

        BigDecimal efficiencyRatio = efficiencyRatio(lookbackCandles);
        IndicatorPoint latest = lookbackIndicators.getLast();
        BigDecimal trendStrength = trendStrength(latest, efficiencyRatio);
        BigDecimal volatility = volatility(lookbackIndicators);
        BigDecimal rangeStayRatio = rangeStayRatio(lookbackCandles);
        int rangeBreakCount = rangeBreakCount(lookbackCandles);
        int reversalCount = reversalCount(lookbackCandles, lookbackIndicators);
        BigDecimal oscillation = clamp(
                reversalScore(reversalCount)
                        .multiply(FeatureSettings.OSCILLATION_REVERSAL_WEIGHT)
                        .add(rangeStayRatio.multiply(FeatureSettings.OSCILLATION_RANGE_STAY_WEIGHT)),
                ZERO,
                ONE_HUNDRED
        );
        RangeStability rangeStability = rangeStability(lookbackCandles);
        BigDecimal breakoutRisk = breakoutRisk(
                lookbackCandles,
                rangeBreakCount,
                rangeStability.rollingRanges()
        );

        return new FeatureSnapshot(
                trendStrength,
                volatility,
                efficiencyRatio,
                rangeStability.score(),
                rangeStayRatio,
                reversalCount,
                oscillation,
                rangeBreakCount,
                breakoutRisk
        );
    }

    private void validateInput(List<Candle> candles) {
        if (candles == null || candles.size() < FeatureSettings.LOOKBACK) {
            throw new FeatureCalculationException(
                    "At least " + FeatureSettings.LOOKBACK + " confirmed candles are required"
            );
        }
    }

    private BigDecimal efficiencyRatio(List<Candle> candles) {
        int latestIndex = candles.size() - 1;
        int startIndex = latestIndex - FeatureSettings.EFFICIENCY_RATIO_PERIOD;
        BigDecimal numerator = candles.get(latestIndex).close()
                .subtract(candles.get(startIndex).close())
                .abs();
        BigDecimal denominator = ZERO;
        for (int index = startIndex + 1; index <= latestIndex; index++) {
            denominator = denominator.add(
                    candles.get(index).close().subtract(candles.get(index - 1).close()).abs()
            );
        }
        if (denominator.signum() == 0) {
            return ZERO;
        }
        return clamp(numerator.divide(denominator, MATH_CONTEXT), ZERO, BigDecimal.ONE);
    }

    private BigDecimal trendStrength(IndicatorPoint latest, BigDecimal efficiencyRatio) {
        BigDecimal adxScore = linearScore(
                latest.adx(),
                FeatureSettings.ADX_NORMALIZATION_LOWER,
                FeatureSettings.ADX_NORMALIZATION_UPPER
        );
        BigDecimal efficiencyRatioScore = efficiencyRatio.multiply(ONE_HUNDRED);
        BigDecimal emaSeparationScore = ZERO;
        if (latest.atr().signum() != 0) {
            BigDecimal emaSeparationAtr = latest.ema20()
                    .subtract(latest.ema50())
                    .abs()
                    .divide(latest.atr(), MATH_CONTEXT);
            emaSeparationScore = clamp(
                    emaSeparationAtr
                            .divide(FeatureSettings.EMA_SEPARATION_NORMALIZATION, MATH_CONTEXT)
                            .multiply(ONE_HUNDRED),
                    ZERO,
                    ONE_HUNDRED
            );
        }
        return clamp(
                adxScore.multiply(FeatureSettings.TREND_STRENGTH_ADX_WEIGHT)
                        .add(efficiencyRatioScore.multiply(FeatureSettings.TREND_STRENGTH_EFFICIENCY_WEIGHT))
                        .add(emaSeparationScore.multiply(FeatureSettings.TREND_STRENGTH_EMA_WEIGHT)),
                ZERO,
                ONE_HUNDRED
        );
    }

    private BigDecimal volatility(List<IndicatorPoint> indicators) {
        List<BigDecimal> atrPercents = indicators.stream()
                .map(IndicatorPoint::atrPercent)
                .filter(value -> value != null)
                .toList();
        if (atrPercents.isEmpty()) {
            throw new FeatureCalculationException("Volatility cannot be calculated for the supplied candles");
        }
        BigDecimal current = atrPercents.getLast();
        int lessCount = 0;
        int equalCount = 0;
        for (BigDecimal value : atrPercents) {
            int comparison = value.compareTo(current);
            if (comparison < 0) {
                lessCount++;
            } else if (comparison == 0) {
                equalCount++;
            }
        }
        if (atrPercents.size() == 1 || lessCount + equalCount == atrPercents.size()) {
            return ZERO;
        }
        BigDecimal midRank = BigDecimal.valueOf(lessCount)
                .add(BigDecimal.valueOf(equalCount - 1).divide(BigDecimal.valueOf(2), MATH_CONTEXT));
        return clamp(
                midRank.divide(BigDecimal.valueOf(atrPercents.size() - 1), MATH_CONTEXT)
                        .multiply(ONE_HUNDRED),
                ZERO,
                ONE_HUNDRED
        );
    }

    private BigDecimal rangeStayRatio(List<Candle> candles) {
        int startIndex = candles.size() - FeatureSettings.EVALUATION_PERIOD;
        int stayedCount = 0;
        for (int index = startIndex; index < candles.size(); index++) {
            RangeBounds bounds = previousRange(candles, index);
            BigDecimal close = candles.get(index).close();
            if (close.compareTo(bounds.lower()) >= 0 && close.compareTo(bounds.upper()) <= 0) {
                stayedCount++;
            }
        }
        return BigDecimal.valueOf(stayedCount)
                .divide(BigDecimal.valueOf(FeatureSettings.EVALUATION_PERIOD), MATH_CONTEXT)
                .multiply(ONE_HUNDRED);
    }

    private int rangeBreakCount(List<Candle> candles) {
        int startIndex = candles.size() - FeatureSettings.EVALUATION_PERIOD;
        int breakCount = 0;
        for (int index = startIndex; index < candles.size(); index++) {
            RangeBounds bounds = previousRange(candles, index);
            Candle candle = candles.get(index);
            if (candle.high().compareTo(bounds.upper()) > 0
                    || candle.low().compareTo(bounds.lower()) < 0) {
                breakCount++;
            }
        }
        return breakCount;
    }

    private int reversalCount(List<Candle> candles, List<IndicatorPoint> indicators) {
        int startIndex = candles.size() - FeatureSettings.EVALUATION_PERIOD;
        int previousDirection = 0;
        int reversalCount = 0;
        for (int index = startIndex; index < candles.size(); index++) {
            BigDecimal delta = candles.get(index).close().subtract(candles.get(index - 1).close());
            BigDecimal atr = required(indicators.get(index).atr(), "ATR");
            int direction = direction(delta, atr);
            if (direction != 0) {
                if (previousDirection != 0 && direction != previousDirection) {
                    reversalCount++;
                }
                previousDirection = direction;
            }
        }
        return reversalCount;
    }

    private int direction(BigDecimal delta, BigDecimal atr) {
        if (atr.signum() == 0 || delta.signum() == 0) {
            return 0;
        }
        BigDecimal threshold = atr.multiply(FeatureSettings.REVERSAL_ATR_MULTIPLIER);
        if (delta.compareTo(threshold) >= 0) {
            return 1;
        }
        if (delta.compareTo(threshold.negate()) <= 0) {
            return -1;
        }
        return 0;
    }

    private BigDecimal reversalScore(int reversalCount) {
        return clamp(
                BigDecimal.valueOf(reversalCount)
                        .divide(FeatureSettings.REVERSAL_TARGET_COUNT, MATH_CONTEXT)
                        .multiply(ONE_HUNDRED),
                ZERO,
                ONE_HUNDRED
        );
    }

    private RangeStability rangeStability(List<Candle> candles) {
        List<RollingRange> rollingRanges = rollingRanges(candles);
        List<BigDecimal> normalizedWidths = rollingRanges.stream()
                .map(RollingRange::normalizedWidth)
                .toList();
        BigDecimal mean = average(normalizedWidths);
        BigDecimal widthStabilityScore = mean.signum() == 0
                ? ONE_HUNDRED
                : clamp(
                ONE_HUNDRED.multiply(
                        BigDecimal.ONE.subtract(
                                clamp(standardDeviation(normalizedWidths).divide(mean, MATH_CONTEXT)
                                        .divide(FeatureSettings.RANGE_WIDTH_CV_LIMIT, MATH_CONTEXT), ZERO, BigDecimal.ONE)
                        )
                ),
                ZERO,
                ONE_HUNDRED
        );

        BigDecimal averageWidth = average(rollingRanges.stream().map(RollingRange::width).toList());
        BigDecimal centerStabilityScore = averageWidth.signum() == 0
                ? ONE_HUNDRED
                : clamp(
                ONE_HUNDRED.multiply(
                        BigDecimal.ONE.subtract(
                                clamp(
                                        rollingRanges.getLast().center()
                                                .subtract(rollingRanges.getFirst().center()).abs()
                                                .divide(averageWidth, MATH_CONTEXT)
                                                .divide(FeatureSettings.RANGE_CENTER_DRIFT_LIMIT, MATH_CONTEXT),
                                        ZERO,
                                        BigDecimal.ONE
                                )
                        )
                ),
                ZERO,
                ONE_HUNDRED
        );
        BigDecimal score = clamp(
                widthStabilityScore.multiply(FeatureSettings.RANGE_STABILITY_WIDTH_WEIGHT)
                        .add(centerStabilityScore.multiply(FeatureSettings.RANGE_STABILITY_CENTER_WEIGHT)),
                ZERO,
                ONE_HUNDRED
        );
        return new RangeStability(score, rollingRanges);
    }

    private BigDecimal breakoutRisk(
            List<Candle> candles,
            int rangeBreakCount,
            List<RollingRange> rollingRanges
    ) {
        BigDecimal rangeBreakScore = clamp(
                BigDecimal.valueOf(rangeBreakCount)
                        .divide(FeatureSettings.RANGE_BREAK_TARGET_COUNT, MATH_CONTEXT)
                        .multiply(ONE_HUNDRED),
                ZERO,
                ONE_HUNDRED
        );
        RangeBounds currentReference = previousRange(candles, candles.size() - 1);
        BigDecimal currentClose = candles.getLast().close();
        BigDecimal rangeWidth = currentReference.upper().subtract(currentReference.lower());
        BigDecimal edgeScore;
        if (currentClose.compareTo(currentReference.lower()) < 0
                || currentClose.compareTo(currentReference.upper()) > 0
                || rangeWidth.signum() == 0) {
            edgeScore = rangeWidth.signum() == 0 ? ZERO : ONE_HUNDRED;
        } else {
            BigDecimal distanceToNearestEdge = currentClose.subtract(currentReference.lower())
                    .min(currentReference.upper().subtract(currentClose));
            edgeScore = clamp(
                    ONE_HUNDRED.multiply(
                            BigDecimal.ONE.subtract(
                                    distanceToNearestEdge.multiply(BigDecimal.valueOf(2))
                                            .divide(rangeWidth, MATH_CONTEXT)
                            )
                    ),
                    ZERO,
                    ONE_HUNDRED
            );
        }

        BigDecimal currentWidth = rollingRanges.getLast().width();
        BigDecimal previousAverageWidth = average(
                rollingRanges.subList(0, rollingRanges.size() - 1).stream()
                        .map(RollingRange::width)
                        .toList()
        );
        BigDecimal expansionScore;
        if (previousAverageWidth.signum() == 0) {
            expansionScore = currentWidth.signum() == 0 ? ZERO : ONE_HUNDRED;
        } else {
            BigDecimal expansionRatio = currentWidth.divide(previousAverageWidth, MATH_CONTEXT);
            expansionScore = clamp(
                    expansionRatio.subtract(BigDecimal.ONE)
                            .divide(FeatureSettings.RANGE_EXPANSION_LIMIT, MATH_CONTEXT)
                            .multiply(ONE_HUNDRED),
                    ZERO,
                    ONE_HUNDRED
            );
        }
        return clamp(
                rangeBreakScore.multiply(FeatureSettings.BREAKOUT_RISK_BREAK_WEIGHT)
                        .add(edgeScore.multiply(FeatureSettings.BREAKOUT_RISK_EDGE_WEIGHT))
                        .add(expansionScore.multiply(FeatureSettings.BREAKOUT_RISK_EXPANSION_WEIGHT)),
                ZERO,
                ONE_HUNDRED
        );
    }

    private List<RollingRange> rollingRanges(List<Candle> candles) {
        List<RollingRange> ranges = new ArrayList<>();
        for (int index = FeatureSettings.REFERENCE_RANGE_PERIOD - 1; index < candles.size(); index++) {
            BigDecimal upper = candles.get(index - FeatureSettings.REFERENCE_RANGE_PERIOD + 1).high();
            BigDecimal lower = candles.get(index - FeatureSettings.REFERENCE_RANGE_PERIOD + 1).low();
            for (int rangeIndex = index - FeatureSettings.REFERENCE_RANGE_PERIOD + 1; rangeIndex <= index; rangeIndex++) {
                upper = upper.max(candles.get(rangeIndex).high());
                lower = lower.min(candles.get(rangeIndex).low());
            }
            BigDecimal width = upper.subtract(lower);
            BigDecimal close = candles.get(index).close();
            ranges.add(new RollingRange(
                    upper,
                    lower,
                    width,
                    width.divide(close, MATH_CONTEXT),
                    upper.add(lower).divide(BigDecimal.valueOf(2), MATH_CONTEXT)
            ));
        }
        return ranges;
    }

    private RangeBounds previousRange(List<Candle> candles, int index) {
        int startIndex = index - FeatureSettings.REFERENCE_RANGE_PERIOD;
        int endIndex = index - 1;
        BigDecimal upper = candles.get(startIndex).high();
        BigDecimal lower = candles.get(startIndex).low();
        for (int rangeIndex = startIndex + 1; rangeIndex <= endIndex; rangeIndex++) {
            upper = upper.max(candles.get(rangeIndex).high());
            lower = lower.min(candles.get(rangeIndex).low());
        }
        return new RangeBounds(upper, lower);
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return ZERO;
        }
        BigDecimal sum = values.stream().reduce(ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), MATH_CONTEXT);
    }

    private BigDecimal standardDeviation(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return ZERO;
        }
        BigDecimal mean = average(values);
        BigDecimal squaredDifferenceSum = values.stream()
                .map(value -> value.subtract(mean).pow(2))
                .reduce(ZERO, BigDecimal::add);
        return squaredDifferenceSum
                .divide(BigDecimal.valueOf(values.size()), MATH_CONTEXT)
                .sqrt(MATH_CONTEXT);
    }

    private BigDecimal linearScore(BigDecimal value, BigDecimal lower, BigDecimal upper) {
        if (value.compareTo(lower) <= 0) {
            return ZERO;
        }
        if (value.compareTo(upper) >= 0) {
            return ONE_HUNDRED;
        }
        return clamp(
                value.subtract(lower)
                        .divide(upper.subtract(lower), MATH_CONTEXT)
                        .multiply(ONE_HUNDRED),
                ZERO,
                ONE_HUNDRED
        );
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal minimum, BigDecimal maximum) {
        return value.max(minimum).min(maximum);
    }

    private BigDecimal required(BigDecimal value, String name) {
        if (value == null) {
            throw new FeatureCalculationException(name + " cannot be calculated for the supplied candles");
        }
        return value;
    }

    private record RangeBounds(BigDecimal upper, BigDecimal lower) {
    }

    private record RollingRange(
            BigDecimal upper,
            BigDecimal lower,
            BigDecimal width,
            BigDecimal normalizedWidth,
            BigDecimal center
    ) {
    }

    private record RangeStability(BigDecimal score, List<RollingRange> rollingRanges) {
    }
}
