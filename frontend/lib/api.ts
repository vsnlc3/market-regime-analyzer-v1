import type { AnalysisWindow, Timeframe } from "@/lib/mock-data"
import { candleCountForWindow } from "@/lib/mock-data"

const MINIMUM_API_WINDOW = 50

export type ApiErrorCode =
  | "INVALID_SYMBOL"
  | "INVALID_TIMEFRAME"
  | "INVALID_WINDOW"
  | "NO_MARKET_DATA"
  | "INSUFFICIENT_CANDLES"
  | "MARKET_DATA_UNAVAILABLE"
  | "API_UNCONFIGURED"
  | "INVALID_API_RESPONSE"

export class ApiClientError extends Error {
  constructor(
    readonly status: number,
    readonly code: ApiErrorCode | string,
    message: string,
  ) {
    super(message)
    this.name = "ApiClientError"
  }
}

export interface AnalysisApiResponse {
  symbol: string
  timeframe: Timeframe
  window: number
  currentPrice: string
  dataAsOf: string
  regime: "RANGE" | "TREND" | "UNSTABLE"
  gridSuitability: number
  gridSuitabilityLevel: "HIGH" | "MEDIUM" | "LOW" | "UNSUITABLE"
  features: {
    trendStrength: number
    volatility: number
    rangeStability: number
    oscillation: number
    breakoutRisk: number
  }
  indicators: {
    adx: number
    atrPct: number
    efficiencyRatio: number
    ema20: string
    ema50: string
    rangeStayRatio: number
    reversalCount: number
    rangeBreakCount: number
  }
  reasons: string[]
}

export interface CandleApiResponse {
  openTime: string
  closeTime: string
  open: string
  high: string
  low: string
  close: string
  volume: string
}

function apiBaseUrl(): string {
  const value = process.env.BACKEND_API_BASE_URL?.trim()
  if (!value) {
    throw new ApiClientError(
      0,
      "API_UNCONFIGURED",
      "Backend API URL is not configured. Set BACKEND_API_BASE_URL.",
    )
  }
  return value.replace(/\/$/, "")
}

function apiWindow(timeframe: Timeframe, window: AnalysisWindow): number {
  const count = candleCountForWindow(timeframe, window)
  if (!Number.isInteger(count) || count < MINIMUM_API_WINDOW) {
    throw new ApiClientError(
      400,
      "INVALID_WINDOW",
      `The selected ${timeframe} / ${window} range requires ${count} candles, but the Backend API requires at least ${MINIMUM_API_WINDOW}.`,
    )
  }
  return count
}

async function getJson<T>(path: string): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${apiBaseUrl()}${path}`, { cache: "no-store" })
  } catch {
    throw new ApiClientError(0, "MARKET_DATA_UNAVAILABLE", "Backend API could not be reached.")
  }

  const body = await response.json().catch(() => null)
  if (!response.ok) {
    throw new ApiClientError(
      response.status,
      body?.code ?? "API_ERROR",
      body?.message ?? "Backend API returned an error.",
    )
  }
  return body as T
}

function query(timeframe: Timeframe, window: AnalysisWindow): string {
  const count = apiWindow(timeframe, window)
  return new URLSearchParams({ timeframe, window: String(count) }).toString()
}

export function fetchAnalysis(
  symbol: string,
  timeframe: Timeframe,
  window: AnalysisWindow,
): Promise<AnalysisApiResponse> {
  return getJson<AnalysisApiResponse>(
    `/api/v1/analysis/${encodeURIComponent(symbol)}?${query(timeframe, window)}`,
  )
}

export function fetchCandles(
  symbol: string,
  timeframe: Timeframe,
  window: AnalysisWindow,
): Promise<CandleApiResponse[]> {
  return getJson<CandleApiResponse[]>(
    `/api/v1/candles/${encodeURIComponent(symbol)}?${query(timeframe, window)}`,
  )
}

export function errorMessage(error: unknown): { code?: string; message: string } {
  if (error instanceof ApiClientError) {
    return { code: error.code, message: error.message }
  }
  return { message: "An unexpected error occurred while loading market data." }
}
