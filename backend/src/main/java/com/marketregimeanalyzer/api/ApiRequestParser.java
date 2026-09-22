package com.marketregimeanalyzer.api;

import com.marketregimeanalyzer.model.Timeframe;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class ApiRequestParser {

    public static final int DEFAULT_WINDOW = 168;
    public static final int MINIMUM_WINDOW = 50;
    public static final int MAXIMUM_WINDOW = 5000;
    public static final String DEFAULT_TIMEFRAME = "1h";

    private static final Set<String> SUPPORTED_SYMBOLS = Set.of("BTC", "ETH", "SOL", "XRP");

    public ApiRequest parse(String symbolValue, String timeframeValue, String windowValue) {
        String symbol = normalizeSymbol(symbolValue);
        Timeframe timeframe = parseTimeframe(timeframeValue);
        int window = parseWindow(windowValue);
        return new ApiRequest(symbol, timeframe, window);
    }

    private String normalizeSymbol(String value) {
        if (value == null || value.isBlank()) {
            throw invalid("INVALID_SYMBOL", "Symbol must not be blank");
        }
        String symbol = value.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_SYMBOLS.contains(symbol)) {
            throw invalid("INVALID_SYMBOL", "Unsupported symbol: " + value);
        }
        return symbol;
    }

    private Timeframe parseTimeframe(String value) {
        if (value == null || value.isBlank()) {
            throw invalid("INVALID_TIMEFRAME", "Timeframe must not be blank");
        }
        try {
            return Timeframe.fromValue(value);
        } catch (IllegalArgumentException exception) {
            throw invalid("INVALID_TIMEFRAME", "Unsupported timeframe: " + value);
        }
    }

    private int parseWindow(String value) {
        if (value == null || value.isBlank()) {
            throw invalid("INVALID_WINDOW", "Window must be a number between 50 and 5000");
        }
        final int window;
        try {
            window = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw invalid("INVALID_WINDOW", "Window must be a number between 50 and 5000");
        }
        if (window < MINIMUM_WINDOW || window > MAXIMUM_WINDOW) {
            throw invalid("INVALID_WINDOW", "Window must be between 50 and 5000 candles");
        }
        return window;
    }

    private ApiException invalid(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }
}
