# Market Regime Analyzer 画面設計

## 1. 目的

Market Regime Analyzerの分析結果を、ユーザーが直感的に確認できるWeb UIを提供する。

UIでは主に以下を確認できるようにする。

- 現在の各コインの市場状態
- Market Regime
- Grid Tradingへの適性
- 判定の根拠
- 実際の価格推移
- AnalyzerのBacktest結果

Market Regime Analyzerは市場分析システムであり、MVPでは売買操作UIを持たない。

---

## 2. 画面構成

MVPでは以下の3画面を想定する。

1. Market Overview
2. Coin Analysis
3. Backtest

基本的な利用イメージ：

```text
Market Overview
      ↓
   Coin選択
      ↓
Coin Analysis

Backtest
→ Analyzer自体の有効性を検証
````

Market OverviewとCoin Analysisは、現在の市場を確認するための画面とする。

BacktestはCoin Analysisの単純な下位画面ではなく、
Analyzerの判定ロジックそのものを検証する独立機能として扱う。

---

# 3. Market Overview

## 3.1 目的

複数の暗号資産について現在の市場状態を一覧表示し、

**「どのコインを詳しく確認するか」**

を判断できるようにする。

アプリ起動時のメイン画面とする。

---

## 3.2 表示形式

複数銘柄を比較しやすいため、基本的にテーブル形式を使用する。

MVPでは表示項目を増やしすぎず、主要な分析結果のみ表示する。

基本項目：

```text
Coin
Current Price
Market Regime
Grid Suitability
```

表示例：

```text
Coin    Price       Regime      Grid Suitability

BTC     $112,430    RANGE       86 HIGH
ETH       $4,320    RANGE       74 MEDIUM
SOL         $238    TREND       31 LOW
XRP        $3.12    UNSTABLE    22 LOW
```

Trend StrengthやBreakout Riskなどの詳細Featureは、
原則としてCoin Analysis画面で確認する。

Overviewの列は将来の機能拡張に応じて変更・追加可能とする。

---

## 3.3 操作

以下を変更可能とする。

```text
Timeframe
Analysis Window
```

例：

```text
Timeframe
15m / 1h / 4h / 1d

Analysis Window
24H / 3D / 7D / 14D / 30D
```

コインの行をクリックするとCoin Analysisへ遷移する。

---

## 3.4 将来拡張

Strategy Selector実装後は、以下のような表示へ拡張可能とする。

```text
Coin   Regime      Recommended Strategy   Suitability

BTC    RANGE       Grid                   86
ETH    TREND       Momentum               81
SOL    BREAKOUT    Breakout               78
XRP    UNSTABLE    No Trade               -
```

ただしMVPではGrid Suitabilityのみを扱う。

---

# 4. Coin Analysis

## 4.1 目的

選択した1銘柄について詳細な分析結果を表示する。

主に、

**「なぜこのMarket Regime / Grid Suitabilityになったのか」**

を理解するための画面とする。

---

## 4.2 情報の優先順位

以下の順番で情報を配置する。

```text
1. Market Regime
2. Grid Suitability
3. Market Features
4. Price Chart
5. Analysis Reasons
6. Indicator Details
```

情報構造としては、

```text
結論
 ↓
理由
 ↓
生データ
```

となるようにする。

---

## 4.3 Header

以下を表示する。

```text
Symbol
Current Price
Timeframe
Analysis Window
Last Updated
```

例：

```text
BTC
$112,430

1h
7D / 168 candles
Updated just now
```

`Last Updated` は、Backend APIの `dataAsOf`（分析対象となった最新の確定Candleの時刻）を表示する。

---

## 4.4 Market Regime

現在のMarket Regimeを大きく表示する。

MVP対象：

```text
RANGE
TREND
UNSTABLE
```

例：

```text
Market Regime

RANGE

Confidence
82%
```

Market Regimeは画面を開いた際にすぐ認識できる表示とする。

---

## 4.5 Grid Suitability

Grid Tradingへの適性を0〜100で表示する。

例：

```text
Grid Suitability

86 / 100

HIGH
```

Level：

```text
80 - 100   HIGH
60 - 79    MEDIUM
40 - 59    LOW
0 - 39     UNSUITABLE
```

Market Regimeと並んで、この画面で最も重要な情報とする。

---

## 4.6 Market Features

Market RegimeおよびGrid Suitabilityの判断材料となるFeatureを表示する。

MVP対象：

```text
Trend Strength
Volatility
Range Stability
Oscillation
Breakout Risk
```

例：

```text
Trend Strength      18 / 100
Volatility          64 / 100
Range Stability     84 / 100
Oscillation         91 / 100
Breakout Risk       21 / 100
```

Progress Barなどを利用し、それぞれの強弱を比較しやすくする。

---

## 4.7 Price Chart

ローソク足チャートを表示する。

チャートの目的はTrading操作ではなく、

**Analyzerの判定と実際の価格推移を人間が視覚的に確認すること**

とする。

MVPで表示するもの：

```text
Candlestick
Volume
```

必要に応じて以下をモック表示する。

```text
Range Upper
Range Lower
```

チャートは重要な情報ではあるが、
Market RegimeやGrid Suitabilityより優先しない。

以下はMVPでは表示しない。

```text
BUY / SELL Marker
Order
Position
PnL
Trade History
大量のGrid Line
```

---

## 4.8 Analysis Reasons

判定理由を表示する。

例：

```text
Why this score?

