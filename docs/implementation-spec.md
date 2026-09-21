# Market Regime Analyzer 実装仕様

## 1. 方針

Market Regime Analyzerは、暗号資産の市場データを取得・分析し、現在のMarket RegimeおよびGrid Trading適性を返す独立システムとする。

MVPでは市場分析のみを担当し、実際の売買機能は持たない。

将来的には複数のTrading Strategyに対する適性を判定するStrategy Selectorへ拡張する。

---

## 2. 技術スタック

### Backend

* Java 21
* Spring Boot
* Spring Web
* Spring RestClient
* Spring Data JPA
* PostgreSQL
* Flyway
* ta4j
* JUnit 5
* Testcontainers

BackendはJava / Spring Bootで構築する。

Hyperliquid REST APIとの通信にはSpring `RestClient` を使用する。

テクニカル指標については `ta4j` を利用できるものは活用し、Market Regime判定に必要な独自Featureはアプリケーション側で実装する。

将来Machine Learningが必要になった場合は、必要に応じてPythonによる分析サービスを別途追加する。

### Frontend

* Next.js
* TypeScript
* React
* Tailwind CSS
* shadcn/ui
* Lightweight Charts

MVP初期ではBackendを優先し、Dashboardは分析API完成後に実装する。

### Infrastructure

* Docker
* Docker Compose

Backend / Frontend / PostgreSQLをDocker Composeで起動可能とする。

---

## 3. 外部API

### Market Data Provider

MVPではHyperliquid Public APIを使用する。

認証が不要なPublic Market Dataのみ利用する。

以下は使用しない。

* 注文API
* Wallet接続
* Private Key
* 資産残高操作

MVPではHyperliquid Perpetual MarketのMarket Dataを分析対象とする。

Perpetual Marketを分析対象とするが、実際のFutures Tradingは行わない。

### Historical / Recent Data

Hyperliquid `candleSnapshot` を使用する。

取得対象：

* Open
* High
* Low
* Close
* Volume
* Trade Count
* Open Time
* Close Time

Hyperliquid APIから取得した確定済みCandleは自前DBへ保存する。

保存したCandleは以下に利用する。

* Market Regime分析
* Grid Suitability算出
* Historical Data蓄積
* Backtest

未確定Candleを扱う場合は、同一Candleを更新可能な構造とする。

### Realtime

将来的にHyperliquid WebSocketの `candle` subscriptionを利用する。

MVP初期ではREST APIのみを利用する。

---

## 4. 標準分析条件

```text
Symbol: BTC
Timeframe: 1h
Window: 168 candles
```

標準では直近7日間を分析する。

以下は変更可能とする。

* Symbol
* Timeframe
* Window

---

## 5. アーキテクチャ

```text
Hyperliquid
     ↓
MarketDataProvider
     ↓
Candle Repository
     ↓
Indicator Service
     ↓
Feature Service
     ↓
Market Regime Analyzer
     ↓
Grid Suitability Service
     ↓
REST API
     ↓
Dashboard / Trading Bot
```

各責務を分離する。

Market Regime AnalyzerからHyperliquid APIを直接呼ばない。

外部APIへの依存はMarket Data Providerへ隔離する。

---

## 6. ディレクトリ構成

```text
market-regime-analyzer-v1/
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── .../
│   │   │   │       ├── api/
│   │   │   │       ├── marketdata/
│   │   │   │       ├── indicator/
│   │   │   │       ├── feature/
│   │   │   │       ├── analyzer/
│   │   │   │       ├── repository/
│   │   │   │       ├── model/
│   │   │   │       └── config/
│   │   │   │
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/
│   │   │
│   │   └── test/
│   │       └── java/
│   │
│   └── pom.xml
│
├── frontend/
│
├── docs/
│   ├── requirements.md
│   └── implementation-spec.md
│
├── docker-compose.yml
├── AGENTS.md
└── README.md
```

---

## 7. Market Data Provider

外部APIへの依存を分離するため、Market Data取得用のInterfaceを定義する。

イメージ：

```java
List<Candle> getCandles(
    String symbol,
    Timeframe timeframe,
    Instant startTime,
    Instant endTime
);
```

MVP実装：

```text
HyperliquidMarketDataProvider
```

将来的に以下のProviderを追加可能とする。

```text
BinanceMarketDataProvider
HistoricalDataProvider
CsvMarketDataProvider
```

Analyzerはデータ取得元を意識しない設計とする。

---

## 8. Candleデータ

最低限以下を保持する。

```text
exchange
symbol
timeframe
openTime
closeTime
open
high
low
close
volume
tradeCount
```

価格・数量など、誤差を避けたい値には `BigDecimal` を使用する。

以下の組み合わせを一意とする。

```text
exchange
symbol
timeframe
openTime
```

同じCandleを再取得した場合は重複登録しない。

確定済みCandleは原則変更しない。

未確定Candleを保存する場合は更新可能な構造とする。

---

## 9. Indicator Service

MVPでは以下を扱う。

