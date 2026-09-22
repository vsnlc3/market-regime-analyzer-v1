# Market Regime Analyzer 要件定義書

## 1. 目的

暗号資産の市場データを分析し、現在の相場環境を判定する。

初期バージョンでは特に、

**「現在の相場がGrid Tradingに適しているか」**

を判定し、ユーザーに分析結果を提供する。

将来的には複数の売買戦略から、その相場に適した戦略を選択する **Strategy Selector** へ拡張する。

---

## 2. システムの役割

Market Regime Analyzerは**市場分析のみを担当し、実際の売買は行わない。**

```text
Market Data
    ↓
Market Regime Analyzer
    ↓
分析結果
```

売買注文、Wallet接続、実資金取引、ポジション管理、資産管理などはMVPで扱わない。

---

## 3. MVPの対象

v0.1ではGrid Trading適性判定に限定する。

判定するMarket Regime：

* RANGE
* TREND
* UNSTABLE

出力する主要情報：

* Market Regime
* Grid Suitability Score（0〜100）
* 市場の特徴量
* 判定理由

---

## 4. 使用データ

分析にはOHLCVを使用する。

```text
Open
High
Low
Close
Volume
```

標準設定：

```text
Timeframe : 1h
Window    : 7D（168本）
```

TimeframeとWindowは変更可能とする。

FrontendではAnalysis Windowを期間として表示し、Backend APIではTimeframeに応じたCandle本数として扱う。FrontendのAPI境界で期間とTimeframeから必要Candle数へ変換する。

---

## 5. データソース

MVPでは **Hyperliquid Public API** を利用する。

用途：

```text
candleSnapshot
→ 直近の過去OHLCV取得

WebSocket Candle
→ リアルタイムデータ取得
```

取得したOHLCVは自前DBへ保存し、将来のバックテストにも利用する。

AnalyzerはHyperliquidへ直接依存しすぎないよう、Market Data Providerを分離する。

```text
Analyzer
    ↓
MarketDataProvider
    ↓
Hyperliquid
```

将来的に他Exchangeや長期Historical Data Providerを追加可能とする。

---

## 6. 分析項目

OHLCVから以下のような指標・特徴量を算出する。

### Trend

* ADX
* 移動平均の傾き
* Efficiency Ratio

### Volatility

* ATR %
* Bollinger Band Width

### Range

* Range Stability
* Range Stay Ratio
* Reversal Count
* Range Break Count
* Oscillation Score

### Breakout Risk

* Breakout Risk

これらを組み合わせてMarket Regimeを判定する。

---

## 7. Grid Suitability Score

現在の市場がGrid Tradingに適している度合いを、

```text
0 ～ 100
```

で表す。

基本的には、

```text
弱いトレンド
+
適度なボラティリティ
+
安定したレンジ
+
高い往復頻度
+
低いブレイクアウトリスク
```

ほどScoreを高くする。

例：

```text
Market Regime
RANGE

Grid Suitability
86 / 100

Trend Strength     18
Volatility         64
Range Stability    84
Oscillation        91
```

---

## 8. 判定理由

Scoreだけでなく、なぜその判定になったか確認できるようにする。

例：

```text
Grid Suitability: 86

・Directional trend is weak
・Price remains inside a stable range
・Reversal frequency is high
・Volatility is sufficient
```

MVPではルールベースの説明文とし、LLMに直接売買判断はさせない。

---

## 9. API

外部システムから分析結果を取得可能とする。

例：

```text
GET /api/v1/analysis/BTC
```

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

ローソク足チャート用のCandle Dataは、分析結果とは分離して取得する。具体的なEndpointとResponse項目は実装仕様書を参照する。

---

## 10. Dashboard

Web画面では最低限以下を表示する。

* Symbol
* Current Price
* Timeframe
* Analysis Window
* Last Updated（dataAsOf）
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

---

## 11. Backtest

過去データを使い、

```text
常にGridを動かす

vs

Grid Suitabilityが高い場合のみGridを動かす
```

を比較する。

評価指標：

* PnL
* Max Drawdown
* Trade Count
* Fees
* Profit Factor

目的は、

**Market Regime Analyzerを利用することで、Gridを常時稼働するより成績を改善できるか検証すること。**

---

## 12. MVP対象外

v0.1では以下を実装しない。

* 実資金取引
* 注文発行
* Futures / Leverage
* Machine Learning
* LLMによる売買判断
* 自動Strategy切替
* Funding Arbitrage
* DCA
* Rebalancing

---

## 13. MVP完了条件

以下を満たした時点でv0.1完成とする。

1. HyperliquidからOHLCVを取得できる
2. OHLCVをDBへ保存できる
3. 市場特徴量を算出できる
4. RANGE / TREND / UNSTABLEを判定できる
5. Grid Suitability Scoreを算出できる
6. 判定理由を確認できる
7. APIから分析結果を取得できる
8. Dashboardで結果を確認できる
9. 過去データでBacktestできる
10. Always Gridとの比較ができる

---

## 14. 将来構想

Market Regime Analyzerを以下へ拡張する。

```text
Market Data
    ↓
Market Regime Analyzer
    ↓
Strategy Selector
    ↓
Grid
Mean Reversion
Trend Following
Momentum
Breakout
No Trade
```

最終的には、

**「現在の市場環境に応じて、適した売買戦略を選択するシステム」**

を目指す。
