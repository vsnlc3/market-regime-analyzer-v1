package com.marketregimeanalyzer.api;

import com.marketregimeanalyzer.model.Timeframe;

public record ApiRequest(
        String symbol,
        Timeframe timeframe,
        int window
) {
}
