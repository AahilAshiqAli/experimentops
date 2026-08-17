import type { TFunction } from 'i18next'

import type { DataTableColumn } from '../../components/DataTable'
import type { ExperimentConfig } from '../../services/experimentConfig.service'

function getPortNames(config: ExperimentConfig) {
  return {
    inputs: [
      ...new Set(
        config.formatMappings.flatMap((manifest) =>
          manifest.inputs.map((input) => input.portName),
        ),
      ),
    ],
    outputs: [
      ...new Set(
        config.formatMappings.flatMap((manifest) =>
          manifest.outputs.map((output) => output.name),
        ),
      ),
    ],
  }
}

function renderPortNames(
  label: string,
  names: string[],
  tone: 'input' | 'output',
) {
  return (
    <span className="flex min-w-0 items-center gap-2">
      <span
        className={`w-8 shrink-0 rounded px-1.5 py-0.5 text-center text-[10px] font-bold uppercase tracking-wide ${
          tone === 'input'
            ? 'bg-sky-100 text-sky-700'
            : 'bg-emerald-100 text-emerald-700'
        }`}
      >
        {label}
      </span>
      <span className="truncate font-mono text-xs text-slate-600">
        {names.length ? names.join(', ') : '—'}
      </span>
    </span>
  )
}

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
    ...(onEdit
      ? [
          {
            cell: (experimentConfig: ExperimentConfig) => {
              const ports = getPortNames(experimentConfig)
              const title = [
                `${t('configs.io.inputs')}: ${ports.inputs.join(', ') || '—'}`,
                `${t('configs.io.outputs')}: ${ports.outputs.join(', ') || '—'}`,
              ].join('\n')

              return (
                <button
                  aria-label={t('configs.io.openContract', {
                    name: experimentConfig.name,
                  })}
                  className="block w-full max-w-xs space-y-1.5 rounded-md border border-transparent p-1.5 text-left transition hover:border-primary/20 hover:bg-primary/5"
                  onClick={(event) => {
                    event.stopPropagation()
                    onEdit(experimentConfig)
                  }}
                  title={title}
                  type="button"
                >
                  {renderPortNames(
                    t('configs.io.inputShort'),
                    ports.inputs,
                    'input',
                  )}
                  {renderPortNames(
                    t('configs.io.outputShort'),
                    ports.outputs,
                    'output',
                  )}
                </button>
              )
            },
            className: 'min-w-72 max-w-xs',
            header: t('configs.io.title'),
            key: 'ioContract',
            value: (config: ExperimentConfig) => {
              const ports = getPortNames(config)
              return [...ports.inputs, ...ports.outputs].join(' ')
            },
          } satisfies DataTableColumn<ExperimentConfig>,
        ]
      : []),
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
