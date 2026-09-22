package com.marketregimeanalyzer.grid;

import com.marketregimeanalyzer.feature.FeatureSnapshot;
import com.marketregimeanalyzer.regime.MarketRegime;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GridSuitabilityServiceTest {

    private final GridSuitabilityService service = new GridSuitabilityService();

    @Test
    void calculatesBaseScoreWithConfiguredWeights() {
        GridSuitabilityResult result = service.calculate(
                features("20", "50", "80", "70", "60", "30"),
                MarketRegime.RANGE
        );

        assertThat(result.score()).isEqualTo(74);
    }

    @Test
    void convertsTrendAndBreakoutRiskToSafetyScores() {
        GridSuitabilityResult favorable = service.calculate(
                features("0", "40", "100", "100", "100", "0"),
                MarketRegime.RANGE
        );
        GridSuitabilityResult highTrend = service.calculate(
                features("100", "40", "100", "100", "100", "0"),
                MarketRegime.RANGE
        );
        GridSuitabilityResult highBreakoutRisk = service.calculate(
                features("0", "40", "100", "100", "100", "100"),
                MarketRegime.RANGE
        );

        assertThat(favorable.score()).isEqualTo(100);
        assertThat(highTrend.score()).isEqualTo(85);
        assertThat(highBreakoutRisk.score()).isEqualTo(85);
    }

    @Test
    void appliesVolatilityFitnessAtBoundariesAndInterpolationPoints() {
        assertThat(scoreForVolatility("20")).isEqualTo(95);
        assertThat(scoreForVolatility("30")).isEqualTo(98);
        assertThat(scoreForVolatility("40")).isEqualTo(100);
        assertThat(scoreForVolatility("70")).isEqualTo(100);
        assertThat(scoreForVolatility("80")).isEqualTo(98);
        assertThat(scoreForVolatility("90")).isEqualTo(95);
    }

    @Test
    void clampsVolatilityFitnessOutsideConfiguredRange() {
        assertThat(scoreForVolatility("-10")).isEqualTo(95);
        assertThat(scoreForVolatility("110")).isEqualTo(95);
    }

    @Test
    void appliesRegimeCapsAndLevels() {
        FeatureSnapshot favorable = features("0", "40", "100", "100", "100", "0");

        GridSuitabilityResult range = service.calculate(favorable, MarketRegime.RANGE);
        GridSuitabilityResult unstable = service.calculate(favorable, MarketRegime.UNSTABLE);
        GridSuitabilityResult trend = service.calculate(favorable, MarketRegime.TREND);

        assertThat(range.score()).isEqualTo(100);
        assertThat(range.level()).isEqualTo(GridSuitabilityLevel.HIGH);
        assertThat(unstable.score()).isEqualTo(59);
        assertThat(unstable.level()).isEqualTo(GridSuitabilityLevel.LOW);
        assertThat(trend.score()).isEqualTo(39);
        assertThat(trend.level()).isEqualTo(GridSuitabilityLevel.UNSUITABLE);
    }

    @Test
    void classifiesAllScoreLevelBoundaries() {
        assertThat(resultForBoundary("0").level()).isEqualTo(GridSuitabilityLevel.UNSUITABLE);
        assertThat(resultForBoundary("39").level()).isEqualTo(GridSuitabilityLevel.UNSUITABLE);
        assertThat(resultForBoundary("40").level()).isEqualTo(GridSuitabilityLevel.LOW);
        assertThat(resultForBoundary("59").level()).isEqualTo(GridSuitabilityLevel.LOW);
        assertThat(resultForBoundary("60").level()).isEqualTo(GridSuitabilityLevel.MEDIUM);
        assertThat(resultForBoundary("79").level()).isEqualTo(GridSuitabilityLevel.MEDIUM);
        assertThat(resultForBoundary("80").level()).isEqualTo(GridSuitabilityLevel.HIGH);
        assertThat(resultForBoundary("100").level()).isEqualTo(GridSuitabilityLevel.HIGH);

        assertThat(resultForBoundary("0").score()).isEqualTo(0);
        assertThat(resultForBoundary("39").score()).isEqualTo(39);
        assertThat(resultForBoundary("40").score()).isEqualTo(40);
        assertThat(resultForBoundary("59").score()).isEqualTo(59);
        assertThat(resultForBoundary("60").score()).isEqualTo(60);
        assertThat(resultForBoundary("79").score()).isEqualTo(79);
        assertThat(resultForBoundary("80").score()).isEqualTo(80);
        assertThat(resultForBoundary("100").score()).isEqualTo(100);
    }

    @Test
    void roundsOnlyTheFinalScoreHalfUp() {
        GridSuitabilityResult belowHalf = service.calculate(
                features("100", "0", "100", "100", "100", "4"),
                MarketRegime.RANGE
        );
        GridSuitabilityResult atHalf = service.calculate(
                features("100", "0", "100", "100", "100", "3.333333333333333333333333333333333"),
                MarketRegime.RANGE
        );

        assertThat(belowHalf.score()).isEqualTo(79);
        assertThat(atHalf.score()).isEqualTo(80);
    }

    @Test
    void returnsDeterministicReasonsInConfiguredOrder() {
        GridSuitabilityResult range = service.calculate(
                features("30", "50", "80", "85", "65", "30"),
                MarketRegime.RANGE
        );
        GridSuitabilityResult trend = service.calculate(
                features("75", "30", "55", "60", "55", "30"),
                MarketRegime.TREND
        );
        GridSuitabilityResult unstable = service.calculate(
                features("70", "90", "30", "60", "30", "70"),
                MarketRegime.UNSTABLE
        );

        assertThat(range.reasons()).containsExactly(
                "Market regime is RANGE, which is favorable for grid trading.",
                "Recent range structure is stable.",
                "Price has remained inside the recent range.",
                "Price oscillation is favorable for repeated grid fills.",
                "Trend strength is low, which is favorable for grid trading.",
                "Breakout risk is low.",
                "Volatility is in a favorable range for grid trading."
        );
        assertThat(trend.reasons()).containsExactly(
                "Market regime is TREND, which is unfavorable for grid trading.",
                "Trend strength is high and may create one-sided grid exposure.",
                "Breakout risk is low."
        );
        assertThat(unstable.reasons()).containsExactly(
                "Market regime is UNSTABLE, so grid trading risk is elevated.",
                "Recent range structure is unstable.",
                "Price oscillation is limited.",
                "Trend strength is high and may create one-sided grid exposure.",
                "Breakout risk is elevated.",
                "Volatility is too high for grid trading."
        );
    }

    private GridSuitabilityResult resultForBoundary(String target) {
        return switch (target) {
            case "0" -> service.calculate(features("100", "0", "0", "0", "0", "100"), MarketRegime.RANGE);
            case "39" -> service.calculate(features("100", "0", "100", "0", "70", "100"), MarketRegime.RANGE);
            case "40" -> service.calculate(features("100", "0", "100", "75", "0", "100"), MarketRegime.RANGE);
            case "59" -> service.calculate(features("100", "0", "100", "100", "70", "100"), MarketRegime.RANGE);
            case "60" -> service.calculate(features("100", "0", "100", "100", "75", "100"), MarketRegime.RANGE);
            case "79" -> service.calculate(features(
                    "100", "0", "100", "100", "100", "6.67"), MarketRegime.RANGE);
            case "80" -> service.calculate(features("0", "0", "100", "100", "100", "100"), MarketRegime.RANGE);
            case "100" -> service.calculate(features("0", "40", "100", "100", "100", "0"), MarketRegime.RANGE);
            default -> throw new IllegalArgumentException("Unknown boundary: " + target);
        };
    }

    private int scoreForVolatility(String volatility) {
        return service.calculate(
                features("0", volatility, "100", "100", "100", "0"),
                MarketRegime.RANGE
        ).score();
    }

    private static FeatureSnapshot features(
            String trendStrength,
            String volatility,
            String rangeStability,
            String rangeStayRatio,
            String oscillation,
            String breakoutRisk
    ) {
        return new FeatureSnapshot(
                new BigDecimal(trendStrength),
                new BigDecimal(volatility),
                new BigDecimal("0.5"),
                new BigDecimal(rangeStability),
                new BigDecimal(rangeStayRatio),
                0,
                new BigDecimal(oscillation),
                0,
                new BigDecimal(breakoutRisk)
        );
    }
}
