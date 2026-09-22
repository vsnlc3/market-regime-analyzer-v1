package com.marketregimeanalyzer.grid;

import java.util.List;

public record GridSuitabilityResult(
        int score,
        GridSuitabilityLevel level,
        List<String> reasons
) {
    public GridSuitabilityResult {
        reasons = List.copyOf(reasons);
    }
}
