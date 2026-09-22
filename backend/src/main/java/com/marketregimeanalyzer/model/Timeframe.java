package com.marketregimeanalyzer.model;

import java.util.Arrays;
import java.time.Duration;

public enum Timeframe {
    FIFTEEN_MINUTES("15m", Duration.ofMinutes(15)),
    ONE_HOUR("1h", Duration.ofHours(1)),
    FOUR_HOURS("4h", Duration.ofHours(4)),
    ONE_DAY("1d", Duration.ofDays(1));

    private final String value;
    private final Duration duration;

    Timeframe(String value, Duration duration) {
        this.value = value;
        this.duration = duration;
    }

    public String value() {
        return value;
    }

    public Duration duration() {
        return duration;
    }

    public static Timeframe fromValue(String value) {
        return Arrays.stream(values())
                .filter(timeframe -> timeframe.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported timeframe: " + value));
    }
}
