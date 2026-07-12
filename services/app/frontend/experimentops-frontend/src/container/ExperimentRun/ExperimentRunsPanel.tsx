import type { ReactNode } from 'react'

export function ExperimentRunsPanel({
  children,
  onAdd,
}: {
  children: ReactNode
  onAdd: () => void
}) {
  return (
    <div>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="font-heading text-2xl font-semibold text-secondary">
            Experiment runs
          </h2>
        </div>
        <button
          className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90"
          onClick={onAdd}
          type="button"
        >
          + Add Experiment Run
        </button>
      </div>
      <div className="mt-6">{children}</div>
    </div>
  )
}
