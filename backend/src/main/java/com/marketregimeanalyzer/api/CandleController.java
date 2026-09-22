package com.marketregimeanalyzer.api;

import com.marketregimeanalyzer.api.dto.CandleResponse;
import com.marketregimeanalyzer.marketdata.CandleQueryService;
import com.marketregimeanalyzer.model.Candle;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/candles")
public class CandleController {

    private final ApiRequestParser apiRequestParser;
    private final CandleQueryService candleQueryService;

    public CandleController(ApiRequestParser apiRequestParser, CandleQueryService candleQueryService) {
        this.apiRequestParser = apiRequestParser;
        this.candleQueryService = candleQueryService;
    }

    @GetMapping("/{symbol}")
    public List<CandleResponse> getCandles(
            @PathVariable String symbol,
            @RequestParam(defaultValue = ApiRequestParser.DEFAULT_TIMEFRAME) String timeframe,
            @RequestParam(defaultValue = "168") String window
    ) {
        ApiRequest request = apiRequestParser.parse(symbol, timeframe, window);
        return candleQueryService.getConfirmedCandles(request)
                .stream()
                .map(CandleResponse::from)
                .toList();
    }
}
