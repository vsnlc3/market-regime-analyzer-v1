# Market Regime Analyzer 開発ガイドライン

## 1. このガイドラインの位置づけ

この文書は、Market Regime Analyzerでコードを書く・レビューする・AIへ実装を依頼する際に守る共通方針をまとめたものである。要件定義書や実装仕様書の代わりに、個別の機能仕様を定義するものではない。

仕様を確認するときは、関連するドキュメントと現在の実装を確認する。仕様判断では `docs/requirements.md`、`docs/implementation-spec.md`、`docs/screen-design.md` を優先し、現在の実装は実装変更時の確認対象として扱う。既存実装を不用意に壊さず、実装とドキュメントに差異がある場合は、推測で大規模に修正せず、差異を明示して判断を分ける。

仕様判断の優先順位は、`docs/requirements.md`、`docs/implementation-spec.md`、`docs/screen-design.md`、現在のリポジトリの実装の順とする。

### MVPの境界

Market Regime Analyzerは市場データを分析し、Market RegimeとGrid Trading適性を返すシステムである。MVPでは次を扱う。

- Market Regime: `RANGE` / `TREND` / `UNSTABLE`
- Grid Suitability Score: 0〜100
- 市場Feature、Indicator、判定理由
- 現在分析と過去データを用いたBacktest

注文発行、Wallet接続、実資金取引、ポジション管理、資産管理などはMVPの対象外である。

## 2. 基本方針

- MVPのスコープを独断で拡張しない。仕様外の機能は、必要性を感じても先回りして追加しない。
- 必要以上の抽象化、汎用化、将来機能のための過剰設計を避ける。
- 1回の変更では目的に直接関係する範囲だけを変更し、無関係なリファクタリングを同時に行わない。
- 変更範囲を小さく保ち、既存の設計・命名・ディレクトリ構成を優先する。
- 分析結果の正しさ、再現性、説明可能性をUI上の見た目より優先する。
- 閾値やWeightなど分析結果に影響する値は、ロジックへ散在させず、変更箇所が追える形で管理する。
- 現在のドキュメントと実装を継続的に参照し、要件・仕様・実装の重複を新しいコードや文書に増やさない。

## 3. 技術スタックと現状

### Backend

Backendの採用技術は `docs/implementation-spec.md` に従う。

- Java 21
- Spring Boot
- Spring Web
- Spring `RestClient`
- Spring Data JPA
- PostgreSQL
- Flyway
- ta4j
- JUnit 5
- Testcontainers

Hyperliquid REST APIとの通信には `RestClient` を使用し、利用可能なテクニカル指標には ta4jを利用する。Market Regime判定に必要な独自Featureはアプリケーション側で実装する。

### Frontend

現在の `frontend/package.json` と実装で確認できる構成は次のとおりである。

- Next.js（App Router）
- React
- TypeScript
- Tailwind CSS
- `frontend/components/ui/` に置くshadcn/ui方針のUIコンポーネント
- Radix UIのSelectなどのプリミティブ
- Lightweight Charts
- `lucide-react`、`clsx`、`tailwind-merge` などの既存ユーティリティ

package.jsonに記載されたバージョンを変更する場合は、既存の依存関係とlockfileを確認する。現在の構成にない状態管理・フォーム・データ取得ライブラリは、必要性と仕様を確認せずに追加しない。

### 現在のFrontend実装

現時点のFrontendはOverviewとCoin Analysisを中心としたUIプロトタイプであり、`frontend/lib/mock-data.ts` のモックデータを表示している。`frontend/app/` がページ、`frontend/components/` が表示・操作部品、`frontend/lib/` が表示用データやユーティリティを担当する。

- `tf` と `win` のクエリパラメータでTimeframeとAnalysis Windowを扱う。
- `frontend/components/coin/price-chart.tsx` がLightweight Chartsでローソク足とVolumeを表示する。
- モックデータは決定的な生成処理を含むため、画面確認やテスト用の再現性を損なわない。
- Backend API接続へ置き換える際も、UI部品がデータ取得元へ直接依存しない構成を維持する。
- `screen-design.md` に記載されたBacktest画面やBackend API連携は、現在のFrontend実装済み機能とは区別する。

