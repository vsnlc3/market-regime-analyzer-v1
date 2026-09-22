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

Frontend UI Prototypeは先行して実装済みである。Backend API完成後に、既存FrontendのMock DataをAPIへ置換する。API接続時も画面デザインを作り直すのではなく、既存UIを可能な限り維持する。

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

FrontendではAnalysis Windowを期間として表示する。Backend APIの `window` はCandle本数として扱い、FrontendのAPI境界でAnalysis WindowとTimeframeから必要Candle数へ変換する。

例：

```text
1h + 7D  → 168 candles
15m + 7D → 672 candles
4h + 7D  → 42 candles
1d + 7D  → 7 candles
```

Frontend URL上の `tf` / `win` はUI内部の表現とし、Backend APIの `timeframe` / `window` とは分離する。変換処理はComponentではなく、Frontend API ClientまたはAdapterで扱う。

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
Dashboard
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

### Indicatorの初期計算条件

以下はMVPの初期値とする。最適値として固定せず、将来のBacktest結果をもとに調整可能な値として扱う。

* ADXはPeriod 14、OHLCを使用し、ta4jで利用可能な実装を使用する。
* ATRはPeriod 14、OHLCを使用し、ta4jで利用可能な実装を使用する。
* ATR %は `ATR(14) / Close × 100` で算出する。Closeは対象Candleの終値とし、計算時に丸めない。Closeが0など計算不能な場合は、0として扱わず算出不能とする。
* EMAはClose系列からEMA20とEMA50を算出する。
* Bollinger BandはClose系列、Period 20、Standard Deviation Multiplier 2.0を使用する。Middle BandはSMA20、Upper Bandは `SMA20 + 2σ`、Lower Bandは `SMA20 - 2σ` とする。
* Indicator計算対象は確定済みCandleのみとし、未来のCandleや未確定Candleを使用しない。
* Indicator一式の算出に必要な最低Candle数は50本とする。50本未満の場合はダミー値を返さず、算出不能として扱う。
* Candleの価格・数量はBigDecimalを維持し、Indicator計算途中で表示用の丸めを行わない。Threshold判定等では丸め前の値を利用できるようにする。
* 上記のPeriod、Multiplier、最低Candle数はIndicator計算に必要な設定値として一箇所で管理し、ロジック内へ散在させない。

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

### Featureの初期計算条件

以下はMVPの初期仕様とする。最適値として固定せず、将来のBacktest結果をもとに調整可能な値として扱う。

Feature計算には分析時点までに確定しているCandleだけを使用する。未来のCandleや未確定Candleは使用しない。基本Lookbackは直近50本とし、50本未満の場合はダミー値を返さず算出不能として扱う。0〜100のScoreは最終的に0〜100へClampし、計算途中では表示用の丸めを行わない。

Featureの内部結果は、以下の値を扱う。

* `trendStrength`: 0〜100
* `volatility`: 0〜100
* `efficiencyRatio`: 0〜1
* `rangeStability`: 0〜100
* `rangeStayRatio`: 0〜100
* `reversalCount`: integer
* `oscillation`: 0〜100
* `rangeBreakCount`: integer
* `breakoutRisk`: 0〜100

#### Efficiency Ratio

Periodは20本とし、次の式で算出する。

```text
ER = abs(Close[t] - Close[t-20])
     / sum(abs(Close[i] - Close[i-1]))
```

分母は直近20期間のClose変化量の絶対値合計とする。分母が0の場合は、方向性が存在しない状態としてERを0とする。

#### Trend Strength

ADX14、Efficiency Ratio 20、EMA20 / EMA50 separationを使用する。

```text
adxScore = clamp((ADX - 15) / (40 - 15) * 100, 0, 100)
efficiencyRatioScore = Efficiency Ratio * 100
emaSeparationAtr = abs(EMA20 - EMA50) / ATR14
emaSeparationScore = clamp(emaSeparationAtr / 2.0 * 100, 0, 100)

trendStrength =
    0.50 * adxScore
  + 0.30 * efficiencyRatioScore
  + 0.20 * emaSeparationScore
```

ADXが15以下の場合のADX Scoreは0、40以上の場合は100とする。ATR14が0の場合のEMA Separation Scoreは0とする。結果は0〜100へClampする。

#### Volatility

