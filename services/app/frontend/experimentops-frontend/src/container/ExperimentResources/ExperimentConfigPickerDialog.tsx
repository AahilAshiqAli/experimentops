import { DataTable } from '../../components/DataTable'
import { useQueryExperimentConfigs } from '../../queries'
import type { ExperimentConfig } from '../../services/experimentConfig.service'
import { getErrorMessage } from '../ProjectDetails/projectDetails.utils'
import { getExperimentConfigColumns } from './columns'
import { SectionState } from '../ProjectDetails/projectDetails.shared'
import { TableSkeleton } from './TableSkeleton'

export function ExperimentConfigPickerDialog({
  experimentType,
  experimentUuid,
  onClose,
  onSelect,
  selectedConfigUuids,
}: {
  experimentType: string
  experimentUuid: string
  onClose: () => void
  onSelect: (config: ExperimentConfig) => void
  selectedConfigUuids: string[]
}) {
  const configsQuery = useQueryExperimentConfigs(
    experimentUuid,
    {
      experimentType,
      page: 0,
      size: 100,
    },
    { enabled: Boolean(experimentType) },
  )

  return (
    <div
      aria-labelledby="config-picker-title"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-secondary/40 p-4"
      role="dialog"
    >
      <div className="flex max-h-[92vh] w-full max-w-6xl flex-col rounded-xl bg-white p-6 shadow-2xl">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2
              className="font-heading text-2xl font-semibold text-secondary"
              id="config-picker-title"
            >
              Experiment Configs
            </h2>
            <p className="mt-1 text-sm text-slate-600">
              Select a config for {experimentType}.
            </p>
          </div>
          <button
            aria-label="Close experiment config picker"
            className="rounded-md p-2 text-slate-500 hover:bg-slate-100"
            onClick={onClose}
            type="button"
          >
            ×
          </button>
        </div>

        <div className="mt-6 overflow-y-auto">
          {configsQuery.isLoading ? (
            <TableSkeleton />
          ) : configsQuery.error ? (
            <SectionState
              message={getErrorMessage(
                configsQuery.error,
                'Unable to load experiment configs. Please try again.',
              )}
              tone="error"
            />
          ) : (
            <DataTable
              columns={getExperimentConfigColumns({
                onSelect,
                selectable: true,
                selectedConfigUuids,
              })}
              data={configsQuery.data?.data ?? []}
              emptyMessage="No configs were found for this experiment type."
              getRowKey={(config) => config.uuid}
              onRowClick={onSelect}
              searchPlaceholder="Search returned configs..."
            />
          )}
        </div>
      </div>
    </div>
  )
}
