import type { TFunction } from 'i18next'

import type { DataTableColumn } from '../../components/DataTable'
import type { Experiment } from '../../services/experiment.service'
import { ExperimentActions } from '../ProjectDetails/projectDetails.shared'
import { formatDate } from '../ProjectDetails/projectDetails.utils'

export function getExperimentColumns(
  projectUuid: string,
  t: TFunction,
  locale?: string,
): DataTableColumn<Experiment>[] {
  return [
    {
      cell: (experiment) => (
        <div className="min-w-72">
          <p className="text-sm font-semibold text-secondary">
            {experiment.name}
          </p>
          <p className="mt-1 line-clamp-1 text-xs text-slate-500">
            {experiment.description || t('project.noDescription')}
          </p>
        </div>
      ),
      header: t('experiments.experiment'),
      key: 'name',
      sort: true,
      value: (experiment) => `${experiment.name} ${experiment.description}`,
    },
    {
      align: 'center',
      header: t('experiments.runs'),
      key: 'runs',
      sort: true,
      value: (experiment) => experiment.runCount,
    },
    {
      align: 'center',
      header: t('experiments.configs'),
      key: 'configs',
      sort: true,
      value: (experiment) => experiment.configCount,
    },
    {
      className: 'whitespace-nowrap text-sm text-slate-500',
      cell: (experiment) => formatDate(experiment.createdAt, locale),
      header: t('experiments.createdColumn'),
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
      header: t('experiments.actions'),
      key: 'actions',
    },
  ]
}
