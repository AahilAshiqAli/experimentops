export function TableSkeleton() {
  return (
    <div className="space-y-2" role="status">
      <div className="h-12 animate-pulse rounded bg-slate-100" />
      <div className="h-10 animate-pulse rounded bg-slate-100" />
      <div className="h-10 animate-pulse rounded bg-slate-100" />
      <span className="sr-only">Loading rows</span>
    </div>
  )
}
