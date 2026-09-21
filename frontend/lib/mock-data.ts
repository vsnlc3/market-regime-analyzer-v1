export type Regime = "RANGE" | "TREND" | "UNSTABLE"
export type SuitabilityLevel = "HIGH" | "MEDIUM" | "LOW" | "UNSUITABLE"
export type Timeframe = "15m" | "1h" | "4h" | "1d"
export type AnalysisWindow = "24H" | "3D" | "7D" | "14D" | "30D"

export const TIMEFRAMES: Timeframe[] = ["15m", "1h", "4h", "1d"]
export const WINDOWS: AnalysisWindow[] = ["24H", "3D", "7D", "14D", "30D"]

export const DEFAULT_TIMEFRAME: Timeframe = "1h"
export const DEFAULT_WINDOW: AnalysisWindow = "7D"

/** number of candles represented by each analysis window on a 1h timeframe */
export const WINDOW_CANDLES: Record<AnalysisWindow, number> = {
  "24H": 24,
  "3D": 72,
  "7D": 168,
  "14D": 336,
  "30D": 720,
}

export interface Features {
  trendStrength: number
  volatility: number
  rangeStability: number
  oscillation: number
  breakoutRisk: number
}

export interface Indicators {
  adx: number
  atrPct: number
  efficiencyRatio: number
  ema20: number
  ema50: number
  rangeStayRatio: number
  reversalCount: number
  rangeBreakCount: number
}

export interface Candle {
  time: number
  open: number
  high: number
  low: number
  close: number
  volume: number
}

export interface CoinAnalysis {
  symbol: string
  name: string
  price: number
  regime: Regime
  confidence: number
  gridSuitability: number
  level: SuitabilityLevel
  features: Features
  indicators: Indicators
  reasons: string[]
  rangeUpper: number
  rangeLower: number
  candles: Candle[]
  windowCandles: number
}

export interface CoinBase {
  symbol: string
  name: string
  price: number
  regime: Regime
  confidence: number
  gridSuitability: number
  features: Features
  indicators: Indicators
  reasons: string[]
  /** relative volatility used to shape the candlestick mock */
  chartVolatility: number
  /** per-candle drift as a fraction; positive = uptrend */
  chartDrift: number
}

export const COIN_ORDER = ["BTC", "ETH", "SOL", "XRP"] as const

const COIN_BASE: Record<string, CoinBase> = {
  BTC: {
    symbol: "BTC",
    name: "Bitcoin",
    price: 112430,
    regime: "RANGE",
    confidence: 82,
    gridSuitability: 86,
    features: {
      trendStrength: 18,
      volatility: 64,
      rangeStability: 84,
      oscillation: 91,
      breakoutRisk: 21,
    },
    indicators: {
      adx: 16.4,
      atrPct: 1.2,
      efficiencyRatio: 0.18,
      ema20: 112430,
      ema50: 112180,
      rangeStayRatio: 91,
      reversalCount: 14,
      rangeBreakCount: 2,
    },
    reasons: [
      "Directional trend is weak",
      "Price remains inside a stable range",
      "Reversal frequency is high",
      "Volatility is sufficient for grid capture",
      "Breakout risk is currently low",
    ],
    chartVolatility: 0.9,
    chartDrift: 0.0,
  },
  ETH: {
    symbol: "ETH",
    name: "Ethereum",
    price: 4320,
    regime: "RANGE",
    confidence: 68,
    gridSuitability: 74,
    features: {
      trendStrength: 34,
      volatility: 58,
      rangeStability: 71,
      oscillation: 76,
      breakoutRisk: 33,
    },
    indicators: {
      adx: 21.7,
      atrPct: 1.6,
      efficiencyRatio: 0.29,
      ema20: 4320,
      ema50: 4265,
      rangeStayRatio: 78,
      reversalCount: 11,
      rangeBreakCount: 4,
    },
    reasons: [
      "Trend is present but not dominant",
      "Range holds on most candles",
      "Reversal frequency is moderate",
      "Volatility supports grid spacing",
      "Breakout risk is contained",
    ],
    chartVolatility: 1.1,
    chartDrift: 0.02,
  },
  SOL: {
    symbol: "SOL",
    name: "Solana",
    price: 238.4,
    regime: "TREND",
    confidence: 79,
    gridSuitability: 31,
    features: {
      trendStrength: 82,
      volatility: 61,
      rangeStability: 27,
      oscillation: 34,
      breakoutRisk: 58,
    },
    indicators: {
      adx: 38.9,
      atrPct: 2.4,
      efficiencyRatio: 0.63,
      ema20: 238.4,
      ema50: 221.7,
      rangeStayRatio: 41,
      reversalCount: 5,
      rangeBreakCount: 9,
    },
    reasons: [
      "Strong directional trend detected",
      "Price is not respecting a stable range",
      "Reversal frequency is low",
      "Efficiency ratio indicates persistent movement",
      "Grid would be exposed to trend continuation",
    ],
    chartVolatility: 1.4,
    chartDrift: 0.16,
  },
  XRP: {
    symbol: "XRP",
    name: "XRP",
    price: 3.12,
    regime: "UNSTABLE",
    confidence: 71,
    gridSuitability: 22,
    features: {
      trendStrength: 44,
      volatility: 93,
      rangeStability: 24,
      oscillation: 47,
      breakoutRisk: 88,
    },
    indicators: {
      adx: 27.3,
      atrPct: 4.1,
      efficiencyRatio: 0.41,
      ema20: 3.12,
      ema50: 2.94,
      rangeStayRatio: 38,
      reversalCount: 8,
      rangeBreakCount: 12,
    },
    reasons: [
      "Volatility is extreme",
      "Breakout risk is high",
      "Range stability is poor",
      "Price behavior is erratic",
      "Grid exposure would be unsafe in this regime",
    ],
    chartVolatility: 2.3,
    chartDrift: 0.03,
  },
}