BackendとDocker Composeは実装仕様上の採用対象であるが、実際のファイルが存在しない段階では、存在するものとしてコードや手順を記述しない。

## 4. Backendの責務分離

処理の基本的な流れは次のとおりである。

```text
Hyperliquid
  → MarketDataProvider
  → Candle Repository
  → Indicator
  → Feature
  → Market Regime
  → Grid Suitability
  → REST API
```

### Controller

- HTTPリクエストを受け、入力を検証し、DTOへ変換してServiceを呼び出す。
- HTTPレスポンス、ステータス、エラー変換を担当する。
- Indicator計算、Feature計算、Regime判定などの分析ロジックを書かない。

### Service

- ユースケースの流れを組み立てる。
- 必要なCandleの取得、計算サービスの呼び出し、分析結果の組み立てを担う。
- Controllerや外部APIの具体的な形式に分析ロジックを依存させない。

### Repository

- Candleなど永続化対象の保存・取得を担当する。
- 外部API呼び出しやMarket Regime判定を担当しない。

### MarketDataProvider

- 外部Market Data Sourceとの通信、レスポンスの解釈、内部のCandleモデルへの変換を担当する。
- MVPの実装はHyperliquid Public APIを対象とする。
- Hyperliquid固有のURL、リクエスト形式、レスポンス形式を分析ロジックへ直接持ち込まない。
- AnalyzerからHyperliquid APIを直接呼び出さない。

### Indicator

- OHLCVからADX、ATR、ATR%、EMA、Bollinger Bandなどの数値を計算する。
- 数値計算に集中し、Market RegimeやGrid Suitabilityの最終判定を行わない。

### Feature

- IndicatorとOHLCVから、Trend Strength、Volatility、Efficiency Ratio、Range Stability、Range Stay Ratio、Reversal Count、Oscillation、Range Break Count、Breakout Riskなどを算出する。
- 必要なFeatureは0〜100へ正規化してもよいが、Indicatorの生値とは区別する。
- Feature計算の中でMarket Regime判定を行わない。

### Market Regime

- Featureを入力として、ルールベースで `RANGE`、`TREND`、`UNSTABLE` を判定する。
- Regimeの判定条件や閾値は一箇所で追跡・変更できるようにする。

### Grid Suitability

- Trend Strength、Volatility、Range Stability、Oscillation、Breakout Riskなどから、Grid Trading適性を0〜100で算出する。
- Market Regimeの判定とGrid Suitabilityの計算を混在させない。
- Scoreだけでなく、ルールに基づく判定理由を扱えるようにする。

本番のLive分析とBacktestでは、可能な限り同じIndicator、Feature、Market Regime、Grid Suitabilityのロジックを使用する。Backtest専用の判定ロジックを二重実装しない。

## 5. Market Dataと時系列データ

- MVPではHyperliquid Public APIのMarket Dataだけを利用する。初期の取得はREST APIの `candleSnapshot` を対象とし、WebSocketは仕様に定義された将来候補として扱う。
- APIレスポンスをそのまま分析へ渡さず、内部のCandleモデルへ変換する。
- Candleには少なくともexchange、symbol、timeframe、openTime、closeTime、open、high、low、close、volume、tradeCountを扱える設計とする。
- 取得した確定足はDBへ保存し、分析・Grid Suitability算出・Historical Data蓄積・Backtestに利用する。
- 時刻はUTCを基準に扱う。ローカル時刻への変換は表示上の都合に限定する。
- 価格、数量、その他精度が重要な値にはBackendで `BigDecimal` を使用する。
- 確定足は原則として変更しない。未確定足を保存する場合だけ、同じCandleを更新できる扱いを明確にする。
- 同一Candleを再取得しても重複登録しない。
- 分析時点より未来のCandle、Indicator、Featureを分析やBacktest判定へ混入させない。判定に使えるデータは、その時点までに確定しているデータに限定する。