✓ Directional trend is weak
✓ Price remains inside a stable range
✓ Reversal frequency is high
✓ Volatility is sufficient
✓ Breakout risk is currently low
```

ユーザーが、

**「なぜGrid Suitabilityが86なのか」**

をFeatureとReasonの両方から理解できるようにする。

---

## 4.9 Indicator Details

Analyzerが使用したIndicatorの生値を確認できるようにする。

例：

```text
ADX                 16.4
ATR %               1.2%
Efficiency Ratio    0.18
EMA20               $112,430
EMA50               $112,180
Range Stay Ratio    91%
Reversal Count      14
Range Break Count   2
```

Feature Scoreとは明確に区別する。

```text
Feature
→ 市場状態を分かりやすく数値化したもの

Indicator
→ Feature計算等に使用する元データ
```

Indicator Detailsは詳細情報のため、表示優先度を低くしてよい。

---

# 5. Backtest

## 5.1 目的

Market Regime Analyzerの判定が実際に有効だったか検証する。

主な問い：

**「Grid Suitabilityを利用した場合、Gridを常時稼働するより結果が改善するか？」**

---

## 5.2 比較対象

```text
Always Grid

vs

Analyzer + Grid
```

例：

```text
Grid Suitability >= 80
→ Grid

Grid Suitability < 80
→ No Trade
```

---

## 5.3 表示項目

主要指標：

```text
PnL
Max Drawdown
Trade Count
Fees
Profit Factor
```

必要に応じて将来以下も追加する。

```text
Win Rate
Sharpe Ratio
```

比較例：

```text
                 Always Grid     Analyzer + Grid

Return               +8.2%            +11.4%
Max Drawdown        -18.3%             -9.1%
Trades                 842               391
Fees                   124                61
Profit Factor         1.08              1.31
```

---

## 5.4 Performance Chart

Always GridとAnalyzer + Gridのパフォーマンス推移を比較できるチャートを表示する。

目的は価格チャートを見ることではなく、

**Analyzerを利用したことによる成績差を確認すること**

とする。

---

## 5.5 Analysis History

必要に応じて各分析時点の判定履歴を表示する。

例：

```text
Date       Regime    Grid Score    Action       Result

09/01      RANGE        87         GRID         +1.2%
09/02      TREND        31         NO TRADE       -
09/03      RANGE        81         GRID         +0.7%
```

AnalyzerがいつGridを選択し、
その後どのような結果になったか確認できるようにする。

---

# 6. Navigation

MVPではNavigationをシンプルに保つ。

例：

```text
Market Regime Analyzer

Overview
Backtest
```

Market OverviewからCoinを選択するとCoin Analysisへ遷移する。

Coin AnalysisはNavigation上の独立したトップレベル項目にしなくてもよい。

---

# 7. UIデザイン方針

Desktop-firstとする。

Dark Themeを基本とする。

以下を重視する。

```text
Professional
Technical
Data-driven
Dense but readable
Clear information hierarchy
Minimal unnecessary decoration
```

TradingViewやHyperliquidのような、
落ち着いたTrading / Quant Analysis Toolの雰囲気を参考とする。

以下は避ける。

```text
Crypto Casino風
過度なGradient
Neon中心の配色
Landing Page風
AI Chat UI
過度なAnimation
大きすぎるCard
```

---

# 8. Responsive Design

PCでの市場分析を最優先する。

Tablet / Mobileでもレイアウトが破綻しないようにする。

ただしMobile対応のためにDesktop版の情報量を過度に削減しない。

---

# 9. MVPで作成しないUI

以下は実装しない。

```text
BUY / SELL Button
Order Form
Wallet Connect
Account Balance
Position
Trade History
Trading PnL
Portfolio
Leverage
Stop Loss
Take Profit
Strategy実行Button
```

Market Regime AnalyzerはMVPでは市場分析システムであり、
売買操作を行わない。

---

# 10. v0 UI Prototypeの対象

最初のUI Prototypeでは以下を優先する。

```text
1. Market Overview
2. Coin Analysis
```

この2画面によって、

```text
Overview
→ どのコインを見るか

Coin Analysis
→ なぜその判定なのか
```

という基本UXを確認する。

Backtest画面はMVPには含まれるが、
UI Prototypeの初回では後回しとしてよい。

現在のv0 UI PrototypeではMarket OverviewとCoin Analysisを実装済みであり、Mock Dataを表示している。Backend API接続後に、既存UIを可能な限り維持したままデータソースを置き換える。

---

# 11. 将来構想

将来的にStrategy Selectorを追加した場合も、
現在の画面構造を基本的に維持する。

```text
Market Overview
       ↓
Coin Analysis
       ↓
Strategy Suitability
```

Overviewでは、

```text
現在どの銘柄が
どのStrategyに適しているか
```

を比較可能にする。

Coin Analysisでは、

```text
なぜそのStrategyが適しているのか
```

を詳細に確認可能にする。

Backtestでは、

```text
その判定ロジックが過去データでも有効だったか
```

を検証する。

最終的にも、

```text
Overview
→ 結論を比較する

Coin Analysis
→ 理由を理解する

Backtest
→ 判断の有効性を検証する
```

という役割分担を維持する。
