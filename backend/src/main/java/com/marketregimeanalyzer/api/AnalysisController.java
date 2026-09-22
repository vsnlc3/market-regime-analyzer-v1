package com.marketregimeanalyzer.api;

import com.marketregimeanalyzer.analysis.AnalysisService;
import com.marketregimeanalyzer.api.dto.AnalysisResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final ApiRequestParser apiRequestParser;
    private final AnalysisService analysisService;

    public AnalysisController(ApiRequestParser apiRequestParser, AnalysisService analysisService) {
        this.apiRequestParser = apiRequestParser;
        this.analysisService = analysisService;
    }

    @GetMapping("/{symbol}")
    public AnalysisResponse getAnalysis(
            @PathVariable String symbol,
            @RequestParam(defaultValue = ApiRequestParser.DEFAULT_TIMEFRAME) String timeframe,
            @RequestParam(defaultValue = "168") String window
    ) {
        ApiRequest request = apiRequestParser.parse(symbol, timeframe, window);
        return analysisService.analyze(request);
    }
}
