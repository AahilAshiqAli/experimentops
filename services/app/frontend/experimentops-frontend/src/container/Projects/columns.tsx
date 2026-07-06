import type { DataTableColumn } from '../../components/DataTable'
import type { Project } from '../../services/project.service'

export const projectColumns: DataTableColumn<Project>[] = [
  {
    className: 'whitespace-nowrap text-sm font-medium text-secondary',
    header: 'Project',
    key: 'name',
    sort: true,
    value: (project) => project.name,
  },
  {
    className: 'min-w-72 text-sm text-slate-600',
    header: 'Description',
    key: 'description',
    value: (project) => project.description,
  },
]
