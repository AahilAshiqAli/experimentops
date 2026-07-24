import type { TFunction } from 'i18next'

import type { DataTableColumn } from '../../components/DataTable'
import type { ExperimentRun } from '../../services/experimentRun.service'

function statusClassName(status: ExperimentRun['status']) {
  if (status === 'SUCCEEDED') return 'bg-emerald-100 text-emerald-700'
  if (status === 'FAILED') return 'bg-red-100 text-red-700'
  if (status === 'RUNNING') return 'bg-blue-100 text-blue-700'
  return 'bg-amber-100 text-amber-700'
}

export const RUN_STATUS_TRANSLATION_KEYS = {
  FAILED: 'runs.status.failed',
  PENDING: 'runs.status.pending',
  RUNNING: 'runs.status.running',
  SUCCEEDED: 'runs.status.succeeded',
} as const

export function getExperimentRunColumns(
  t: TFunction,
): DataTableColumn<ExperimentRun>[] {
  return [
    {
      header: t('runs.columns.name'),
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
          {t(RUN_STATUS_TRANSLATION_KEYS[run.status])}
        </span>
      ),
      header: t('runs.columns.status'),
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
      header: t('runs.columns.progress'),
      key: 'progress',
      value: (run) => run.progress,
    },
    {
      align: 'center',
      header: t('runs.columns.datasets'),
      key: 'datasetCount',
      value: (run) => run.datasetCount,
    },
    {
      header: t('runs.columns.duration'),
      key: 'duration',
      value: (run) => run.duration,
    },
  ]
}
