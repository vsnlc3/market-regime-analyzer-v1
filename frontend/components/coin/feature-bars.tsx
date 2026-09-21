import { cn } from "@/lib/utils"
import { Card } from "@/components/ui/card"
import type { Features } from "@/lib/mock-data"

interface FeatureDef {
  key: keyof Features
  label: string
  hint: string
  /** whether a high value is favorable (green) or unfavorable (red) for grids */
  favorableHigh: boolean
}

const FEATURES: FeatureDef[] = [
  { key: "trendStrength", label: "Trend Strength", hint: "Directional persistence", favorableHigh: false },
  { key: "volatility", label: "Volatility", hint: "Movement amplitude", favorableHigh: true },
  { key: "rangeStability", label: "Range Stability", hint: "Band adherence", favorableHigh: true },
  { key: "oscillation", label: "Oscillation", hint: "Back-and-forth motion", favorableHigh: true },
  { key: "breakoutRisk", label: "Breakout Risk", hint: "Range-exit probability", favorableHigh: false },
]

function toneFor(value: number, favorableHigh: boolean): string {
  const good = favorableHigh ? value >= 60 : value <= 40
  const bad = favorableHigh ? value <= 35 : value >= 65
  if (good) return "bg-level-high"
  if (bad) return "bg-level-unsuitable"
  return "bg-level-medium"
}

export function FeatureBars({ features }: { features: Features }) {
  return (
    <Card className="p-5">
      <div className="mb-4 flex items-center justify-between">
        <span className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
          Market Features
        </span>
        <span className="text-xs text-muted-foreground">0 – 100</span>
      </div>
      <div className="flex flex-col gap-3.5">
        {FEATURES.map((f) => {
          const value = features[f.key]
          return (
            <div key={f.key} className="grid grid-cols-[9.5rem_1fr_2.5rem] items-center gap-3">
              <div className="flex flex-col">
                <span className="text-sm text-foreground">{f.label}</span>
                <span className="text-[11px] text-muted-foreground">{f.hint}</span>
              </div>
              <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
                <div
                  className={cn("h-full rounded-full transition-all", toneFor(value, f.favorableHigh))}
                  style={{ width: `${value}%` }}
                />
              </div>
              <span className="text-right font-mono text-sm tabular-nums text-foreground">
                {value}
              </span>
            </div>
          )
        })}
      </div>
    </Card>
  )
}
