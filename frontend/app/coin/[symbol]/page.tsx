import { notFound } from "next/navigation"
import { AppHeader } from "@/components/app-header"
import { CoinAnalysisHeader } from "@/components/coin/coin-analysis-header"
import { RegimeCard } from "@/components/coin/regime-card"
import { SuitabilityCard } from "@/components/coin/suitability-card"
import { FeatureBars } from "@/components/coin/feature-bars"
import { PriceChart } from "@/components/coin/price-chart"
import { AnalysisReasons } from "@/components/coin/analysis-reasons"
import { IndicatorDetails } from "@/components/coin/indicator-details"
import { Card } from "@/components/ui/card"
import {
  buildAnalysis,
  isValidSymbol,
  normalizeTimeframe,
  normalizeWindow,
  WINDOW_CANDLES,
} from "@/lib/mock-data"

export default async function CoinAnalysisPage({
  params,
  searchParams,
}: {
  params: Promise<{ symbol: string }>
  searchParams: Promise<{ tf?: string; win?: string }>
}) {
  const { symbol: rawSymbol } = await params
  const symbol = decodeURIComponent(rawSymbol).toUpperCase()
  if (!isValidSymbol(symbol)) notFound()

  const sp = await searchParams
  const timeframe = normalizeTimeframe(sp.tf)
  const window = normalizeWindow(sp.win)
  const query = new URLSearchParams({ tf: timeframe, win: window }).toString()

  const a = buildAnalysis(symbol, timeframe, window)

  return (
    <div className="min-h-screen">
      <AppHeader />
      <main className="mx-auto max-w-[1400px] px-4 py-6 md:px-6 md:py-8">
        <div className="flex flex-col gap-6">
          <CoinAnalysisHeader
            symbol={a.symbol}
            name={a.name}
            price={a.price}
            timeframe={timeframe}
            window={window}
            query={query}
          />

          {/* Primary conclusion */}
          <section className="grid gap-4 md:grid-cols-2" aria-label="Analysis conclusion">
            <RegimeCard regime={a.regime} confidence={a.confidence} />
            <SuitabilityCard score={a.gridSuitability} level={a.level} />
          </section>

          <FeatureBars features={a.features} />

          <section className="grid gap-4 lg:grid-cols-3" aria-label="Price action and reasoning">
            <Card className="flex flex-col p-5 lg:col-span-2">
              <div className="mb-3 flex items-center justify-between">
                <span className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                  Price Action
                </span>
                <span className="font-mono text-xs text-muted-foreground">
                  {timeframe} · {window} · {WINDOW_CANDLES[window]} candles
                </span>
              </div>
              <PriceChart
                candles={a.candles}
                rangeUpper={a.rangeUpper}
                rangeLower={a.rangeLower}
              />
            </Card>
            <AnalysisReasons reasons={a.reasons} />
          </section>

          <IndicatorDetails indicators={a.indicators} price={a.price} />
        </div>
      </main>
    </div>
  )
}
