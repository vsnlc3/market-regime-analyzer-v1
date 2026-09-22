import type { Candle, Features, Indicators, Timeframe, AnalysisWindow } from "@/lib/mock-data"
import type { AnalysisApiResponse, CandleApiResponse } from "@/lib/api"

const COIN_NAMES: Record<string, string> = {
  BTC: "Bitcoin",
  ETH: "Ethereum",
  SOL: "Solana",
  XRP: "XRP",
}

export interface AnalysisViewModel {
  symbol: string
  name: string
  price: number
  regime: AnalysisApiResponse["regime"]
  gridSuitability: number
  level: AnalysisApiResponse["gridSuitabilityLevel"]
  features: Features
  indicators: Indicators
  reasons: string[]
  dataAsOf: string
  timeframe: Timeframe
  window: AnalysisWindow
  windowCandles: number
}

export interface CoinAnalysisViewModel extends AnalysisViewModel {
  candles: Candle[]
}

function numberValue(value: number | string, field: string): number {
  const parsed = typeof value === "number" ? value : Number(value)
  if (!Number.isFinite(parsed)) {
    throw new Error(`Invalid numeric value in API response: ${field}`)
  }
  return parsed
}

export function toAnalysisViewModel(
  response: AnalysisApiResponse,
  timeframe: Timeframe,
  window: AnalysisWindow,
): AnalysisViewModel {
  return {
    symbol: response.symbol,
    name: COIN_NAMES[response.symbol] ?? response.symbol,
    price: numberValue(response.currentPrice, "currentPrice"),
    regime: response.regime,
    gridSuitability: numberValue(response.gridSuitability, "gridSuitability"),
    level: response.gridSuitabilityLevel,
    features: {
      trendStrength: numberValue(response.features.trendStrength, "features.trendStrength"),
      volatility: numberValue(response.features.volatility, "features.volatility"),
      rangeStability: numberValue(response.features.rangeStability, "features.rangeStability"),
      oscillation: numberValue(response.features.oscillation, "features.oscillation"),
      breakoutRisk: numberValue(response.features.breakoutRisk, "features.breakoutRisk"),
    },
    indicators: {
      adx: numberValue(response.indicators.adx, "indicators.adx"),
      atrPct: numberValue(response.indicators.atrPct, "indicators.atrPct"),
      efficiencyRatio: numberValue(response.indicators.efficiencyRatio, "indicators.efficiencyRatio"),
      ema20: numberValue(response.indicators.ema20, "indicators.ema20"),
      ema50: numberValue(response.indicators.ema50, "indicators.ema50"),
      rangeStayRatio: numberValue(response.indicators.rangeStayRatio, "indicators.rangeStayRatio"),
      reversalCount: numberValue(response.indicators.reversalCount, "indicators.reversalCount"),
      rangeBreakCount: numberValue(response.indicators.rangeBreakCount, "indicators.rangeBreakCount"),
    },
    reasons: response.reasons,
    dataAsOf: response.dataAsOf,
    timeframe,
    window,
    windowCandles: response.window,
  }
}

export function toChartCandle(response: CandleApiResponse): Candle {
  const time = Math.floor(Date.parse(response.openTime) / 1000)
  if (!Number.isFinite(time)) {
    throw new Error("Invalid openTime in Candle API response")
  }
  return {
    time,
    open: numberValue(response.open, "open"),
    high: numberValue(response.high, "high"),
    low: numberValue(response.low, "low"),
    close: numberValue(response.close, "close"),
    volume: numberValue(response.volume, "volume"),
  }
}

export function toCoinAnalysisViewModel(
  response: AnalysisApiResponse,
  candles: CandleApiResponse[],
  timeframe: Timeframe,
  window: AnalysisWindow,
): CoinAnalysisViewModel {
  return {
    ...toAnalysisViewModel(response, timeframe, window),
    candles: candles.map(toChartCandle),
  }
}

export function toOverviewSummary(response: AnalysisApiResponse) {
  return {
    symbol: response.symbol,
    name: COIN_NAMES[response.symbol] ?? response.symbol,
    price: numberValue(response.currentPrice, "currentPrice"),
    regime: response.regime,
    gridSuitability: numberValue(response.gridSuitability, "gridSuitability"),
    level: response.gridSuitabilityLevel,
  }
}
