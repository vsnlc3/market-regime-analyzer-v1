import { AppHeader } from "@/components/app-header"
import { AnalysisControls } from "@/components/analysis-controls"
import { OverviewTable } from "@/components/overview-table"
import {
  getCoinSummaries,
  normalizeTimeframe,
  normalizeWindow,
  WINDOW_CANDLES,
} from "@/lib/mock-data"

export default async function OverviewPage({
  searchParams,
}: {
  searchParams: Promise<{ tf?: string; win?: string }>
}) {
  const sp = await searchParams
  const timeframe = normalizeTimeframe(sp.tf)
  const window = normalizeWindow(sp.win)
  const summaries = getCoinSummaries(timeframe, window)
  const query = new URLSearchParams({ tf: timeframe, win: window }).toString()

  return (
    <div className="min-h-screen">
      <AppHeader />
      <main className="mx-auto max-w-[1400px] px-4 py-6 md:px-6 md:py-8">
        <div className="flex flex-col gap-5">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div className="flex flex-col gap-1">
              <h1 className="text-lg font-semibold tracking-tight text-foreground">Market Overview</h1>
              <p className="max-w-xl text-sm text-muted-foreground">
                Compare the current regime and grid-trading suitability across assets, then open a
                coin to understand the reasoning behind its score.
              </p>
            </div>
            <AnalysisControls timeframe={timeframe} window={window} />
          </div>

          <OverviewTable summaries={summaries} query={query} />

          <p className="text-xs text-muted-foreground">
            Showing {timeframe} candles over a {window} window ({WINDOW_CANDLES[window]} candles).
            Detailed features such as Trend Strength, Volatility and Breakout Risk are available in
            each coin&apos;s analysis view.
          </p>
        </div>
      </main>
    </div>
  )
}
