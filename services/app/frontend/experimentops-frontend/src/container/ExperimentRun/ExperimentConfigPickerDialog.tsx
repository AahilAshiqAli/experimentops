import { DataTable } from '../../components/DataTable'
import { TableSkeleton } from '../../components/TableSkeleton'
import { useTranslation } from 'react-i18next'
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
  const { t } = useTranslation()
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
              {t('runs.picker.configsTitle')}
            </h2>
            <p className="mt-1 text-sm text-slate-600">
              {t('runs.picker.configDescription', { experimentType })}
            </p>
          </div>
          <button
            aria-label={t('runs.picker.closeConfig')}
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
              message={getErrorMessage(error, t('configs.errors.load'))}
              tone="error"
            />
          ) : (
            <div
              aria-busy={isSelecting}
              className={isSelecting ? 'pointer-events-none opacity-60' : ''}
            >
              {isSelecting ? (
                <p className="mb-3 rounded-md bg-sky-50 px-3 py-2 text-sm text-sky-700">
                  {t('runs.picker.loadingConfigDetails')}
                </p>
              ) : null}
              <DataTable
                columns={getExperimentConfigColumns({
                  onSelect,
                  selectable: true,
                  selectedConfigUuids,
                  t,
                })}
                data={configs}
                emptyMessage={t('runs.picker.configsEmpty')}
                getRowKey={(config) => config.uuid}
                onRowClick={onSelect}
                searchPlaceholder={t('runs.picker.searchConfigs')}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
