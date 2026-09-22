package com.marketregimeanalyzer.indicator;

import com.marketregimeanalyzer.model.Candle;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.averages.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.num.NumFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class IndicatorService {

    public IndicatorSnapshot calculate(List<Candle> candles, Instant analysisAsOf) {
        IndicatorPoint latest = calculateSeries(candles, analysisAsOf).latest();
        return new IndicatorSnapshot(
                latest.adx(),
                latest.atr(),
                latest.atrPercent(),
                latest.ema20(),
                latest.ema50(),
                latest.bollingerMiddle(),
                latest.bollingerUpper(),
                latest.bollingerLower()
        );
    }

    public IndicatorSeries calculateSeries(List<Candle> candles, Instant analysisAsOf) {
        validateInput(candles, analysisAsOf);

        BarSeries series = toBarSeries(candles);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        ADXIndicator adx = new ADXIndicator(series, IndicatorSettings.ADX_PERIOD);
        ATRIndicator atr = new ATRIndicator(series, IndicatorSettings.ATR_PERIOD);
        EMAIndicator ema20 = new EMAIndicator(closePrice, IndicatorSettings.EMA_SHORT_PERIOD);
        EMAIndicator ema50 = new EMAIndicator(closePrice, IndicatorSettings.EMA_LONG_PERIOD);
        SMAIndicator bollingerMiddle = new SMAIndicator(closePrice, IndicatorSettings.BOLLINGER_PERIOD);
        StandardDeviationIndicator standardDeviation =
                new StandardDeviationIndicator(closePrice, IndicatorSettings.BOLLINGER_PERIOD);
        Num multiplier = series.numFactory()
                .numOf(IndicatorSettings.BOLLINGER_STANDARD_DEVIATION_MULTIPLIER);
        BollingerBandsMiddleIndicator middleBand = new BollingerBandsMiddleIndicator(bollingerMiddle);
        BollingerBandsUpperIndicator upperBand =
                new BollingerBandsUpperIndicator(middleBand, standardDeviation, multiplier);
        BollingerBandsLowerIndicator lowerBand =
                new BollingerBandsLowerIndicator(middleBand, standardDeviation, multiplier);

        List<IndicatorPoint> points = new java.util.ArrayList<>();
        for (int index = series.getBeginIndex(); index <= series.getEndIndex(); index++) {
            Num closeValue = valueAt(closePrice, index, "Close");
            if (closeValue.isZero()) {
                throw new IndicatorCalculationException(
                        "ATR % cannot be calculated because a close value is zero"
                );
            }
            Num atrValue = valueAtOrNull(atr, index);
            Num atrPercent = atrValue == null
                    ? null
                    : atrValue.dividedBy(closeValue).multipliedBy(series.numFactory().hundred());
            points.add(new IndicatorPoint(
                    decimalValueOrNull(adx, index),
                    decimalValueOrNull(atr, index),
                    atrPercent == null ? null : atrPercent.bigDecimalValue(),
                    decimalValueOrNull(ema20, index),
                    decimalValueOrNull(ema50, index),
                    decimalValueOrNull(middleBand, index),
                    decimalValueOrNull(upperBand, index),
                    decimalValueOrNull(lowerBand, index)
            ));
        }

        IndicatorPoint latest = points.getLast();
        requireLatest(latest, "ADX", latest.adx());
        requireLatest(latest, "ATR", latest.atr());
        requireLatest(latest, "ATR %", latest.atrPercent());
        requireLatest(latest, "EMA20", latest.ema20());
        requireLatest(latest, "EMA50", latest.ema50());
        requireLatest(latest, "Bollinger middle band", latest.bollingerMiddle());
        requireLatest(latest, "Bollinger upper band", latest.bollingerUpper());
        requireLatest(latest, "Bollinger lower band", latest.bollingerLower());
        return new IndicatorSeries(List.copyOf(points));
    }

    private void validateInput(List<Candle> candles, Instant analysisAsOf) {
        if (candles == null || candles.size() < IndicatorSettings.MINIMUM_CANDLE_COUNT) {
            throw new IndicatorCalculationException(
                    "At least " + IndicatorSettings.MINIMUM_CANDLE_COUNT + " confirmed candles are required"
            );
        }
        Objects.requireNonNull(analysisAsOf, "analysisAsOf must not be null");

        Candle previous = null;
        for (Candle candle : candles) {
            if (candle == null) {
                throw new IndicatorCalculationException("Candle data must not contain null values");
            }
            if (candle.openTime() == null || candle.closeTime() == null
                    || candle.closeTime().isBefore(candle.openTime())) {
                throw new IndicatorCalculationException("Candle time range is invalid");
            }
            if (candle.closeTime().isAfter(analysisAsOf)) {
                throw new IndicatorCalculationException("Future or unconfirmed candles cannot be used");
            }
            if (previous != null && !candle.openTime().isAfter(previous.openTime())) {
                throw new IndicatorCalculationException("Candles must be ordered by open time");
            }
            if (candle.open() == null || candle.high() == null || candle.low() == null
                    || candle.close() == null || candle.volume() == null) {
                throw new IndicatorCalculationException("Candle numeric values must not be null");
            }
            previous = candle;
        }
    }

    private BarSeries toBarSeries(List<Candle> candles) {
        NumFactory numFactory = org.ta4j.core.num.DecimalNumFactory.getInstance();
        BarSeries series = new BaseBarSeriesBuilder()
                .withName("market-data")
                .withNumFactory(numFactory)
                .build();

        for (Candle candle : candles) {
            Duration timePeriod = Duration.between(candle.openTime(), candle.closeTime());
            series.addBar(new BaseBar(
                    timePeriod,
                    candle.openTime(),
                    numFactory.numOf(candle.open()),
                    numFactory.numOf(candle.high()),
                    numFactory.numOf(candle.low()),
                    numFactory.numOf(candle.close()),
                    numFactory.numOf(candle.volume()),
                    numFactory.numOf(candle.volume()),
                    candle.tradeCount()
            ));
        }
        return series;
    }

    private BigDecimal decimalValue(Indicator<Num> indicator, int index, String name) {
        return valueAt(indicator, index, name).bigDecimalValue();
    }

    private BigDecimal decimalValueOrNull(Indicator<Num> indicator, int index) {
        Num value = valueAtOrNull(indicator, index);
        return value == null ? null : value.bigDecimalValue();
    }

    private Num valueAtOrNull(Indicator<Num> indicator, int index) {
        Num value = indicator.getValue(index);
        return value == null || value.isNaN() ? null : value;
    }

    private void requireLatest(IndicatorPoint point, String name, BigDecimal value) {
        if (value == null) {
            throw new IndicatorCalculationException(name + " cannot be calculated for the supplied candles");
        }
    }

    private Num valueAt(Indicator<Num> indicator, int index, String name) {
        Num value = indicator.getValue(index);
        if (value == null || value.isNaN()) {
            throw new IndicatorCalculationException(name + " cannot be calculated for the supplied candles");
        }
        return value;
    }
}