Hyperliquid Private API、API Key、Wallet、注文処理、ポジションや資産残高の扱いは追加しない。

## 6. Database

- DBはPostgreSQL、永続化はSpring Data JPA、Schema変更はFlyway Migrationで管理する。
- Schemaを手動変更してコードとDBの状態を分岐させない。Migrationは既存履歴を壊さず、順序と適用結果を確認する。
- Candleの一意性は `exchange`、`symbol`、`timeframe`、`openTime` の組み合わせで保証する。
- 重複保存防止のUnique Constraintと、取得条件に必要なIndexをDB側へ設ける。
- 保存・更新・重複処理など一連の書き込みには、必要な範囲でTransactionを利用する。
- 確定足の再取得によって意図せず既存データを書き換えない。
- 接続先、ユーザー名、パスワードなど環境依存の接続情報をソースコードへハードコードしない。
- 現在の仕様にないSoft Delete、複雑なRelation、不要な汎用Repositoryを先に導入しない。

## 7. Frontend開発方針

### 画面と情報設計

画面の役割は `docs/screen-design.md` に従う。

- Market Overviewは複数銘柄の比較と詳細画面への導線を担う。
- Coin Analysisは、結論、理由、元データの順で理解できるようにする。
- Coin Analysisの優先順位は、Market Regime、Grid Suitability、Market Features、Price Chart、Analysis Reasons、Indicator Detailsの順とする。
- Desktop-first、Dark Theme、技術的で落ち着いた情報密度のある画面を維持する。
- 既存のレイアウト、配色、カード、テーブル、チャートの意図を確認せずに全面変更しない。
- Trading操作を連想させるUIを追加しない。Buy/Sell、Order、Wallet、Position、PnLなどはMVP対象外である。

### 実装

- UIコンポーネントは表示と操作に集中させ、データ取得・変換処理を分離する。
- 現在のMock Dataは将来Backend APIのResponseへ置換できる境界を保つ。UI内にモック固有の生成ロジックを増やさない。
- Query Parameter、Timeframe、Analysis Windowの許可値と既定値は、既存の `frontend/lib/mock-data.ts` と画面の挙動に合わせる。
- Server ComponentとClient Componentの境界は、既存のNext.js実装に合わせる。ブラウザ操作やチャートなどクライアントで必要な処理だけをClient Componentにする。
- 数値や価格の表示形式は既存の表示ユーティリティを優先し、同じ値を画面ごとに別形式で表示しない。
- 画面を変更する場合は、`screen-design.md` の情報の優先順位と、分析結果を理解するための目的を維持する。

## 8. API

### 現時点で定義されている責務

REST APIは分析結果を外部から取得するための入口である。定義済みの主なEndpointは次のとおりである。

```text
GET /api/v1/analysis/{symbol}
GET /health
```

Analysis APIでは、`timeframe` と `window` を指定でき、symbol、timeframe、window、regime、gridSuitability、gridSuitabilityLevel、features、reasonsなど、仕様で定義された分析結果を返す。レスポンスの構造化データを正とし、画面表示用の文言や装飾をBackendの分析ロジックへ混ぜない。

### 実装ルール

- RequestとResponseはDTOとして扱い、Entityや外部APIのレスポンスをそのまま公開しない。
- symbol、timeframe、windowなどの入力はAPI境界で検証し、不正な値を分析層へ渡さない。
- ControllerはValidation結果とServiceの結果をHTTPレスポンスへ変換する。
- 成功・入力不備・外部データ取得失敗・内部エラーを区別できるようにする。
- HTTPステータスの詳細な対応表、エラーJSONの項目、エラーコードは現仕様で固定されていない。実装時に独自の契約を広げず、必要になったらAPI仕様として先に明文化する。
- Backend API未接続のFrontendでは、現在のモック表示をAPI仕様として扱わない。API接続時にDTOと画面用モデルの変換箇所を設ける。

## 9. Testing

