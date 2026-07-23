import type { TFunction } from 'i18next'

import type { DataTableColumn } from '../../components/DataTable'
import type { ExperimentConfig } from '../../services/experimentConfig.service'

export function getExperimentConfigColumns({
  onEdit,
  onSelect,
  selectable = false,
  selectedConfigUuids = [],
  t,
}: {
  onEdit?: (config: ExperimentConfig) => void
  onSelect?: (config: ExperimentConfig) => void
  selectable?: boolean
  selectedConfigUuids?: string[]
  t: TFunction
}): DataTableColumn<ExperimentConfig>[] {
  const columns: DataTableColumn<ExperimentConfig>[] = [
    {
      className: 'text-sm font-semibold text-secondary',
      header: t('configs.name'),
      key: 'name',
      sort: true,
      value: (config) => config.name,
    },
    {
      filter: true,
      header: t('configs.experimentType'),
      key: 'experimentType',
      sort: true,
      value: (config) => config.experimentType,
    },
    {
      cell: (experimentConfig) => (
        <button
          className="block max-w-md truncate rounded bg-slate-100 px-2 py-1 text-left font-mono text-xs text-slate-700 transition hover:bg-slate-200 hover:text-primary"
          onClick={(event) => {
            event.stopPropagation()
            onEdit?.(experimentConfig)
          }}
          disabled={!onEdit}
          title={JSON.stringify(experimentConfig.config)}
          type="button"
        >
          {JSON.stringify(experimentConfig.config)}
        </button>
      ),
      className: 'min-w-72 max-w-md',
      header: t('configs.config'),
      key: 'config',
      value: (config) => JSON.stringify(config.config),
    },
  ]

  if (!selectable) return columns

  return [
    {
      cell: (config) => {
        const isSelected = selectedConfigUuids.includes(config.uuid)

        return (
          <button
            className={`rounded-md px-3 py-1.5 text-xs font-semibold transition ${
              isSelected
                ? 'bg-primary/10 text-primary'
                : 'border border-slate-300 text-secondary hover:border-primary hover:text-primary'
            }`}
            onClick={(event) => {
              event.stopPropagation()
              onSelect?.(config)
            }}
            type="button"
          >
            {isSelected ? t('datasets.selected') : t('datasets.select')}
          </button>
        )
      },
      header: t('datasets.columns.select'),
      key: 'select',
    },
    ...columns,
  ]
}