* ADX
* ATR
* ATR %
* EMA
* Bollinger Band

可能なものは `ta4j` を利用する。

Indicator Serviceは数値計算のみを担当する。

Market Regime判定やGrid Suitability判定は行わない。

---

## 10. Feature Service

IndicatorおよびOHLCVからMarket Regime判定用のFeatureを算出する。

MVP対象：

* Trend Strength
* Volatility
* Efficiency Ratio
* Range Stability
* Range Stay Ratio
* Reversal Count
* Oscillation Score
* Range Break Count
* Breakout Risk

必要に応じて0〜100のScoreへ正規化する。

IndicatorとFeatureは明確に分離する。

例：

```text
ADX
EMA
Efficiency Ratio
      ↓
Trend Strength
```

Feature Serviceは市場の特徴を数値化する責務を持つが、最終的なMarket Regime判定は行わない。

---

## 11. Market Regime Analyzer

MVPでは以下のMarket Regimeを判定する。

```text
RANGE
TREND
UNSTABLE
```

初期実装はMachine Learningではなくルールベースとする。

例：

```text
Trend Strength: LOW
Range Stability: HIGH
Oscillation: HIGH

↓

RANGE
```

別例：

```text
Trend Strength: HIGH
Efficiency Ratio: HIGH

↓

TREND
```

別例：

```text
Volatility: EXTREME
Breakout Risk: HIGH

↓

UNSTABLE
```

閾値やWeightはロジック内へ散在させず、設定値として管理する。

---

## 12. Grid Suitability Service

現在の市場がGrid Tradingに適している度合いを0〜100で返す。

主な判断材料：

```text
Trend Strength
Volatility
Range Stability
Oscillation
Breakout Risk
```

基本的な考え方：

```text
弱いTrend
+
適度なVolatility
+
高いRange Stability
+
高いOscillation
+
低いBreakout Risk

↓

高いGrid Suitability
```

Score計算式やWeightは固定的な正解とせず、Backtest結果をもとに調整する。

Score例：

```text
0 - 39   UNSUITABLE
40 - 59  LOW
60 - 79  MEDIUM
80 - 100 HIGH
```

閾値についてもBacktest結果に応じて変更可能とする。

---

## 13. 判定理由

Grid Suitability Scoreだけでなく、判定根拠も返す。

例：

```text
Grid Suitability: 86

Reasons:
- Directional trend is weak
- Price remains inside a stable range
- Reversal frequency is high
- Volatility is sufficient
```

MVPではルールベースで判定理由を生成する。

LLMに直接Market Regimeや売買判断をさせない。

将来的にLLMを利用する場合も、Analyzerが算出した構造化データを自然言語で説明する用途に限定する。

---

## 14. API

### Analysis

```text
GET /api/v1/analysis/{symbol}
```

Query：

```text
timeframe=1h
window=168
```

レスポンス例：

```json
{
  "symbol": "BTC",
  "timeframe": "1h",
  "window": 168,
  "regime": "RANGE",
  "gridSuitability": 86,
  "gridSuitabilityLevel": "HIGH",
  "features": {
    "trendStrength": 18,
    "volatility": 64,
    "rangeStability": 84,
    "oscillation": 91,
    "breakoutRisk": 21
  },
  "reasons": [
    "Directional trend is weak",
    "Price remains inside a stable range",
    "Reversal frequency is high"
  ]
}
```

### Health Check

```text
GET /health
```

---

## 15. Backtest

本番分析と同じAnalyzerを過去データに対しても利用できる構造とする。

Backtest専用にMarket Regime判定ロジックを二重実装しない。

比較対象：

```text
Always Grid

vs

Grid Suitabilityが一定以上の場合のみGrid
```

例：

```text
Grid Suitability >= 80
→ Grid実行

Grid Suitability < 80
→ No Trade
```

評価対象：

* PnL
* Max Drawdown
* Trade Count
* Fees
* Profit Factor

必要に応じて以下も追加可能とする。

* Win Rate
* Sharpe Ratio

Analyzerの判定時点より未来のデータを入力として使用しない。

未来データの混入を防止し、Look-Ahead Biasが発生しないようにする。

---

## 16. Historical Data方針

Hyperliquidから取得したCandleを継続的にDBへ蓄積する。

```text
Hyperliquid
     ↓
Candle取得
     ↓
PostgreSQL
     ↓
Historical Dataset
```

このデータを将来のBacktestやMachine Learning用データとして利用する。

MVPではHyperliquidから取得可能な範囲のHistorical Dataを利用してBacktestする。

より長期間のデータが必要になった場合は、別のHistorical Data Providerを追加する。

Analyzer側の分析ロジックはData Providerの違いを意識しない設計とする。

---

## 17. MVP実装順序

### Step 1

Spring Boot Backendプロジェクトを作成する。

最低限以下を確認する。

```text
アプリケーション起動
GET /health
Unit Test実行
```

この段階ではDB・Indicator・Analyzer・Frontendを実装しない。