BackendのテストはJUnit 5を基本とし、DB連携にはTestcontainersを利用する。テスト可能な変更では、実装だけでなく対応するテストも追加・更新する。

- Indicator計算: ADX、ATR、ATR%、EMA、Bollinger Bandの既知データに対する計算結果を検証する。
- Feature計算: 0、100、閾値付近、データ不足、一定値、急変などの境界を検証する。
- Market Regime判定: RANGE、TREND、UNSTABLEへ分類される代表データと、閾値の境界を検証する。
- Grid Suitability: Scoreの0、100、Level境界と、判定理由が入力Featureと整合することを検証する。
- Repository/DB: Migration適用、Candleの保存、Unique Constraint、重複再取得、確定足と未確定足の扱いを検証する。
- MarketDataProvider: 正常な変換、外部APIの異常、空データ、不正レスポンスを検証する。
- API: 必要に応じて、正常レスポンス、入力Validation、エラー変換、health checkを検証する。

テストデータは固定値または決定的な生成方法を使い、実行時刻や外部APIの状態に結果を依存させない。特に時系列テストでは、分析時点より後のデータを入力へ含めないことを明示的に確認し、look-ahead biasを防ぐ。

Frontendにテストを追加する場合は、既存の構成を確認してから必要最小限のツールを導入する。テスト導入だけを理由に新しい状態管理やデータ取得ライブラリを追加しない。

## 10. 設定とセキュリティ

- Secret、Private Key、API Key、DBパスワードなどをGitへコミットしない。
- 環境依存値をソースコード、Migration、Frontendの公開コードへハードコードしない。
- `.env` などの実値ファイルは適切にGitの対象外とし、共有が必要な場合は値を含まないテンプレートだけを管理する。
- 将来API Key等が必要になった場合も、環境変数など実行環境から注入する。今回、認証やOAuthの仕組みを追加しない。
- Public Market Dataのみを利用するMVPの範囲で、秘密情報を必要とする外部APIやPrivate APIへ拡張しない。

## 11. GitとAIを使った開発

- 変更前に、対象機能に関係するdocs、既存コード、既存テスト、設定を確認する。
- 指示されたファイル・機能の範囲以外を勝手に変更しない。
- 無関係なリファクタリング、命名変更、フォーマット変更、依存ライブラリ追加を同時に行わない。
- 新しいライブラリは、既存の実装で解決できない必要性があり、採用理由と影響範囲を説明できる場合に限る。
- docsと実装の矛盾を発見しても、独断で大規模修正せず、差異と選択肢を報告する。
- 仕様に不明点がある場合は、推測で機能やAPI契約を追加しない。最小の変更で止めるか、仕様の確認を依頼する。
- テスト可能な変更では、関連テストを追加・更新し、実行結果を確認する。
- AIへ依頼する場合も、対象ファイル、変更しない範囲、確認すべきdocs、受け入れ条件を明示する。
- 変更後はdiffを確認し、要求されていないファイルが変更されていないことを確認する。

## 12. 段階的な実装

実装順は、このガイドラインで新しく管理しない。`docs/implementation-spec.md` の実装Stepを参照する。

同仕様では、Backend起動とhealth check、HyperliquidからのCandle取得、PostgreSQLへの保存、Indicator、Feature、Market Regime、Grid Suitability、Analysis API、Backtest、Dashboardまでの実装項目がStep 1〜10として定義されている。

Backend実装は原則として `docs/implementation-spec.md` の実装Stepを基準に進める。Stepは依存関係や実装順を判断するための基本方針として利用する。既に実装済みのFrontend UI Prototypeは例外として扱い、Step順に合わせるためだけに作り直さない。未実装部分については、必要な前提を無視して先のBackend機能を実装しない。

将来候補として扱えるのは、既存docsに記載されたWebSocketによるRealtime取得、追加のHistorical Data Provider、Strategy Selector、Backtest画面の拡張などに限る。候補は現在の必須要件と混同せず、この文書で未定義の具体的な設計や責務を追加しない。
