"use client"

import { useRouter } from "next/navigation"
import { ChevronRight } from "lucide-react"
import { cn } from "@/lib/utils"
import { formatPrice, type Regime, type SuitabilityLevel } from "@/lib/mock-data"
import { regimeClasses, levelText, levelBar } from "@/lib/display"

interface Summary {
  symbol: string
  name: string
  price: number
  regime: Regime
  gridSuitability: number
  level: SuitabilityLevel
}

export function OverviewTable({
  summaries,
  query,
}: {
  summaries: Summary[]
  query: string
}) {
  const router = useRouter()

  function open(symbol: string) {
    router.push(`/coin/${symbol}?${query}`)
  }

  return (
    <div className="overflow-x-auto rounded-md border border-border">
      <table className="w-full min-w-[640px] border-collapse text-sm">
        <thead>
          <tr className="border-b border-border text-xs uppercase tracking-wider text-muted-foreground">
            <th className="px-4 py-2.5 text-left font-medium">Coin</th>
            <th className="px-4 py-2.5 text-right font-medium">Current Price</th>
            <th className="px-4 py-2.5 text-left font-medium">Market Regime</th>
            <th className="px-4 py-2.5 text-left font-medium">Grid Suitability</th>
            <th className="w-10 px-4 py-2.5" aria-hidden />
          </tr>
        </thead>
        <tbody>
          {summaries.map((s) => (
            <tr
              key={s.symbol}
              tabIndex={0}
              role="link"
              aria-label={`Open ${s.symbol} analysis`}
              onClick={() => open(s.symbol)}
              onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ") {
                  e.preventDefault()
                  open(s.symbol)
                }
              }}
              className="group cursor-pointer border-b border-border/60 outline-none transition-colors last:border-0 hover:bg-accent/60 focus-visible:bg-accent/60"
            >
              <td className="px-4 py-3">
                <div className="flex items-center gap-3">
                  <span className="flex h-8 w-8 items-center justify-center rounded-md border border-border bg-panel font-mono text-xs font-semibold text-foreground">
                    {s.symbol}
                  </span>
                  <span className="text-muted-foreground">{s.name}</span>
                </div>
              </td>
              <td className="px-4 py-3 text-right font-mono tabular-nums text-foreground">
                {formatPrice(s.price)}
              </td>
              <td className="px-4 py-3">
                <span
                  className={cn(
                    "inline-flex items-center rounded border px-2 py-0.5 font-mono text-xs font-medium tracking-wide",
                    regimeClasses[s.regime],
                  )}
                >
                  {s.regime}
                </span>
              </td>
              <td className="px-4 py-3">
                <div className="flex items-center gap-3">
                  <div className="h-1.5 w-24 overflow-hidden rounded-full bg-muted">
                    <div
                      className={cn("h-full rounded-full", levelBar[s.level])}
                      style={{ width: `${s.gridSuitability}%` }}
                    />
                  </div>
                  <span className="font-mono tabular-nums text-foreground">{s.gridSuitability}</span>
                  <span className={cn("font-mono text-xs font-medium", levelText[s.level])}>
                    {s.level}
                  </span>
                </div>
              </td>
              <td className="px-4 py-3 text-right">
                <ChevronRight className="ml-auto h-4 w-4 text-muted-foreground transition-transform group-hover:translate-x-0.5 group-hover:text-foreground" />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