### Step 2

Hyperliquid `candleSnapshot` へ接続する。

以下を取得できる状態にする。

```text
BTC
1h
168 candles
```

取得結果を内部のCandleモデルへ変換する。

この段階ではDB保存を行わなくてもよい。

### Step 3

PostgreSQL・Spring Data JPA・Flywayを導入する。

取得したCandleをDBへ保存できるようにする。

以下を保証する。

```text
同一Candleの重複登録防止
確定済みCandleの保存
未確定Candleの更新
```

### Step 4

Indicator Serviceを実装する。

```text
ADX
ATR
ATR %
EMA
Bollinger Band
```

### Step 5

Feature Serviceを実装する。

```text
Trend Strength
Volatility
Efficiency Ratio
Range Stability
Range Stay Ratio
Reversal Count
Oscillation
Range Break Count
Breakout Risk
```

### Step 6

Market Regime Analyzerを実装する。

```text
RANGE
TREND
UNSTABLE
```

### Step 7

Grid Suitability Serviceを実装する。

```text
0 ～ 100
```

のScoreと判定理由を返す。

### Step 8

Analysis APIを実装する。

```text
GET /api/v1/analysis/{symbol}
```

### Step 9

Backtest機能を実装する。

```text
Always Grid

vs

Analyzer + Grid
```

を比較可能にする。

### Step 10

Dashboardを実装する。

---

## 18. Dashboard

MVPのDashboardでは最低限以下を表示する。

* Symbol
* Current Price
* Timeframe
* Analysis Window
* Market Regime
* Grid Suitability Score
* Grid Suitability Level
* Trend Strength
* Volatility
* Range Stability
* Oscillation
* Breakout Risk
* 判定理由
* ローソク足チャート

ローソク足チャートには `Lightweight Charts` を使用する。

将来的には以下のOverlayを検討する。

* EMA
* Bollinger Band
* Range Upper
* Range Lower
* Reversal Point
* Breakout Point

---

## 19. MVPで実装しないもの

以下はMVPへ追加しない。

* 実資金取引
* Hyperliquid注文API
* Wallet接続
* Private Key管理
* Leverage
* Strategy自動切替
* Machine Learning
* LLMによる市場判定
* Momentum Bot
* Breakout Bot
* Mean Reversion Bot
* DCA
* Rebalancing
* Funding Arbitrage
* Portfolio Management
* 自動Position Size算出
* Stop Loss管理
* Take Profit管理

これらはMVP完成後に必要性を検討する。

---

## 20. 実装上の重要方針

* AnalyzerとTrading Botを分離する
* Analyzerは注文を実行しない
* 外部API依存をMarket Data Providerへ隔離する
* AnalyzerからHyperliquid APIを直接呼ばない
* IndicatorとFeatureを分離する
* FeatureとMarket Regime判定を分離する
* Market RegimeとGrid Suitabilityを分離する
* 本番分析とBacktestで同じ分析ロジックを利用する
* Scoreの根拠を説明可能にする
* 金額・価格などには必要に応じて `BigDecimal` を使用する
* 時刻は原則UTCで扱う
* 閾値やWeightをロジック内へ散在させない
* MVP外の機能を先回りして実装しない
* 将来のStrategy追加やMachine Learningで再利用可能な構造にする
* 過剰な抽象化を避ける
* シンプルさを優先する
* 主要な分析ロジックにはUnit Testを書く
* 外部API連携には異常系のTestを用意する
* Backtestで未来データを利用しない
* 秘密情報をGit RepositoryへCommitしない

---

## 21. MVP完了条件

以下を満たした時点でMarket Regime Analyzer v0.1を完成とする。

1. Spring Boot Backendが起動できる
2. HyperliquidからOHLCVを取得できる
3. CandleをPostgreSQLへ保存できる
4. ADX / ATR / EMA / Bollinger Bandを算出できる
5. Market Regime用Featureを算出できる
6. RANGE / TREND / UNSTABLEを判定できる
7. Grid Suitability Scoreを算出できる
8. 判定理由を取得できる
9. REST APIから分析結果を取得できる
10. Dashboardで分析結果を確認できる
11. 過去データを使用してBacktestできる
12. Always Gridとの結果比較ができる

---

## 22. 将来構想

MVP完成後、Market Regime Analyzerを以下へ発展させる。

```text
Market Data
     ↓
Indicator
     ↓
Feature
     ↓
Market Regime Analyzer
     ↓
Strategy Selector
     ↓
┌──────────────────────┐
│ Grid                 │
│ Mean Reversion       │
│ Trend Following      │
│ Momentum             │
│ Breakout             │
│ No Trade             │
└──────────────────────┘
     ↓
Risk Engine
     ↓
Trading Bot
```

最終的には、

**現在の市場環境を分析し、その環境に適したTrading Strategyを選択するシステム**

へ発展させる。

Market Regime Analyzerは、その中核となる市場分析システムとして独立して利用可能な構造を維持する。
