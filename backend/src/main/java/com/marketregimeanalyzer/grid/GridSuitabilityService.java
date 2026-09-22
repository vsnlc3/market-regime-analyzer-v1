package com.marketregimeanalyzer.grid;

import com.marketregimeanalyzer.feature.FeatureSnapshot;
import com.marketregimeanalyzer.regime.MarketRegime;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class GridSuitabilityService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final java.math.MathContext MATH_CONTEXT = java.math.MathContext.DECIMAL128;

    public GridSuitabilityResult calculate(FeatureSnapshot features, MarketRegime regime) {
        if (features == null || regime == null) {
            throw new IllegalArgumentException("FeatureSnapshot and MarketRegime are required");
        }

        BigDecimal trendSafetyScore = clamp(ONE_HUNDRED.subtract(features.trendStrength()), ZERO, ONE_HUNDRED);
        BigDecimal breakoutSafetyScore = clamp(ONE_HUNDRED.subtract(features.breakoutRisk()), ZERO, ONE_HUNDRED);
        BigDecimal volatilityFitnessScore = volatilityFitness(features.volatility());
        BigDecimal baseScore = clamp(
                features.rangeStability().multiply(GridSuitabilitySettings.RANGE_STABILITY_WEIGHT)
                        .add(features.rangeStayRatio().multiply(GridSuitabilitySettings.RANGE_STAY_WEIGHT))
                        .add(features.oscillation().multiply(GridSuitabilitySettings.OSCILLATION_WEIGHT))
                        .add(trendSafetyScore.multiply(GridSuitabilitySettings.TREND_SAFETY_WEIGHT))
                        .add(breakoutSafetyScore.multiply(GridSuitabilitySettings.BREAKOUT_SAFETY_WEIGHT))
                        .add(volatilityFitnessScore.multiply(GridSuitabilitySettings.VOLATILITY_FITNESS_WEIGHT)),
                ZERO,
                ONE_HUNDRED
        );

        BigDecimal finalScore = baseScore.min(BigDecimal.valueOf(scoreCap(regime)));
        int score = finalScore.setScale(0, RoundingMode.HALF_UP).intValueExact();
        return new GridSuitabilityResult(score, level(score), reasons(features, regime));
    }

    private BigDecimal volatilityFitness(BigDecimal volatility) {
        if (volatility.compareTo(GridSuitabilitySettings.VOLATILITY_LOWER_ZERO) <= 0
                || volatility.compareTo(GridSuitabilitySettings.VOLATILITY_UPPER_ZERO) >= 0) {
            return ZERO;
        }
        if (volatility.compareTo(GridSuitabilitySettings.VOLATILITY_OPTIMAL_LOWER) < 0) {
            return clamp(
                    volatility.subtract(GridSuitabilitySettings.VOLATILITY_LOWER_ZERO)
                            .divide(
                                    GridSuitabilitySettings.VOLATILITY_OPTIMAL_LOWER
                                            .subtract(GridSuitabilitySettings.VOLATILITY_LOWER_ZERO),
                                    MATH_CONTEXT
                            )
                            .multiply(ONE_HUNDRED),
                    ZERO,
                    ONE_HUNDRED
            );
        }
        if (volatility.compareTo(GridSuitabilitySettings.VOLATILITY_OPTIMAL_UPPER) <= 0) {
            return ONE_HUNDRED;
        }
        return clamp(
                GridSuitabilitySettings.VOLATILITY_UPPER_ZERO.subtract(volatility)
                        .divide(
                                GridSuitabilitySettings.VOLATILITY_UPPER_ZERO
                                        .subtract(GridSuitabilitySettings.VOLATILITY_OPTIMAL_UPPER),
                                MATH_CONTEXT
                        )
                        .multiply(ONE_HUNDRED),
                ZERO,
                ONE_HUNDRED
        );
    }

    private int scoreCap(MarketRegime regime) {
        return switch (regime) {
            case RANGE -> GridSuitabilitySettings.RANGE_SCORE_CAP;
            case UNSTABLE -> GridSuitabilitySettings.UNSTABLE_SCORE_CAP;
            case TREND -> GridSuitabilitySettings.TREND_SCORE_CAP;
        };
    }

    private GridSuitabilityLevel level(int score) {
        if (score <= 39) {
            return GridSuitabilityLevel.UNSUITABLE;
        }
        if (score <= 59) {
            return GridSuitabilityLevel.LOW;
        }
        if (score <= 79) {
            return GridSuitabilityLevel.MEDIUM;
        }
        return GridSuitabilityLevel.HIGH;
    }

    private List<String> reasons(FeatureSnapshot features, MarketRegime regime) {
        List<String> reasons = new ArrayList<>();
        reasons.add(regimeReason(regime));

        if (features.rangeStability().compareTo(GridSuitabilitySettings.RANGE_STABILITY_REASON_POSITIVE) >= 0) {
            reasons.add("Recent range structure is stable.");
        } else if (features.rangeStability().compareTo(GridSuitabilitySettings.RANGE_STABILITY_REASON_NEGATIVE) < 0) {
            reasons.add("Recent range structure is unstable.");
        }

        if (features.rangeStayRatio().compareTo(GridSuitabilitySettings.RANGE_STAY_REASON_POSITIVE) >= 0) {
            reasons.add("Price has remained inside the recent range.");
        } else if (features.rangeStayRatio().compareTo(GridSuitabilitySettings.RANGE_STAY_REASON_NEGATIVE) < 0) {
            reasons.add("Price frequently leaves the recent range.");
        }

        if (features.oscillation().compareTo(GridSuitabilitySettings.OSCILLATION_REASON_POSITIVE) >= 0) {
            reasons.add("Price oscillation is favorable for repeated grid fills.");
        } else if (features.oscillation().compareTo(GridSuitabilitySettings.OSCILLATION_REASON_NEGATIVE) < 0) {
            reasons.add("Price oscillation is limited.");
        }

        if (features.trendStrength().compareTo(GridSuitabilitySettings.TREND_REASON_NEGATIVE) >= 0) {
            reasons.add("Trend strength is high and may create one-sided grid exposure.");
        } else if (features.trendStrength().compareTo(GridSuitabilitySettings.TREND_REASON_POSITIVE) < 0) {
            reasons.add("Trend strength is low, which is favorable for grid trading.");
        }

        if (features.breakoutRisk().compareTo(GridSuitabilitySettings.BREAKOUT_REASON_NEGATIVE) >= 0) {
            reasons.add("Breakout risk is elevated.");
        } else if (features.breakoutRisk().compareTo(GridSuitabilitySettings.BREAKOUT_REASON_POSITIVE) < 0) {
            reasons.add("Breakout risk is low.");
        }

        if (features.volatility().compareTo(GridSuitabilitySettings.VOLATILITY_LOWER_ZERO) <= 0) {
            reasons.add("Volatility is too low for efficient grid fills.");
        } else if (features.volatility().compareTo(GridSuitabilitySettings.VOLATILITY_OPTIMAL_LOWER) >= 0
                && features.volatility().compareTo(GridSuitabilitySettings.VOLATILITY_OPTIMAL_UPPER) <= 0) {
            reasons.add("Volatility is in a favorable range for grid trading.");
        } else if (features.volatility().compareTo(GridSuitabilitySettings.VOLATILITY_UPPER_ZERO) >= 0) {
            reasons.add("Volatility is too high for grid trading.");
        }
        return reasons;
    }

    private String regimeReason(MarketRegime regime) {
        return switch (regime) {
            case RANGE -> "Market regime is RANGE, which is favorable for grid trading.";
            case TREND -> "Market regime is TREND, which is unfavorable for grid trading.";
            case UNSTABLE -> "Market regime is UNSTABLE, so grid trading risk is elevated.";
        };
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal minimum, BigDecimal maximum) {
        return value.max(minimum).min(maximum);
    }
}
