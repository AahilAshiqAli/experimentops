import type { TFunction } from 'i18next'

import type { DataTableColumn } from '../../components/DataTable'
import type { Project } from '../../services/project.service'

export function getProjectColumns(t: TFunction): DataTableColumn<Project>[] {
  return [
    {
      className: 'whitespace-nowrap text-sm font-medium text-secondary',
      header: t('projects.projectColumn'),
      key: 'name',
      sort: true,
      value: (project) => project.name,
    },
    {
      className: 'min-w-72 text-sm text-slate-600',
      header: t('projects.descriptionColumn'),
      key: 'description',
      value: (project) => project.description,
    },
  ]
}
