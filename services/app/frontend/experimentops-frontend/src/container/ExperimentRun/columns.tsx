import type { DataTableColumn } from '../../components/DataTable'
import type { ExperimentRun } from '../../services/experimentRun.service'

function statusClassName(status: ExperimentRun['status']) {
  if (status === 'SUCCEEDED') return 'bg-emerald-100 text-emerald-700'
  if (status === 'FAILED') return 'bg-red-100 text-red-700'
  if (status === 'RUNNING') return 'bg-blue-100 text-blue-700'
  return 'bg-amber-100 text-amber-700'
}

export function getExperimentRunColumns(): DataTableColumn<ExperimentRun>[] {
  return [
    {
      header: 'Name',
      key: 'name',
      value: (run) => run.name,
    },
    {
      align: 'center',
      cell: (run) => (
        <span
          className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${statusClassName(
            run.status,
          )}`}
        >
          {run.status}
        </span>
      ),
      header: 'Status',
      key: 'status',
      value: (run) => run.status,
    },
    {
      cell: (run) => (
        <div className="min-w-28">
          <div className="mb-1 flex justify-between text-xs text-slate-600">
            <span>{run.progress}%</span>
          </div>
          <div className="h-2 overflow-hidden rounded-full bg-slate-100">
            <div
              className="h-full rounded-full bg-primary transition-all"
              style={{ width: `${Math.max(0, Math.min(100, run.progress))}%` }}
            />
          </div>
        </div>
      ),
      header: 'Progress',
      key: 'progress',
      value: (run) => run.progress,
    },
    {
      align: 'center',
      header: 'Datasets',
      key: 'datasetCount',
      value: (run) => run.datasetCount,
    },
    {
      header: 'Duration',
      key: 'duration',
      value: (run) => run.duration,
    },
    {
      header: 'Pipeline',
      key: 'executionMode',
      value: (run) =>
        `${run.executionMode.length} step${run.executionMode.length === 1 ? '' : 's'}`,
    },
  ]
}