VolatilityはATR%の絶対的なThresholdではなく、直近50本のFeature Lookback内におけるRelative Volatilityとして扱う。Lookback内で算出可能なATR%系列を作り、現在のATR%のPercentileを0〜100で返す。低いATR%は低Score、高いATR%は高Scoreとする。Tieはmid-rank相当の決定的な方法で扱う。ATR%=0だけの系列ではScoreを0とする。

#### Range Stay Ratio / Range Break Count

Evaluation Windowは直近20本とする。各Evaluation Candleより前の20本からReference Rangeを作る。

```text
Reference Upper = previous 20 candlesの最高High
Reference Lower = previous 20 candlesの最低Low
```

Evaluation CandleのCloseが `Reference Lower <= Close <= Reference Upper` の場合、そのCandleはRange内に滞在したと判定する。

```text
rangeStayRatio = Range内に滞在したEvaluation Candle数
                  / 有効なEvaluation Candle数 * 100
```

Evaluation Candle自身はReference Rangeへ含めない。

`High > Reference Upper` または `Low < Reference Lower` の場合、そのCandleをRange Breakとして1回Countする。同一Candleで上下両方をBreakしてもCountは1とする。

#### Reversal Count / Oscillation

直近20本をEvaluation Windowとし、各CandleのDirectionを次で判定する。

```text
delta = Close[i] - Close[i-1]
threshold = ATR14[i] * 0.25

delta >= threshold  -> +1
delta <= -threshold -> -1
それ以外           -> 0
```

0 Directionは無視し、直前の非0 Directionと現在の非0 Directionが逆になった場合にReversalを1回Countする。ATR14が0で価格変化もない場合はDirection 0として扱う。

```text
reversalScore = clamp(reversalCount / 6.0 * 100, 0, 100)
oscillation = 0.60 * reversalScore + 0.40 * rangeStayRatio
```

結果は0〜100へClampする。

#### Range Stability

20本のRolling Rangeを使用し、各時点の最高High、最低Lowから次を算出する。

```text
rangeWidth = upper - lower
rangeCenter = (upper + lower) / 2
```

Range WidthはClose等で正規化した値を使用する。直近の有効なRolling Range Width系列の変動係数を `CV = standard deviation / mean` とし、次でWidth Stability Scoreを算出する。

```text
widthStabilityScore = 100 * (1 - clamp(CV / 0.5, 0, 1))
```

評価期間の最初と最後のRange Center差を平均Range Widthで正規化し、次でCenter Stability Scoreを算出する。

```text
centerDriftRatio = abs(lastCenter - firstCenter) / averageRangeWidth
centerStabilityScore = 100 * (1 - clamp(centerDriftRatio / 0.5, 0, 1))
rangeStability = 0.60 * widthStabilityScore + 0.40 * centerStabilityScore
```

Range Widthの平均が0の場合、該当する安定性を100として扱う。結果は0〜100へClampする。

#### Breakout Risk

Range Break Count、現在価格のRange境界への近さ、Range Width Expansionを使用する。

```text
rangeBreakScore = clamp(rangeBreakCount / 4.0 * 100, 0, 100)
```

現在Candleより前の20本からReference Rangeを作る。現在CloseがRange外の場合のEdge Scoreは100とする。Range内かつRange Widthが0でない場合は次で算出し、0〜100へClampする。

```text
distanceToNearestEdge = min(Close - lower, upper - Close)
edgeScore = 100 * (1 - 2 * distanceToNearestEdge / rangeWidth)
```

現在の20本Range Widthを、それ以前のRolling Range Width平均と比較する。

```text
expansionRatio = currentRangeWidth / previousAverageRangeWidth
expansionScore = clamp((expansionRatio - 1.0) / 0.5 * 100, 0, 100)
```

Rangeが拡大していない場合はExpansion Scoreを0とする。過去平均Range Widthが0の場合、現在Range Widthも0なら0、現在Range Widthが正なら100として扱う。

```text
breakoutRisk =
    0.50 * rangeBreakScore
  + 0.30 * edgeScore
  + 0.20 * expansionScore
```

結果は0〜100へClampする。Feature計算ではGrid Suitability自体を判定しない。

#### Feature設定値

以下の値は一箇所で追跡・変更可能にし、ロジック内へMagic Numberとして散在させない。現段階では外部Config Server等は追加しない。

