import { DataTable } from '../../components/DataTable'
import { TableSkeleton } from '../../components/TableSkeleton'
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
  onSelect,
  projectUuid,
  selectedVersionUuid,
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
  onSelect: (dataset: Dataset, version: DatasetVersion) => void
  projectUuid: string
  selectedVersionUuid?: string
  totalDatasetVersions: number
}) {
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
              Find Dataset
            </h2>
            <p className="mt-1 text-sm text-slate-600">
              Open a dataset folder, then select a completed dataset version for
              this run.
            </p>
          </div>
          <button
            aria-label="Close dataset picker"
            className="rounded-md p-2 text-slate-500 hover:bg-slate-100"
            onClick={onClose}
            type="button"
          >
            ×
          </button>
        </div>

        <div className="mt-6 overflow-y-auto">
          {openedDataset ? (
            <>
              <button
                className="text-sm font-semibold text-primary hover:underline"
                onClick={() => onOpenDataset(null)}
                type="button"
              >
                ← Dataset folders
              </button>
              <div className="mt-5 flex items-center gap-4">
                <FolderIcon className="h-12 w-12" />
                <div>
                  <h3 className="font-heading text-2xl font-semibold text-secondary">
                    {openedDataset.name}
                  </h3>
                  <p className="mt-1 text-sm text-slate-500">
                    Select a completed dataset version.
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
                      'Unable to load dataset versions. Please try again.',
                    )}
                    tone="error"
                  />
                ) : (
                  <DataTable
                    columns={getDatasetVersionColumns({
                      onPreview: (version) => onSelect(openedDataset, version),
                      onSelect: (version) => onSelect(openedDataset, version),
                      pendingUuid: null,
                      selectable: true,
                      selectedVersionUuid,
                      totalElements: totalDatasetVersions,
                    })}
                    data={datasetVersions}
                    emptyMessage="No completed versions are available in this dataset folder."
                    getRowKey={(version) => version.datasetVersionUuid}
                    onRowClick={(version) => onSelect(openedDataset, version)}
                    searchPlaceholder="Search completed dataset versions..."
                  />
                )}
              </div>
            </>
          ) : (
            <>
              <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                <div>
                  <h3 className="font-heading text-xl font-semibold text-secondary">
                    Dataset folders
                  </h3>
                  <p className="mt-1 text-sm text-slate-600">
                    Open a folder to inspect its completed dataset versions.
                  </p>
                </div>
                <label className="block sm:w-72">
                  <span className="sr-only">Search dataset folders</span>
                  <input
                    className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none transition placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/20"
                    onChange={(event) =>
                      onDatasetPickerSearchChange(event.target.value)
                    }
                    placeholder="Search dataset folders..."
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
                    <span className="sr-only">Loading dataset folders</span>
                  </div>
                ) : datasetsError ? (
                  <SectionState
                    message={getErrorMessage(
                      datasetsError,
                      'Unable to load dataset folders. Please try again.',
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
                        ? 'No dataset folders match your search.'
                        : 'No dataset folders have been created yet.'
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
