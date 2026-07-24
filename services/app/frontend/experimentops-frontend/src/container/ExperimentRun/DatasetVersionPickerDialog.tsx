import { DataTable } from '../../components/DataTable'
import { TableSkeleton } from '../../components/TableSkeleton'
import { useTranslation } from 'react-i18next'
import type { Dataset, DatasetVersion } from '../../services/dataset.service'
import { getDatasetVersionColumns } from '../DatasetDetails/columns'
import {
  DatasetFolderCard,
  FolderIcon,
  Pagination,
  SectionState,
} from '../ProjectDetails/projectDetails.shared'
import { getErrorMessage } from '../ProjectDetails/projectDetails.utils'

export function DatasetVersionPickerDialog({
  datasetPickerPage,
  datasetPickerSearch,
  datasetPickerTotalPages,
  datasetVersions,
  datasetVersionsError,
  isDatasetVersionsLoading,
  isDatasetsLoading,
  datasetsError,
  filteredDatasets,
  openedDataset,
  onClose,
  onDatasetPickerPageChange,
  onDatasetPickerSearchChange,
  onOpenDataset,
  onToggle,
  projectUuid,
  selectedVersionUuids,
  totalDatasetVersions,
}: {
  datasetPickerPage: number
  datasetPickerSearch: string
  datasetPickerTotalPages: number
  datasetVersions: DatasetVersion[]
  datasetVersionsError: unknown
  isDatasetVersionsLoading: boolean
  isDatasetsLoading: boolean
  datasetsError: unknown
  filteredDatasets: Dataset[]
  openedDataset: Dataset | null
  onClose: () => void
  onDatasetPickerPageChange: (page: number) => void
  onDatasetPickerSearchChange: (search: string) => void
  onOpenDataset: (dataset: Dataset | null) => void
  onToggle: (dataset: Dataset, version: DatasetVersion) => void
  projectUuid: string
  selectedVersionUuids: string[]
  totalDatasetVersions: number
}) {
  const { t } = useTranslation()
  return (
    <div
      aria-labelledby="dataset-picker-title"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-secondary/40 p-4"
      role="dialog"
    >
      <div className="flex max-h-[92vh] w-full max-w-6xl flex-col rounded-xl bg-white p-6 shadow-2xl">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2
              className="font-heading text-2xl font-semibold text-secondary"
              id="dataset-picker-title"
            >
              {t('runs.picker.title')}
            </h2>
            <p className="mt-1 text-sm text-slate-600">
              {t('runs.picker.datasetDescription')}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <span className="rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
              {t('runs.picker.selected', {
                count: selectedVersionUuids.length,
              })}
            </span>
            <button
              className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white hover:bg-primary/90"
              onClick={onClose}
              type="button"
            >
              {t('runs.picker.done')}
            </button>
            <button
              aria-label={t('runs.picker.closeDataset')}
              className="rounded-md p-2 text-slate-500 hover:bg-slate-100"
              onClick={onClose}
              type="button"
            >
              ×
            </button>
          </div>
        </div>

        <div className="mt-6 overflow-y-auto">
          {openedDataset ? (
            <>
              <button
                className="text-sm font-semibold text-primary hover:underline"
                onClick={() => onOpenDataset(null)}
                type="button"
              >
                ← {t('datasets.title')}
              </button>
              <div className="mt-5 flex items-center gap-4">
                <FolderIcon className="h-12 w-12" />
                <div>
                  <h3 className="font-heading text-2xl font-semibold text-secondary">
                    {openedDataset.name}
                  </h3>
                  <p className="mt-1 text-sm text-slate-500">
                    {t('runs.picker.completedDescription')}
                  </p>
                </div>
              </div>

              <div className="mt-6">
                {isDatasetVersionsLoading ? (
                  <TableSkeleton />
                ) : datasetVersionsError ? (
                  <SectionState
                    message={getErrorMessage(
                      datasetVersionsError,
                      t('datasets.errors.loadVersions'),
                    )}
                    tone="error"
                  />
                ) : (
                  <DataTable
                    columns={getDatasetVersionColumns({
                      onPreview: (version) => onToggle(openedDataset, version),
                      onSelect: (version) => onToggle(openedDataset, version),
                      pendingUuid: null,
                      selectable: true,
                      selectedVersionUuids,
                      totalElements: totalDatasetVersions,
                      t,
                    })}
                    data={datasetVersions}
                    emptyMessage={t('runs.picker.datasetsEmpty')}
                    getRowKey={(version) => version.datasetVersionUuid}
                    onRowClick={(version) => onToggle(openedDataset, version)}
                    searchPlaceholder={t('runs.picker.searchVersions')}
                  />
                )}
              </div>
            </>
          ) : (
            <>
              <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                <div>
                  <h3 className="font-heading text-xl font-semibold text-secondary">
                    {t('datasets.title')}
                  </h3>
                  <p className="mt-1 text-sm text-slate-600">
                    {t('runs.picker.datasetsDescription')}
                  </p>
                </div>
                <label className="block sm:w-72">
                  <span className="sr-only">{t('datasets.searchLabel')}</span>
                  <input
                    className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none transition placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/20"
                    onChange={(event) =>
                      onDatasetPickerSearchChange(event.target.value)
                    }
                    placeholder={t('datasets.search')}
                    type="search"
                    value={datasetPickerSearch}
                  />
                </label>
              </div>

              <div className="mt-6">
                {isDatasetsLoading ? (
                  <div
                    className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3"
                    role="status"
                  >
                    {[1, 2, 3].map((item) => (
                      <div
                        className="h-40 animate-pulse rounded-xl bg-slate-100"
                        key={item}
                      />
                    ))}
                    <span className="sr-only">
                      {t('datasets.loadingFolders')}
                    </span>
                  </div>
                ) : datasetsError ? (
                  <SectionState
                    message={getErrorMessage(
                      datasetsError,
                      t('datasets.errors.loadFolders'),
                    )}
                    tone="error"
                  />
                ) : filteredDatasets.length ? (
                  <>
                    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                      {filteredDatasets.map((dataset) => (
                        <DatasetFolderCard
                          dataset={dataset}
                          key={dataset.datasetUuid}
                          onOpen={onOpenDataset}
                          projectUuid={projectUuid}
                        />
                      ))}
                    </div>
                    <div className="mt-6">
                      <Pagination
                        onPageChange={onDatasetPickerPageChange}
                        page={Math.min(
                          datasetPickerPage,
                          datasetPickerTotalPages,
                        )}
                        totalPages={datasetPickerTotalPages}
                      />
                    </div>
                  </>
                ) : (
                  <SectionState
                    message={
                      datasetPickerSearch
                        ? t('datasets.noSearchResults')
                        : t('datasets.empty')
                    }
                  />
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
