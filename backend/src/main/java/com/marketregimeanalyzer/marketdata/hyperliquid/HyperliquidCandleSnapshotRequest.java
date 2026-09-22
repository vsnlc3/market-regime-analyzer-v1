package com.marketregimeanalyzer.marketdata.hyperliquid;

public record HyperliquidCandleSnapshotRequest(
        String type,
        Request req
) {

    public static HyperliquidCandleSnapshotRequest of(
            String symbol,
            String interval,
            long startTime,
            long endTime
    ) {
        return new HyperliquidCandleSnapshotRequest(
                "candleSnapshot",
                new Request(symbol, interval, startTime, endTime)
        );
    }

    public record Request(
            String coin,
            String interval,
            long startTime,
            long endTime
    ) {
    }
}