* Feature Lookback: 50
* Efficiency Ratio Period: 20
* Reference Range Period: 20
* Evaluation Period: 20
* Reversal ATR Multiplier: 0.25
* Reversal Target Count: 6
* Range Break Target Count: 4
* ADX Normalization Lower / Upper: 15 / 40
* EMA Separation Normalization: 2.0 ATR
* Range Width CV Limit: 0.5
* Range Center Drift Limit: 0.5
* Range Expansion Limit: 0.5
* Trend Strength Weight: ADX 0.50 / Efficiency Ratio 0.30 / EMA Separation 0.20
* Oscillation Weight: Reversal 0.60 / Range Stay 0.40
* Range Stability Weight: Width Stability 0.60 / Center Stability 0.40
* Breakout Risk Weight: Range Break 0.50 / Range Edge 0.30 / Range Expansion 0.20

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

### Market Regime判定の初期ルール

以下はMVPの初期ルールとする。最適値として固定せず、将来のBacktest結果をもとに調整可能な値として扱う。

Market Regimeは `RANGE`、`TREND`、`UNSTABLE` の3種類とする。Regime判定はStep 5で算出済みのFeatureを入力とし、Regime判定側でIndicatorを再計算しない。

判定は次の優先順位で行う。

1. EXTREME UNSTABLE
2. TREND
3. RANGE
4. FALLBACK UNSTABLE

#### EXTREME UNSTABLE

次の両方を満たす場合、他の条件より優先して `UNSTABLE` とする。

```text
Volatility >= 80
Breakout Risk >= 85
```

Volatilityだけ、またはBreakout Riskだけが高い場合は、この条件だけではUNSTABLEとしない。

#### TREND

EXTREME UNSTABLEに該当しない場合、次のすべてを満たす場合に `TREND` とする。

```text
Trend Strength >= 65
Efficiency Ratio >= 0.45
```

Range Stay RatioやRange Break CountはTRENDの必須条件としない。

#### RANGE

EXTREME UNSTABLEおよびTRENDに該当しない場合、次のすべてを満たす場合に `RANGE` とする。

```text
Range Stability >= 60
Range Stay Ratio >= 70
Oscillation >= 50
Trend Strength < 60
Breakout Risk < 60
```

#### FALLBACK UNSTABLE

EXTREME UNSTABLE、TREND、RANGEのいずれにも該当しない場合は `UNSTABLE` とする。曖昧な状態、遷移中の状態、Feature間で条件が矛盾する状態を無理にRANGEまたはTRENDへ分類しない。

判定値の境界では、EXTREME UNSTABLE、TREND、Range Stability・Range Stay Ratio・Oscillationは `>=` を使用する。RANGEのTrend StrengthとBreakout Riskは `< 60` を使用する。

設定値は `RegimeSettings` 等へ集約し、判定ロジック内へMagic Numberとして散在させない。外部Config Server等は追加しない。

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

### Grid Suitabilityの初期ルール

以下はMVPの初期ルールとする。最適なWeightやThresholdとして固定せず、将来のBacktest結果をもとに検証・調整する対象として扱う。

Grid Suitabilityは、現在の市場状態がGrid Tradingにどの程度適しているかを0〜100で表すRule-based Scoreとする。入力はStep 5で算出済みのFeatureとStep 6で判定済みのMarket Regimeとし、Indicatorの直接参照、Featureの再計算、Market Regimeの再判定は行わない。

#### Base Score

Market Regimeを考慮しないBase Scoreを、次の6 Componentから算出する。

```text
trendSafetyScore = clamp(100 - trendStrength, 0, 100)
breakoutSafetyScore = clamp(100 - breakoutRisk, 0, 100)

baseScore =
    0.25 * rangeStability
  + 0.20 * rangeStayRatio
  + 0.20 * oscillation
  + 0.15 * trendSafetyScore
  + 0.15 * breakoutSafetyScore
  + 0.05 * volatilityFitnessScore
```

Range Stability、Range Stay Ratio、OscillationはFeatureの値をそのまま使用する。

Volatility FitnessはStep 5のRelative Volatilityに対する台形型Scoreとする。

```text
Volatility <= 20       -> 0
20 < Volatility < 40   -> (Volatility - 20) / 20 * 100
40 <= Volatility <= 70 -> 100
70 < Volatility < 90   -> (90 - Volatility) / 20 * 100
Volatility >= 90       -> 0
```

