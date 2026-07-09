export function ExperimentRunsPanel({ onAdd }: { onAdd: () => void }) {
  return (
    <div>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="font-heading text-2xl font-semibold text-secondary">
            Experiment runs
          </h2>
          <p className="mt-1 text-sm text-slate-600">
            Run this experiment against a dataset using an ordered config
            pipeline.
          </p>
        </div>
        <button
          className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90"
          onClick={onAdd}
          type="button"
        >
          + Add Experiment Run
        </button>
      </div>
      <div className="mt-6 rounded-lg border border-dashed border-slate-300 px-6 py-12 text-center text-sm text-slate-600">
        No experiment runs to display yet.
      </div>
    </div>
  )
}
