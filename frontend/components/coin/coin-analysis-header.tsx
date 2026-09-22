import Link from "next/link"
import { ArrowLeft } from "lucide-react"
import { AnalysisControls } from "@/components/analysis-controls"
import { formatPrice, type Timeframe, type AnalysisWindow } from "@/lib/mock-data"
import { formatDataAsOf } from "@/lib/display"

export function CoinAnalysisHeader({
  symbol,
  name,
  price,
  dataAsOf,
  timeframe,
  window,
  query,
}: {
  symbol: string
  name: string
  price?: number
  dataAsOf?: string
  timeframe: Timeframe
  window: AnalysisWindow
  query: string
}) {
  return (
    <div className="flex flex-col gap-4">
      <Link
        href={`/?${query}`}
        className="inline-flex w-fit items-center gap-1.5 text-sm text-muted-foreground transition-colors hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to Overview
      </Link>

      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div className="flex items-center gap-4">
          <span className="flex h-12 w-12 items-center justify-center rounded-md border border-border bg-panel font-mono text-sm font-semibold text-foreground">
            {symbol}
          </span>
          <div className="flex flex-col gap-0.5">
            <div className="flex items-baseline gap-2.5">
              <h1 className="text-xl font-semibold tracking-tight text-foreground">{symbol}</h1>
              <span className="text-sm text-muted-foreground">{name}</span>
            </div>
            {price !== undefined ? (
              <span className="font-mono text-2xl font-semibold tabular-nums text-foreground">
                {formatPrice(price)}
              </span>
            ) : null}
            {dataAsOf ? (
              <span className="text-xs text-muted-foreground">Last Updated {formatDataAsOf(dataAsOf)}</span>
            ) : null}
          </div>
        </div>

        <AnalysisControls timeframe={timeframe} window={window} />
      </div>
    </div>
  )
}
