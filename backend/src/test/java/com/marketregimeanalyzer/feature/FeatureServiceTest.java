package com.marketregimeanalyzer.feature;

import com.marketregimeanalyzer.indicator.IndicatorService;
import com.marketregimeanalyzer.model.Candle;
import com.marketregimeanalyzer.model.Timeframe;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeatureServiceTest {

    private final FeatureService featureService = new FeatureService(new IndicatorService());

    @Test
    void constantPriceProducesStableNonDummyFeatureValues() {
        List<Candle> candles = candles(50, index -> BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.ZERO);

        FeatureSnapshot result = featureService.calculate(candles, analysisAsOf(candles));

        assertThat(result.efficiencyRatio()).isEqualByComparingTo("0");
        assertThat(result.trendStrength()).isEqualByComparingTo("0");
        assertThat(result.volatility()).isEqualByComparingTo("0");
        assertThat(result.rangeStability()).isEqualByComparingTo("100");
        assertThat(result.rangeStayRatio()).isEqualByComparingTo("100");
        assertThat(result.reversalCount()).isZero();
        assertThat(result.oscillation()).isEqualByComparingTo("40.0");
        assertThat(result.rangeBreakCount()).isZero();
        assertThat(result.breakoutRisk()).isEqualByComparingTo("0");
    }

    @Test
    void oneWayTrendProducesHighEfficiencyAndTrendStrength() {
        List<Candle> candles = candles(50, index -> BigDecimal.valueOf(100 + index), BigDecimal.ONE, BigDecimal.ONE);

        FeatureSnapshot result = featureService.calculate(candles, analysisAsOf(candles));

        assertThat(result.efficiencyRatio()).isEqualByComparingTo("1");
        assertThat(result.trendStrength()).isGreaterThan(new BigDecimal("50"));
        assertThat(result.rangeBreakCount()).isPositive();
        assertThat(result.breakoutRisk()).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
    }

    @Test
    void oscillatingRangeProducesReversalsAndOscillationScore() {
        List<Candle> candles = candles(
                50,
                index -> index < 30 ? BigDecimal.valueOf(100) : BigDecimal.valueOf(index % 2 == 0 ? 100 : 110),
                BigDecimal.ONE,
                BigDecimal.ONE
        );

        FeatureSnapshot result = featureService.calculate(candles, analysisAsOf(candles));

        assertThat(result.efficiencyRatio()).isBetween(BigDecimal.ZERO, BigDecimal.ONE);
        assertThat(result.reversalCount()).isGreaterThan(0);
        assertThat(result.rangeStayRatio()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.oscillation()).isGreaterThan(new BigDecimal("40"));
    }

    @Test
    void breakoutDataProducesRangeBreaksAndBreakoutRisk() {
        List<Candle> candles = candles(
                50,
                index -> index < 30 ? BigDecimal.valueOf(100) : BigDecimal.valueOf(100 + (index - 29) * 10L),
                BigDecimal.ONE,
                BigDecimal.ONE
        );

        FeatureSnapshot result = featureService.calculate(candles, analysisAsOf(candles));

        assertThat(result.rangeBreakCount()).isGreaterThan(0);
        assertThat(result.breakoutRisk()).isGreaterThan(new BigDecimal("50"));
        assertThat(result.breakoutRisk()).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
    }

    @Test
    void allScoreFeaturesStayWithinTheirDefinedBounds() {
        List<Candle> candles = candles(50, index -> BigDecimal.valueOf(100 + (index % 7)), BigDecimal.ONE, BigDecimal.ONE);

        FeatureSnapshot result = featureService.calculate(candles, analysisAsOf(candles));

        assertThat(result.trendStrength()).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
        assertThat(result.volatility()).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
        assertThat(result.efficiencyRatio()).isBetween(BigDecimal.ZERO, BigDecimal.ONE);
        assertThat(result.rangeStability()).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
        assertThat(result.rangeStayRatio()).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
        assertThat(result.oscillation()).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
        assertThat(result.breakoutRisk()).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
    }

    @Test
    void rejectsInsufficientCandles() {
        List<Candle> candles = candles(49, index -> BigDecimal.valueOf(100 + index), BigDecimal.ONE, BigDecimal.ONE);

        assertThatThrownBy(() -> featureService.calculate(candles, analysisAsOf(candles)))
                .isInstanceOf(FeatureCalculationException.class)
                .hasMessageContaining("50");
    }

    @Test
    void rejectsFutureOrUnconfirmedCandles() {
        List<Candle> candles = candles(50, index -> BigDecimal.valueOf(100 + index), BigDecimal.ONE, BigDecimal.ONE);

        assertThatThrownBy(() -> featureService.calculate(candles, analysisAsOf(candles).minusMillis(1)))
                .isInstanceOf(FeatureCalculationException.class)
                .hasMessageContaining("Future or unconfirmed");
    }

    private static List<Candle> candles(
            int count,
            IntFunction<BigDecimal> closeValue,
            BigDecimal highOffset,
            BigDecimal lowOffset
    ) {
        Instant firstOpenTime = Instant.parse("2020-01-01T00:00:00Z");
        List<Candle> candles = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Instant openTime = firstOpenTime.plus(index, ChronoUnit.HOURS);
            BigDecimal close = closeValue.apply(index);
            candles.add(new Candle(
                    "TEST",
                    "BTC",
                    Timeframe.ONE_HOUR,
                    openTime,
                    openTime.plus(59, ChronoUnit.MINUTES),
                    close,
                    close.add(highOffset),
                    close.subtract(lowOffset),
                    close,
                    BigDecimal.TEN,
                    1L
            ));
        }
        return candles;
    }

    private static Instant analysisAsOf(List<Candle> candles) {
        return candles.getLast().closeTime();
    }
}
