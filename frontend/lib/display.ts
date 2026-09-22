import type { Regime, SuitabilityLevel } from "@/lib/mock-data"

export const regimeClasses: Record<Regime, string> = {
  RANGE: "text-regime-range border-regime-range/40 bg-regime-range/10",
  TREND: "text-regime-trend border-regime-trend/40 bg-regime-trend/10",
  UNSTABLE: "text-regime-unstable border-regime-unstable/40 bg-regime-unstable/10",
}

export const regimeText: Record<Regime, string> = {
  RANGE: "text-regime-range",
  TREND: "text-regime-trend",
  UNSTABLE: "text-regime-unstable",
}

export const levelClasses: Record<SuitabilityLevel, string> = {
  HIGH: "text-level-high border-level-high/40 bg-level-high/10",
  MEDIUM: "text-level-medium border-level-medium/40 bg-level-medium/10",
  LOW: "text-level-low border-level-low/40 bg-level-low/10",
  UNSUITABLE: "text-level-unsuitable border-level-unsuitable/40 bg-level-unsuitable/10",
}

export const levelText: Record<SuitabilityLevel, string> = {
  HIGH: "text-level-high",
  MEDIUM: "text-level-medium",
  LOW: "text-level-low",
  UNSUITABLE: "text-level-unsuitable",
}

export const levelBar: Record<SuitabilityLevel, string> = {
  HIGH: "bg-level-high",
  MEDIUM: "bg-level-medium",
  LOW: "bg-level-low",
  UNSUITABLE: "bg-level-unsuitable",
}

export const regimeDescription: Record<Regime, string> = {
  RANGE: "Price is oscillating within a bounded band",
  TREND: "Price is moving directionally with persistence",
  UNSTABLE: "Price behavior is erratic and hard to model",
}