export function levelForScore(score: number): SuitabilityLevel {
  if (score >= 80) return "HIGH"
  if (score >= 60) return "MEDIUM"
  if (score >= 40) return "LOW"
  return "UNSUITABLE"
}

function clamp(n: number, min = 0, max = 100) {
  return Math.max(min, Math.min(max, n))
}

/** deterministic pseudo-random generator seeded from a string */
function seededRandom(seed: string) {
  let h = 2166136261
  for (let i = 0; i < seed.length; i++) {
    h ^= seed.charCodeAt(i)
    h = Math.imul(h, 16777619)
  }
  return function () {
    h += 0x6d2b79f5
    let t = h
    t = Math.imul(t ^ (t >>> 15), t | 1)
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61)
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

/** small signed offset in [-range, range] derived from a seed */
function seedOffset(seed: string, range: number) {
  return (seededRandom(seed)() * 2 - 1) * range
}

function timeframeSeconds(tf: Timeframe): number {
  switch (tf) {
    case "15m":
      return 15 * 60
    case "1h":
      return 60 * 60
    case "4h":
      return 4 * 60 * 60
    case "1d":
      return 24 * 60 * 60
  }
}

function generateCandles(base: CoinBase, tf: Timeframe, count: number): Candle[] {
  const rng = seededRandom(`${base.symbol}-${tf}`)
  const step = timeframeSeconds(tf)
  const now = Math.floor(Date.now() / 1000)
  const start = now - step * count

  const candles: Candle[] = []
  // anchor the series so the last close lands near the quoted price
  let price = base.price * (1 - base.chartDrift * count * 0.5) || base.price
  const amp = base.price * 0.006 * base.chartVolatility

  for (let i = 0; i < count; i++) {
    const drift = base.price * base.chartDrift * 0.01
    const noise = (rng() - 0.5) * 2 * amp
    const open = price
    let close = open + drift + noise
    // mean-reversion pull for range-like assets
    if (base.regime === "RANGE") {
      close += (base.price - close) * 0.04
    }
    const wick = amp * (0.4 + rng() * 0.9)
    const high = Math.max(open, close) + wick * rng()
    const low = Math.min(open, close) - wick * rng()
    const volume = base.price * (0.5 + rng() * 1.5) * base.chartVolatility

    candles.push({
      time: start + i * step,
      open: round(open, base.price),
      high: round(high, base.price),
      low: round(low, base.price),
      close: round(close, base.price),
      volume: Math.round(volume),
    })
    price = close
  }
  return candles
}

function round(value: number, reference: number): number {
  if (reference >= 1000) return Math.round(value * 100) / 100
  if (reference >= 1) return Math.round(value * 1000) / 1000
  return Math.round(value * 100000) / 100000
}

export function getCoinSummaries(tf: Timeframe, win: AnalysisWindow) {
  return COIN_ORDER.map((symbol) => {
    const a = buildAnalysis(symbol, tf, win)
    return {
      symbol: a.symbol,
      name: a.name,
      price: a.price,
      regime: a.regime,
      gridSuitability: a.gridSuitability,
      level: a.level,
    }
  })
}

export function buildAnalysis(
  symbol: string,
  tf: Timeframe,
  win: AnalysisWindow,
): CoinAnalysis {
  const base = COIN_BASE[symbol] ?? COIN_BASE.BTC
  const seed = `${base.symbol}-${tf}-${win}`

  // deterministic drift of features/score depending on tf + window
  const scoreShift = seedOffset(seed, 6)
  const gridSuitability = Math.round(clamp(base.gridSuitability + scoreShift))
  const confidence = Math.round(clamp(base.confidence + seedOffset(seed + "c", 5), 40, 97))

  const features: Features = {
    trendStrength: Math.round(clamp(base.features.trendStrength + seedOffset(seed + "t", 5))),
    volatility: Math.round(clamp(base.features.volatility + seedOffset(seed + "v", 5))),
    rangeStability: Math.round(clamp(base.features.rangeStability + seedOffset(seed + "r", 5))),
    oscillation: Math.round(clamp(base.features.oscillation + seedOffset(seed + "o", 5))),
    breakoutRisk: Math.round(clamp(base.features.breakoutRisk + seedOffset(seed + "b", 5))),
  }

  const priceJitter = 1 + seedOffset(seed + "p", 0.015)
  const price = round(base.price * priceJitter, base.price)

  const indicators: Indicators = {
    adx: round2(base.indicators.adx + seedOffset(seed + "adx", 2)),
    atrPct: round2(base.indicators.atrPct + seedOffset(seed + "atr", 0.3)),
    efficiencyRatio: round2(base.indicators.efficiencyRatio + seedOffset(seed + "er", 0.05)),
    ema20: round(base.price * (1 + seedOffset(seed + "e20", 0.004)), base.price),
    ema50: round(base.price * (1 + seedOffset(seed + "e50", 0.01) - 0.004), base.price),
    rangeStayRatio: Math.round(clamp(base.indicators.rangeStayRatio + seedOffset(seed + "rsr", 4))),
    reversalCount: Math.max(0, Math.round(base.indicators.reversalCount + seedOffset(seed + "rc", 3))),
    rangeBreakCount: Math.max(0, Math.round(base.indicators.rangeBreakCount + seedOffset(seed + "rbc", 2))),
  }

  const windowCandles = WINDOW_CANDLES[win]
  const chartCount = Math.min(180, Math.max(60, windowCandles))
  const candles = generateCandles(base, tf, chartCount)

  const lows = candles.map((c) => c.low)
  const highs = candles.map((c) => c.high)
  const rangeLower = round(Math.min(...lows), base.price)
  const rangeUpper = round(Math.max(...highs), base.price)

  return {
    symbol: base.symbol,
    name: base.name,
    price,
    regime: base.regime,
    confidence,
    gridSuitability,
    level: levelForScore(gridSuitability),
    features,
    indicators,
    reasons: base.reasons,
    rangeUpper,
    rangeLower,
    candles,
    windowCandles,
  }
}

function round2(n: number): number {
  return Math.round(n * 100) / 100
}

export function isValidSymbol(symbol: string): boolean {
  return (COIN_ORDER as readonly string[]).includes(symbol)
}

export function normalizeTimeframe(value: string | undefined): Timeframe {
  return (TIMEFRAMES as string[]).includes(value ?? "") ? (value as Timeframe) : DEFAULT_TIMEFRAME
}

export function normalizeWindow(value: string | undefined): AnalysisWindow {
  return (WINDOWS as string[]).includes(value ?? "") ? (value as AnalysisWindow) : DEFAULT_WINDOW
}

export function formatPrice(price: number): string {
  if (price >= 1000) {
    return "$" + price.toLocaleString("en-US", { minimumFractionDigits: 0, maximumFractionDigits: 0 })
  }
  if (price >= 1) {
    return "$" + price.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  }
  return "$" + price.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 4 })
}
