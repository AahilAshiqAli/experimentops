import type { DataTableColumn } from '../../components/DataTable'
import type { ExperimentConfig } from '../../services/experimentConfig.service'

export function getExperimentConfigColumns(
  onEdit: (config: ExperimentConfig) => void,
): DataTableColumn<ExperimentConfig>[] {
  return [
    {
      className: 'text-sm font-semibold text-secondary',
      header: 'Name',
      key: 'name',
      sort: true,
      value: (config) => config.name,
    },
    {
      cell: (experimentConfig) => (
        <button
          className="block max-w-md truncate rounded bg-slate-100 px-2 py-1 text-left font-mono text-xs text-slate-700 transition hover:bg-slate-200 hover:text-primary"
          onClick={() => onEdit(experimentConfig)}
          title={JSON.stringify(experimentConfig.config)}
          type="button"
        >
          {JSON.stringify(experimentConfig.config)}
        </button>
      ),
      className: 'min-w-72 max-w-md',
      header: 'Config',
      key: 'config',
      value: (config) => JSON.stringify(config.config),
    },
  ]
}
