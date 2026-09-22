import { Card } from "@/components/ui/card"
import { Check } from "lucide-react"

export function AnalysisReasons({ reasons }: { reasons: string[] }) {
  return (
    <Card className="flex h-full flex-col p-5">
      <span className="mb-4 text-xs font-medium uppercase tracking-wider text-muted-foreground">
        Analysis Reasons
      </span>
      <ul className="flex flex-col gap-3">
        {reasons.map((reason, i) => (
          <li key={i} className="flex items-start gap-2.5 text-sm text-foreground">
            <span className="mt-0.5 flex h-4 w-4 shrink-0 items-center justify-center rounded-full border border-border bg-panel text-muted-foreground">
              <Check className="h-2.5 w-2.5" />
            </span>
            <span className="text-pretty leading-relaxed">{reason}</span>
          </li>
        ))}
      </ul>
    </Card>
  )
}
