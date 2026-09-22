import { cn } from "@/lib/utils"
import { Card } from "@/components/ui/card"
import type { SuitabilityLevel } from "@/lib/mock-data"
import { levelClasses, levelText } from "@/lib/display"

const levelCopy: Record<SuitabilityLevel, string> = {
  HIGH: "Conditions strongly favor a grid strategy",
  MEDIUM: "A grid can work with careful bounds",
  LOW: "Grid trading is discouraged here",
  UNSUITABLE: "Avoid deploying a grid in this regime",
}

const RADIUS = 52
const CIRC = 2 * Math.PI * RADIUS

export function SuitabilityCard({
  score,
  level,
}: {
  score: number
  level: SuitabilityLevel
}) {
  const dash = (score / 100) * CIRC

  return (
    <Card className="flex flex-col gap-4 p-5">
      <div className="flex items-center justify-between">
        <span className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
          Grid Suitability
        </span>
        <span className="text-xs text-muted-foreground">0 – 100</span>
      </div>

      <div className="flex items-center gap-5">
        <div className="relative h-32 w-32 shrink-0">
          <svg viewBox="0 0 120 120" className="h-full w-full -rotate-90">
            <circle
              cx="60"
              cy="60"
              r={RADIUS}
              fill="none"
              strokeWidth="8"
              className="stroke-muted"
            />
            <circle
              cx="60"
              cy="60"
              r={RADIUS}
              fill="none"
              strokeWidth="8"
              strokeLinecap="round"
              strokeDasharray={`${dash} ${CIRC}`}
              className={cn(levelText[level].replace("text-", "stroke-"))}
            />
          </svg>
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            <span className="font-mono text-3xl font-semibold tabular-nums text-foreground">
              {score}
            </span>
            <span className="text-[10px] uppercase tracking-wider text-muted-foreground">score</span>
          </div>
        </div>

        <div className="flex flex-col gap-3">
          <span
            className={cn(
              "inline-flex w-fit items-center rounded-md border px-2.5 py-1 font-mono text-sm font-semibold tracking-wide",
              levelClasses[level],
            )}
          >
            {level}
          </span>
          <p className="text-sm text-pretty text-muted-foreground">{levelCopy[level]}</p>
        </div>
      </div>
    </Card>
  )
}