Base Scoreは計算途中で丸めず、0〜100へClampする。

#### Market RegimeによるScore Cap

Market RegimeをWeightへ再度直接加算せず、最終Scoreの上限として使用する。

* `RANGE`: Cap 100
* `UNSTABLE`: Cap 59
* `TREND`: Cap 39

```text
finalScore = min(baseScore, regimeCap)
```

最終Scoreだけを `HALF_UP` で整数へ変換し、丸め後の整数でLevelを判定する。

```text
0 - 39   UNSUITABLE
40 - 59  LOW
60 - 79  MEDIUM
80 - 100 HIGH
```

#### Analysis Reasons

Analysis ReasonsはLLMを使用せず、決定的なRule-based `List<String>` として生成する。Market Regime Reasonを必ず最初に1件追加し、以下の順序を固定する。

1. Market Regime
2. Range Stability
3. Range Stay Ratio
4. Oscillation
5. Trend Strength
6. Breakout Risk
7. Volatility

条件と文言は次のとおりとする。

* RANGE: `Market regime is RANGE, which is favorable for grid trading.`
* TREND: `Market regime is TREND, which is unfavorable for grid trading.`
* UNSTABLE: `Market regime is UNSTABLE, so grid trading risk is elevated.`
* Range Stability >= 70: `Recent range structure is stable.`
* Range Stability < 50: `Recent range structure is unstable.`
* Range Stay Ratio >= 80: `Price has remained inside the recent range.`
* Range Stay Ratio < 60: `Price frequently leaves the recent range.`
* Oscillation >= 60: `Price oscillation is favorable for repeated grid fills.`
* Oscillation < 40: `Price oscillation is limited.`
* Trend Strength >= 65: `Trend strength is high and may create one-sided grid exposure.`
* Trend Strength < 40: `Trend strength is low, which is favorable for grid trading.`
* Breakout Risk >= 60: `Breakout risk is elevated.`
* Breakout Risk < 40: `Breakout risk is low.`
* Volatility <= 20: `Volatility is too low for efficient grid fills.`
* Volatility between 40 and 70 inclusive: `Volatility is in a favorable range for grid trading.`
* Volatility >= 90: `Volatility is too high for grid trading.`

上記の中間値ではReasonを追加しない。Market Regime Reasonが必ず存在するため、Reasonsは空Listにならない。

Score Weight、Volatility Fitness Threshold、Regime Cap、Reason Thresholdは `GridSuitabilitySettings` 等へ集約し、Magic Numberとしてロジック内へ散在させない。外部Config Server等は追加しない。

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

`window` はBackendではCandle本数として扱う。Frontendで表示するAnalysis Windowの期間から、Timeframeに応じたCandle本数へ変換して渡す。

レスポンス例：

```json
{
  "symbol": "BTC",
  "timeframe": "1h",
  "window": 168,
  "currentPrice": "112430",
  "dataAsOf": "2026-09-22T02:30:00Z",
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
  "indicators": {
    "adx": 16.4,
    "atrPct": 1.2,
    "efficiencyRatio": 0.18,
    "ema20": "112430",
    "ema50": "112180",
    "rangeStayRatio": 91,
    "reversalCount": 14,
    "rangeBreakCount": 2
  },
  "reasons": [
    "Directional trend is weak",
    "Price remains inside a stable range",
    "Reversal frequency is high"
  ]
}
```

`currentPrice` は、MVPでは分析に使用した最新の確定Candleの `close` とする。`dataAsOf` は分析対象となった最新の確定Candleの時刻とする。

APIで返す時刻はISO 8601 UTCを基本とする。価格・数量など精度が重要な値はJSON文字列として返し、Backend内部の `BigDecimal` の精度を不用意に失わないようにする。FrontendではAPI DTOを画面用Modelへ変換し、表示形式やLightweight Chartsが必要とする数値・Unix timestamp等への変換はAdapter側で行う。

`confidence` は現時点ではFrontend Mock専用項目とし、Backend API Contractには含めない。`name` はFrontend側の静的Metadataとして扱う。`chartVolatility`、`chartDrift` はMock Candle生成専用であり、APIへ追加しない。`rangeUpper`、`rangeLower` は現在のPrototype表示用であり、正式な計算仕様が確定するまでAnalysis APIの必須項目にしない。

