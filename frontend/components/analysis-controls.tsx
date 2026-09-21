"use client"

import { useRouter, usePathname, useSearchParams } from "next/navigation"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { TIMEFRAMES, WINDOWS, type Timeframe, type AnalysisWindow } from "@/lib/mock-data"

export function AnalysisControls({
  timeframe,
  window,
}: {
  timeframe: Timeframe
  window: AnalysisWindow
}) {
  const router = useRouter()
  const pathname = usePathname()
  const searchParams = useSearchParams()

  function update(key: "tf" | "win", value: string) {
    const params = new URLSearchParams(searchParams.toString())
    params.set(key, value)
    router.replace(`${pathname}?${params.toString()}`, { scroll: false })
  }

  return (
    <div className="flex flex-wrap items-center gap-4">
      <ControlGroup label="Timeframe">
        <Select value={timeframe} onValueChange={(v) => update("tf", v)}>
          <SelectTrigger className="w-[5.5rem] font-mono">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {TIMEFRAMES.map((tf) => (
              <SelectItem key={tf} value={tf} className="font-mono">
                {tf}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </ControlGroup>

      <ControlGroup label="Analysis Window">
        <Select value={window} onValueChange={(v) => update("win", v)}>
          <SelectTrigger className="w-[5.5rem] font-mono">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {WINDOWS.map((w) => (
              <SelectItem key={w} value={w} className="font-mono">
                {w}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </ControlGroup>
    </div>
  )
}

function ControlGroup({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center gap-2">
      <span className="text-xs uppercase tracking-wider text-muted-foreground">{label}</span>
      {children}
    </div>
  )
}
