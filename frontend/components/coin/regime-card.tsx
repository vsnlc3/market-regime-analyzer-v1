import { cn } from "@/lib/utils"
import { Card } from "@/components/ui/card"
import type { Regime } from "@/lib/mock-data"
import { regimeClasses, regimeText, regimeDescription } from "@/lib/display"

export function RegimeCard({ regime, confidence }: { regime: Regime; confidence: number }) {
  return (
    <Card className="flex flex-col gap-4 p-5">
      <div className="flex items-center justify-between">
        <span className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
          Market Regime
        </span>
        <span className="text-xs text-muted-foreground">Primary conclusion</span>
      </div>

      <div className="flex items-baseline gap-3">
        <span
          className={cn(
            "inline-flex items-center rounded-md border px-3 py-1.5 font-mono text-2xl font-semibold tracking-tight",
            regimeClasses[regime],
          )}
        >
          {regime}
        </span>
      </div>

      <p className="text-sm text-muted-foreground">{regimeDescription[regime]}</p>

      <div className="mt-auto flex flex-col gap-1.5 border-t border-border pt-3">
        <div className="flex items-center justify-between text-xs">
          <span className="uppercase tracking-wider text-muted-foreground">Confidence</span>
          <span className={cn("font-mono tabular-nums", regimeText[regime])}>{confidence}%</span>
        </div>
        <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
          <div
            className={cn("h-full rounded-full", regimeText[regime].replace("text-", "bg-"))}
            style={{ width: `${confidence}%` }}
          />
        </div>
      </div>
    </Card>
  )
}
