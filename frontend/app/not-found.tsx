import Link from "next/link"
import { AppHeader } from "@/components/app-header"

export default function NotFound() {
  return (
    <div className="min-h-screen">
      <AppHeader />
      <main className="mx-auto flex max-w-[1400px] flex-col items-center justify-center gap-4 px-4 py-32 text-center">
        <span className="font-mono text-4xl font-semibold text-muted-foreground">404</span>
        <h1 className="text-lg font-semibold text-foreground">Coin not found</h1>
        <p className="max-w-sm text-sm text-muted-foreground">
          That asset is not part of the tracked universe. Head back to the overview to pick a
          supported coin.
        </p>
        <Link
          href="/"
          className="mt-2 rounded-md border border-border bg-panel px-4 py-2 text-sm text-foreground transition-colors hover:bg-accent"
        >
          Back to Overview
        </Link>
      </main>
    </div>
  )
}
