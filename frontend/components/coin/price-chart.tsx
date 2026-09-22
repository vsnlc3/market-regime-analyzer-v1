"use client"

import { useEffect, useRef } from "react"
import {
  createChart,
  ColorType,
  CrosshairMode,
  LineStyle,
  type IChartApi,
  type UTCTimestamp,
} from "lightweight-charts"
import type { Candle } from "@/lib/mock-data"

const UP = "#3fb27f"
const DOWN = "#e06c6c"
const GRID = "rgba(255,255,255,0.045)"
const TEXT = "#8b93a1"
const RANGE_LINE = "#6aa6c9"

export function PriceChart({
  candles,
  rangeUpper,
  rangeLower,
}: {
  candles: Candle[]
  rangeUpper?: number
  rangeLower?: number
}) {
  const containerRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<IChartApi | null>(null)

  useEffect(() => {
    const container = containerRef.current
    if (!container) return

    const chart = createChart(container, {
      layout: {
        background: { type: ColorType.Solid, color: "rgba(0,0,0,0)" },
        textColor: TEXT,
        fontFamily: "var(--font-geist-mono), monospace",
        fontSize: 11,
      },
      grid: {
        vertLines: { color: GRID },
        horzLines: { color: GRID },
      },
      crosshair: {
        mode: CrosshairMode.Normal,
        vertLine: { color: TEXT, width: 1, style: LineStyle.Dotted, labelBackgroundColor: "#2a2f38" },
        horzLine: { color: TEXT, width: 1, style: LineStyle.Dotted, labelBackgroundColor: "#2a2f38" },
      },
      rightPriceScale: { borderColor: "rgba(255,255,255,0.08)" },
      timeScale: { borderColor: "rgba(255,255,255,0.08)", timeVisible: true, secondsVisible: false },
      handleScale: false,
      handleScroll: false,
      autoSize: true,
    })
    chartRef.current = chart

    const candleSeries = chart.addCandlestickSeries({
      upColor: UP,
      downColor: DOWN,
      borderUpColor: UP,
      borderDownColor: DOWN,
      wickUpColor: UP,
      wickDownColor: DOWN,
    })
    candleSeries.setData(
      candles.map((c) => ({
        time: c.time as UTCTimestamp,
        open: c.open,
        high: c.high,
        low: c.low,
        close: c.close,
      })),
    )

    if (rangeUpper !== undefined) {
      candleSeries.createPriceLine({
        price: rangeUpper,
        color: RANGE_LINE,
        lineWidth: 1,
        lineStyle: LineStyle.Dashed,
        axisLabelVisible: true,
        title: "Range Upper",
      })
    }
    if (rangeLower !== undefined) {
      candleSeries.createPriceLine({
        price: rangeLower,
        color: RANGE_LINE,
        lineWidth: 1,
        lineStyle: LineStyle.Dashed,
        axisLabelVisible: true,
        title: "Range Lower",
      })
    }

    const volumeSeries = chart.addHistogramSeries({
      priceFormat: { type: "volume" },
      priceScaleId: "volume",
    })
    volumeSeries.priceScale().applyOptions({
      scaleMargins: { top: 0.82, bottom: 0 },
    })
    volumeSeries.setData(
      candles.map((c) => ({
        time: c.time as UTCTimestamp,
        value: c.volume,
        color: c.close >= c.open ? "rgba(63,178,127,0.35)" : "rgba(224,108,108,0.35)",
      })),
    )

    chart.timeScale().fitContent()

    return () => {
      chart.remove()
      chartRef.current = null
    }
  }, [candles, rangeUpper, rangeLower])

  return <div ref={containerRef} className="h-[340px] w-full" aria-label="Price candlestick chart" />
}