### Candle Data

Price Chart用のCandle DataはAnalysis APIと分離して取得する。

```text
GET /api/v1/candles/{symbol}
```

Query：

```text
timeframe=1h
window=168
```

ResponseにはChart表示に必要な次の項目を含める。

```text
openTime
closeTime
open
high
low
close
volume
```

`tradeCount` はBackend内部には保持するが、Frontend Chartで使用しない場合は必須Response項目としない。Candle Dataの取得もAnalysis APIと同じTimeframe / Windowの変換ルールを使用する。

### Step 8 REST APIの確定事項

MVPでは次の2 Endpointだけを実装する。Overview専用APIなどは追加しない。

```text
GET /api/v1/analysis/{symbol}
GET /api/v1/candles/{symbol}
```

Query Parameter未指定時は、`timeframe=1h`、`window=168`を使用する。`window`はAnalysis Windowの期間ではなく、Backendが扱うCandle本数とする。

MVPでサポートするSymbolは`BTC`、`ETH`、`SOL`、`XRP`とする。Symbolは大文字小文字を区別せず大文字へ正規化し、その他のSymbolはUnsupported Symbolとして扱う。Hyperliquid上の全Symbolを動的に許可する機能は追加しない。

MVPでサポートするTimeframeは`15m`、`1h`、`4h`、`1d`とする。`window`は50以上5000以下のCandle本数とし、Analysis APIとCandle APIで同じValidationを使用する。

入力不正はHTTP 400で返す。API ErrorのResponseは次の2項目だけを持つ共通形式とする。

```json
{
  "code": "INVALID_TIMEFRAME",
  "message": "Unsupported timeframe: 5m"
}
```

`INVALID_SYMBOL`、`INVALID_TIMEFRAME`、`INVALID_WINDOW`を入力エラーのCodeとして使用する。timestamp、stack trace、debug情報、request ID、Validation Frameworkの内部構造はResponseへ含めない。

HTTP StatusとCodeは次のとおりとする。

* 400 Bad Request: `INVALID_SYMBOL`、`INVALID_TIMEFRAME`、`INVALID_WINDOW`
* 404 Not Found: `NO_MARKET_DATA`。有効な入力だが、HyperliquidおよびDBからCandleを1件も取得できない場合。
* 422 Unprocessable Entity: `INSUFFICIENT_CANDLES`。Market Dataは存在するが、要求されたwindowまたは分析に必要なCandle数を確保できない場合。架空データによる補完は行わない。
* 502 Bad Gateway: `MARKET_DATA_UNAVAILABLE`。Hyperliquid Public APIの通信失敗や外部API異常でMarket Dataを取得できない場合。ただし、DBに要求数を満たす十分な最新Candleが存在し、外部APIが不要な場合はDBのデータを使用する。

分析・Candle取得では、まず確定済みCandleをDBから利用できるか確認する。要求数を満たさない場合だけHyperliquid Public APIから取得し、確定済みCandleを内部モデルへ変換してDBへ保存する。分析には要求されたwindowの最新Candleを使用し、`currentPrice`は最新Candleの`close`、`dataAsOf`は最新Candleの時刻とする。

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

現在の実装順序、進捗、各Stepの完了条件は `docs/TASKS.md` を正とする。

本仕様書では、Backendのアーキテクチャ、責務、データモデル、分析ロジック、API、Backtestなどの技術仕様を定義する。Stepの詳細な進捗管理やDone条件は重複して管理しない。

Frontend UI PrototypeはBackendより先に実装済みである。Backend API完成後に、既存FrontendのMock DataをAPIへ置換する。既存UIをStep順に合わせるためだけに作り直さない。

---

## 18. Dashboard

MVPのDashboardでは最低限以下を表示する。

* Symbol
* Current Price
* Timeframe
* Analysis Window
* Last Updated（`dataAsOf`）
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

* Analyzerは市場分析のみを担当し、注文・Wallet・資産操作を行わない
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
```

最終的には、

**現在の市場環境を分析し、その環境に適したTrading Strategyを選択するシステム**

へ発展させる。

Market Regime Analyzerは、その中核となる市場分析システムとして独立して利用可能な構造を維持する。
