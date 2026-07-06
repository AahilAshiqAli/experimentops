import type { DataTableColumn } from '../../components/DataTable'
import type { Experiment } from '../../services/experiment.service'
import { ExperimentActions } from '../ProjectDetails/projectDetails.shared'
import { formatDate } from '../ProjectDetails/projectDetails.utils'

export function getExperimentColumns(
  projectUuid: string,
): DataTableColumn<Experiment>[] {
  return [
    {
      cell: (experiment) => (
        <div className="min-w-72">
          <p className="text-sm font-semibold text-secondary">
            {experiment.name}
          </p>
          <p className="mt-1 line-clamp-1 text-xs text-slate-500">
            {experiment.description || 'No description provided.'}
          </p>
        </div>
      ),
      header: 'Experiment',
      key: 'name',
      sort: true,
      value: (experiment) => `${experiment.name} ${experiment.description}`,
    },
    {
      align: 'center',
      filter: true,
      header: 'Experiment Type',
      key: 'type',
      sort: true,
      value: (experiment) => experiment.experimentType,
    },
    {
      align: 'center',
      header: 'Runs',
      key: 'runs',
      sort: true,
      value: (experiment) => experiment.runCount,
    },
    {
      align: 'center',
      header: 'Configs',
      key: 'configs',
      sort: true,
      value: (experiment) => experiment.configCount,
    },
    {
      className: 'whitespace-nowrap text-sm text-slate-500',
      cell: (experiment) => formatDate(experiment.createdAt),
      header: 'Created',
      key: 'created',
      sort: true,
      value: (experiment) => experiment.createdAt,
    },
    {
      align: 'right',
      cell: (experiment) => (
        <span className="inline-flex gap-2">
          <ExperimentActions
            experimentUuid={experiment.experimentUuid}
            projectUuid={projectUuid}
          />
        </span>
      ),
      className: 'whitespace-nowrap',
      header: 'Actions',
      key: 'actions',
    },
  ]
}
