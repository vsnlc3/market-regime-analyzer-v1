package com.marketregimeanalyzer.marketdata.hyperliquid;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HyperliquidCandleResponse(
        @JsonProperty("T") Long closeTime,
        @JsonProperty("c") String close,
        @JsonProperty("h") String high,
        @JsonProperty("i") String interval,
        @JsonProperty("l") String low,
        @JsonProperty("n") Long tradeCount,
        @JsonProperty("o") String open,
        @JsonProperty("s") String symbol,
        @JsonProperty("t") Long openTime,
        @JsonProperty("v") String volume
) {
}
