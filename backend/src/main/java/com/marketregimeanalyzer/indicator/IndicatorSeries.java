package com.marketregimeanalyzer.indicator;

import java.util.List;

public record IndicatorSeries(List<IndicatorPoint> points) {

    public IndicatorPoint latest() {
        return points.getLast();
    }
}
