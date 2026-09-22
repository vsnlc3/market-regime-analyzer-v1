package com.marketregimeanalyzer.marketdata.hyperliquid;

import com.marketregimeanalyzer.marketdata.MarketDataException;
import com.marketregimeanalyzer.model.Candle;
import com.marketregimeanalyzer.model.Timeframe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HyperliquidMarketDataProviderTest {

    private static final Instant START = Instant.parse("2026-09-21T00:00:00Z");
    private static final Instant END = Instant.parse("2026-09-22T00:00:00Z");

    private MockRestServiceServer server;
    private HyperliquidMarketDataProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new HyperliquidMarketDataProvider(
                builder,
                "http://hyperliquid.test",
                new HyperliquidCandleMapper()
        );
    }

    @Test
    void mapsCandleSnapshotResponseToInternalCandle() {
        server.expect(requestTo("http://hyperliquid.test/info"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "type": "candleSnapshot",
                          "req": {
                            "coin": "BTC",
                            "interval": "1h",
                            "startTime": 1789948800000,
                            "endTime": 1790035200000
                          }
                        }
                        """))
                .andRespond(withSuccess("""
                        [
                          {
                            "T": 1790035199999,
                            "c": "112430.10",
                            "h": "112500.20",
                            "i": "1h",
                            "l": "112300.00",
                            "n": 1234,
                            "o": "112350.00",
                            "s": "BTC",
                            "t": 1790031600000,
                            "v": "987.654321"
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<Candle> candles = provider.getCandles(" btc ", Timeframe.ONE_HOUR, START, END);

        assertThat(candles).hasSize(1);
        Candle candle = candles.getFirst();
        assertThat(candle.exchange()).isEqualTo("HYPERLIQUID");
        assertThat(candle.symbol()).isEqualTo("BTC");
        assertThat(candle.timeframe()).isEqualTo(Timeframe.ONE_HOUR);
        assertThat(candle.openTime()).isEqualTo(Instant.ofEpochMilli(1790031600000L));
        assertThat(candle.closeTime()).isEqualTo(Instant.ofEpochMilli(1790035199999L));
        assertThat(candle.open()).isEqualByComparingTo(new BigDecimal("112350.00"));
        assertThat(candle.high()).isEqualByComparingTo(new BigDecimal("112500.20"));
        assertThat(candle.low()).isEqualByComparingTo(new BigDecimal("112300.00"));
        assertThat(candle.close()).isEqualByComparingTo(new BigDecimal("112430.10"));
        assertThat(candle.volume()).isEqualByComparingTo(new BigDecimal("987.654321"));
        assertThat(candle.tradeCount()).isEqualTo(1234L);
        server.verify();
    }

    @Test
    void wrapsHyperliquidHttpErrors() {
        server.expect(requestTo("http://hyperliquid.test/info"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> provider.getCandles("BTC", Timeframe.ONE_HOUR, START, END))
                .isInstanceOf(MarketDataException.class)
                .hasMessageContaining("Failed to retrieve candles");
        server.verify();
    }

    @Test
    void rejectsAnInvalidTimeRangeBeforeCallingHyperliquid() {
        assertThatThrownBy(() -> provider.getCandles("BTC", Timeframe.ONE_HOUR, END, START))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endTime must be after startTime");
    }
}
