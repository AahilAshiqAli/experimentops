import { DataTable } from '../../components/DataTable'
import { TableSkeleton } from '../../components/TableSkeleton'
import type { ExperimentConfig } from '../../services/experimentConfig.service'
import { getExperimentConfigColumns } from '../ExperimentConfigs/columns'
import { SectionState } from '../ProjectDetails/projectDetails.shared'
import { getErrorMessage } from '../ProjectDetails/projectDetails.utils'

export function ExperimentConfigPickerDialog({
  configs,
  error,
  experimentType,
  isLoading,
  isSelecting,
  onClose,
  onSelect,
  selectedConfigUuids,
}: {
  configs: ExperimentConfig[]
  error: unknown
  experimentType: string
  isLoading: boolean
  isSelecting: boolean
  onClose: () => void
  onSelect: (config: ExperimentConfig) => void
  selectedConfigUuids: string[]
}) {
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
          {isLoading ? (
            <TableSkeleton />
          ) : error ? (
            <SectionState
              message={getErrorMessage(
                error,
                'Unable to load experiment configs. Please try again.',
              )}
              tone="error"
            />
          ) : (
            <div
              aria-busy={isSelecting}
              className={isSelecting ? 'pointer-events-none opacity-60' : ''}
            >
              {isSelecting ? (
                <p className="mb-3 rounded-md bg-sky-50 px-3 py-2 text-sm text-sky-700">
                  Loading config inputs and outputs…
                </p>
              ) : null}
              <DataTable
                columns={getExperimentConfigColumns({
                  onSelect,
                  selectable: true,
                  selectedConfigUuids,
                })}
                data={configs}
                emptyMessage="No configs were found for this experiment type."
                getRowKey={(config) => config.uuid}
                onRowClick={onSelect}
                searchPlaceholder="Search returned configs..."
              />
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
