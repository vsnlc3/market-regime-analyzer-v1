import { Card } from "@/components/ui/card"

export function ApiState({
  title,
  message,
  code,
}: {
  title: string
  message: string
  code?: string
}) {
  return (
    <Card className="flex flex-col gap-2 border-border p-6">
      <span className="text-xs font-medium uppercase tracking-wider text-muted-foreground">{title}</span>
      {code ? <span className="font-mono text-sm text-level-low">{code}</span> : null}
      <p className="text-sm text-muted-foreground">{message}</p>
    </Card>
  )
}
