# Market Regime Analyzer Tasks

このファイルは、実装仕様を定義する文書ではなく、現在の完了状況、次に実装するStep、各Stepの完了条件を管理するための進捗表である。

- `requirements.md`: 何を作るか
- `implementation-spec.md`: どう作るか
- `screen-design.md`: どう見せるか
- `development-guidelines.md`: どういうルールで開発するか
- `TASKS.md`: 今どこまで終わっていて、次に何をするか

各Stepの技術的な詳細は `docs/implementation-spec.md` を参照する。

## 現在の状況

### 完了済み

- [x] Requirements
- [x] Implementation Spec
- [x] Screen Design
- [x] Development Guidelines
- [x] Frontend UI Prototype
  - [x] Market Overview
  - [x] Coin Analysis
  - [x] BTC / ETH / SOL / XRP Mock Data
  - [x] Timeframe / Analysis Window controls
  - [x] Market Regime表示
  - [x] Grid Suitability表示
  - [x] Market Features
  - [x] Candlestick / Volume Chart
  - [x] Analysis Reasons
  - [x] Indicator Details
  - [x] Range Upper / Range LowerのPrototype表示

Frontend UI Prototypeは実装済みとして扱う。Step順に合わせるために作り直さない。現在はBackend APIへ接続しておらず、Mock Dataを表示している。

### 未完了

- [ ] Backend実装
- [ ] Backend API接続
- [ ] Backtestロジック
- [ ] Backtest UI

## Analysis Windowの共通ルール

Analysis Window、Timeframe、Candle本数の変換仕様は `docs/implementation-spec.md` を参照する。

Analysis Windowの正式仕様は `docs/implementation-spec.md` を正とし、TASKS.mdへ重複して持たせない。

## 今後の実装Step

### Step 1 - Backend Bootstrap

対象：Java 21、Spring Boot、Backend project、`GET /health`、Unit Test

Done when：

- [ ] Spring Boot Applicationが起動する
- [ ] `/health` が正常レスポンスを返す
- [ ] Testが成功する

### Step 2 - Market Data Provider

対象：MarketDataProvider、Hyperliquid Public API、`candleSnapshot`、BTC、1h、168 candles、Candle内部モデル変換、正常系・異常系Test

Done when：

- [ ] HyperliquidからCandleを取得できる
- [ ] 外部API形式を内部Candleへ変換できる
- [ ] Analyzer側がHyperliquid固有形式へ依存しない
- [ ] 正常系・異常系Testが成功する

### Step 3 - Candle Persistence

対象：PostgreSQL、Spring Data JPA、Flyway、Candle Entity、Candle Repository、Unique Constraint、Index、重複登録防止、Testcontainers

Done when：

- [ ] Migrationを適用できる
- [ ] Candleを保存・取得できる
- [ ] 同一Candleの重複登録を防止できる
- [ ] Repository / DB連携Testが成功する

### Step 4 - Indicator

対象：ADX、ATR、ATR %、EMA、Bollinger Band

Done when：

- [ ] 対象Indicatorを計算できる
- [ ] 既知データと境界値のTestが成功する

### Step 5 - Feature

対象：Trend Strength、Volatility、Efficiency Ratio、Range Stability、Range Stay Ratio、Reversal Count、Oscillation、Range Break Count、Breakout Risk

Done when：

- [ ] 対象Featureを計算できる
- [ ] 0 / 100、閾値付近、データ不足のTestが成功する

### Step 6 - Market Regime

対象：`RANGE`、`TREND`、`UNSTABLE`、Rule-based判定、Boundary Test

Done when：

- [ ] 3種類のMarket Regimeを判定できる
- [ ] 判定閾値のBoundary Testが成功する

### Step 7 - Grid Suitability

対象：Score 0〜100、HIGH / MEDIUM / LOW / UNSUITABLE、Analysis Reasons、Boundary Test

Done when：

- [ ] ScoreとLevelを算出できる
- [ ] 判定理由を返せる
- [ ] ScoreとLevelの境界Testが成功する

### Step 8 - REST API

#### Analysis API

`GET /api/v1/analysis/{symbol}`

対象：`timeframe`、Candle本数としての`window`、Validation、Response DTO、`currentPrice`、`dataAsOf`、Indicator、Feature、Market Regime、Grid Suitability、Analysis Reasons

#### Candle API

`GET /api/v1/candles/{symbol}`

対象：`timeframe`、Candle本数としての`window`、Validation、Candle Response DTO、`openTime`、`closeTime`、`open`、`high`、`low`、`close`、`volume`

Done when：

- [ ] Analysis APIが定義済みResponse DTOを返す
- [ ] Candle APIがChart用Candle Dataを返す
- [ ] `currentPrice` と `dataAsOf` を返す
- [ ] Analysis API / Candle APIの入力ValidationとAPI Testが成功する
- [ ] EntityやHyperliquid API Responseをそのまま外部公開していない
- [ ] `confidence`、`name`、`chartVolatility`、`chartDrift`、`rangeUpper`、`rangeLower`を必須Responseへ追加していない

### Step 9 - Frontend Integration

現在のMock DataをBackend APIへ置き換える。Frontend UIのデザインを作り直すStepではない。

対象：Market Overview、Coin Analysis、Analysis API接続、Candle Data接続、DTOからFrontend View Modelへの変換、dataAsOf / Last Updated表示、Loading、API Error、Empty Data、Invalid Symbol、Insufficient Candle Data

Done when：

- [ ] Market OverviewがAnalysis APIを表示する
- [ ] Coin AnalysisがAnalysis APIとCandle APIを表示する
- [ ] DTOとFrontend View Modelの変換がComponent外に分離されている
- [ ] `dataAsOf` をLast Updatedとして表示する
- [ ] Loading / Error / Empty / Invalid Symbol / Insufficient Candle Dataを扱える
- [ ] 既存のFrontend UIデザインを可能な限り維持している

### Step 10 - Backtest

対象：Always Grid、Analyzer + Grid、PnL、Max Drawdown、Trade Count、Fees、Profit Factor、Look-Ahead Bias防止

Done when：

- [ ] Always GridとAnalyzer + Gridを比較できる
- [ ] 指定された評価指標を算出できる
- [ ] 判定時点より未来のデータを利用しない
- [ ] 本番分析と同じAnalyzerロジックを利用する

### Step 11 - Backtest UI

Backtest結果をFrontendから確認できる画面を実装する。画面の表示内容は `docs/screen-design.md` を参照する。

Done when：

- [ ] Backtest結果を画面で確認できる
- [ ] Always GridとAnalyzer + Gridを比較できる
- [ ] Performance Chartまたは定義済みの比較結果を表示できる

## Next Step

次に着手するStepは **Step 1 - Backend Bootstrap** とする。
