package com.marketregimeanalyzer.repository;

import com.marketregimeanalyzer.model.Candle;
import com.marketregimeanalyzer.model.Timeframe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
class CandleRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private CandleRepository candleRepository;

    @Autowired
    private CandleEntityMapper candleEntityMapper;

    @BeforeEach
    void cleanUp() {
        candleRepository.deleteAll();
    }

    @Test
    void savesAndFindsCandleByIdentity() {
        Candle candle = candle();
        candleRepository.saveAndFlush(candleEntityMapper.toEntity(candle));

        CandleEntity stored = candleRepository
                .findByExchangeAndSymbolAndTimeframeAndOpenTime(
                        candle.exchange(), candle.symbol(), candle.timeframe(), candle.openTime()
                )
                .orElseThrow();

        assertThat(candleEntityMapper.toModel(stored)).isEqualTo(candle);
    }

    @Test
    void preventsDuplicateCandles() {
        Candle candle = candle();
        candleRepository.saveAndFlush(candleEntityMapper.toEntity(candle));

        assertThatThrownBy(() -> candleRepository.saveAndFlush(candleEntityMapper.toEntity(candle)))
                .hasRootCauseInstanceOf(org.postgresql.util.PSQLException.class);
    }

    private static Candle candle() {
        Instant openTime = Instant.parse("2026-09-22T00:00:00Z");
        return new Candle(
                "HYPERLIQUID",
                "BTC",
                Timeframe.ONE_HOUR,
                openTime,
                openTime.plusSeconds(3599),
                new BigDecimal("112350.00"),
                new BigDecimal("112500.20"),
                new BigDecimal("112300.00"),
                new BigDecimal("112430.10"),
                new BigDecimal("987.654321"),
                1234L
        );
    }
}
