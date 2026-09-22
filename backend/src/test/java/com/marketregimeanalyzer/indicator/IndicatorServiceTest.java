package com.marketregimeanalyzer.indicator;

import com.marketregimeanalyzer.model.Candle;
import com.marketregimeanalyzer.model.Timeframe;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class IndicatorServiceTest {

    private final IndicatorService indicatorService = new IndicatorService();

    @Test
    void calculatesConfiguredIndicatorsForExactlyFiftyCandles() {
        List<Candle> candles = candles(50, index -> BigDecimal.valueOf(100 + index));

        IndicatorSnapshot result = indicatorService.calculate(candles, analysisAsOf(candles));

        assertThat(result.adx()).isNotNull().isPositive();
        assertThat(result.atr()).isNotNull().isPositive();
        assertThat(result.ema20()).isNotNull().isPositive();
        assertThat(result.ema50()).isNotNull().isPositive();
        assertThat(result.ema20()).isGreaterThan(result.ema50());

        BigDecimal expectedAtrPercent = result.atr()
                .divide(candles.getLast().close(), MathContext.DECIMAL128)
                .multiply(BigDecimal.valueOf(100));
        assertThat(result.atrPercent()).isCloseTo(expectedAtrPercent, within(new BigDecimal("0.000000000000000001")));
        assertThat(result.atrPercent().setScale(2, RoundingMode.HALF_UP))
                .isNotEqualByComparingTo(result.atrPercent());

        assertThat(result.bollingerMiddle()).isEqualByComparingTo("139.5");
        assertThat(result.bollingerUpper()).isGreaterThan(result.bollingerMiddle());
        assertThat(result.bollingerMiddle()).isGreaterThan(result.bollingerLower());
    }

    @Test
    void calculatesConstantPriceIndicatorsWithoutDummyValues() {
        List<Candle> candles = candles(50, index -> BigDecimal.valueOf(100));

        IndicatorSnapshot result = indicatorService.calculate(candles, analysisAsOf(candles));

        assertThat(result.ema20()).isEqualByComparingTo("100");
        assertThat(result.ema50()).isEqualByComparingTo("100");
        assertThat(result.bollingerMiddle()).isEqualByComparingTo("100");
        assertThat(result.bollingerUpper()).isEqualByComparingTo("100");
        assertThat(result.bollingerLower()).isEqualByComparingTo("100");
        assertThat(result.atrPercent()).isPositive();
    }

    @Test
    void rejectsFewerThanMinimumCandles() {
        List<Candle> candles = candles(49, index -> BigDecimal.valueOf(100 + index));

        assertThatThrownBy(() -> indicatorService.calculate(candles, analysisAsOf(candles)))
                .isInstanceOf(IndicatorCalculationException.class)
                .hasMessageContaining("50");
    }

    @Test
    void rejectsZeroLatestCloseInsteadOfReturningZeroAtrPercent() {
        List<Candle> candles = candles(50, index -> index == 49 ? BigDecimal.ZERO : BigDecimal.valueOf(100 + index));

        assertThatThrownBy(() -> indicatorService.calculate(candles, analysisAsOf(candles)))
                .isInstanceOf(IndicatorCalculationException.class)
                .hasMessageContaining("close value is zero");
    }

    @Test
    void rejectsCandlesThatAreNotConfirmedAtAnalysisTime() {
        List<Candle> candles = candles(50, index -> BigDecimal.valueOf(100 + index));
        Instant analysisAsOf = candles.getLast().closeTime().minusMillis(1);

        assertThatThrownBy(() -> indicatorService.calculate(candles, analysisAsOf))
                .isInstanceOf(IndicatorCalculationException.class)
                .hasMessageContaining("Future or unconfirmed");
    }

    private static List<Candle> candles(int count, IntFunction<BigDecimal> closeValue) {
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
                    close.add(BigDecimal.ONE),
                    close.subtract(BigDecimal.ONE),
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
