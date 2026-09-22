import { Card } from "@/components/ui/card"
import { formatPrice, type Indicators } from "@/lib/mock-data"

export function IndicatorDetails({
  indicators,
  price,
}: {
  indicators: Indicators
  price: number
}) {
  const rows: { label: string; value: string; hint: string }[] = [
    { label: "ADX", value: indicators.adx.toFixed(1), hint: "Trend strength index" },
    { label: "ATR %", value: `${indicators.atrPct.toFixed(2)}%`, hint: "Avg. true range" },
    { label: "Efficiency Ratio", value: indicators.efficiencyRatio.toFixed(2), hint: "Directional efficiency" },
    { label: "EMA 20", value: formatPrice(indicators.ema20), hint: "Fast moving average" },
    { label: "EMA 50", value: formatPrice(indicators.ema50), hint: "Slow moving average" },
    { label: "Range Stay Ratio", value: `${indicators.rangeStayRatio}%`, hint: "Candles inside range" },
    { label: "Reversals", value: String(indicators.reversalCount), hint: "Direction flips in window" },
    { label: "Range Breaks", value: String(indicators.rangeBreakCount), hint: "Band exits in window" },
  ]

  return (
    <Card className="p-5">
      <div className="mb-4 flex items-center justify-between">
        <span className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
          Indicator Details
        </span>
        <span className="font-mono text-xs text-muted-foreground">Last close {formatPrice(price)}</span>
      </div>
      <dl className="grid grid-cols-2 gap-x-6 gap-y-4 sm:grid-cols-3 lg:grid-cols-4">
        {rows.map((r) => (
          <div key={r.label} className="flex flex-col gap-1 border-l border-border pl-3">
            <dt className="text-[11px] uppercase tracking-wider text-muted-foreground">{r.label}</dt>
            <dd className="font-mono text-lg tabular-nums text-foreground">{r.value}</dd>
            <dd className="text-[11px] text-muted-foreground">{r.hint}</dd>
          </div>
        ))}
      </dl>
    </Card>
  )
}
