import type { TFunction } from 'i18next'

import type { DataTableColumn } from '../../components/DataTable'
import type { DatasetVersion } from '../../services/dataset.service'
import {
  formatBytes,
  formatLabel,
} from '../ProjectDetails/projectDetails.utils'

export function getDatasetVersionColumns({
  onPreview,
  onSelect,
  pendingUuid,
  selectable = false,
  selectedVersionUuid,
  selectedVersionUuids,
  totalElements,
  t,
}: {
  onPreview: (version: DatasetVersion) => void
  onSelect?: (version: DatasetVersion) => void
  pendingUuid: string | null
  selectable?: boolean
  selectedVersionUuid?: string
  selectedVersionUuids?: string[]
  totalElements: number
  t: TFunction
}): DataTableColumn<DatasetVersion>[] {
  const columns: DataTableColumn<DatasetVersion>[] = [
    {
      cell: (version) => (
        <button
          className="text-left font-semibold text-secondary hover:text-primary hover:underline disabled:cursor-wait disabled:opacity-60"
          disabled={pendingUuid !== null}
          onClick={(event) => {
            event.stopPropagation()
            onPreview(version)
          }}
          type="button"
        >
          {pendingUuid === version.datasetVersionUuid
            ? t('datasets.openingFile', { file: version.originalFileName })
            : version.originalFileName}
        </button>
      ),
      className: 'min-w-64 text-sm',
      header: t('datasets.columns.file'),
      key: 'file',
      sort: true,
      value: (version) => version.originalFileName,
    },
    {
      cell: (_version, index) => (
        <span className="rounded bg-primary/10 px-2 py-1 text-xs font-semibold text-primary">
          v{totalElements - index}
        </span>
      ),
      header: t('datasets.columns.version'),
      key: 'version',
    },
    {
      cell: (version) => formatLabel(version.format),
      filter: true,
      header: t('datasets.columns.format'),
      key: 'format',
      sort: true,
      value: (version) => version.format,
    },
    {
      cell: (version) => formatBytes(version.size),
      className: 'whitespace-nowrap text-sm text-slate-600',
      header: t('datasets.columns.size'),
      key: 'size',
      sort: true,
      value: (version) => version.size,
    },
    {
      cell: (version) => formatLabel(version.scanStatus ?? 'NOT_STARTED'),
      className: 'whitespace-nowrap text-sm text-slate-600',
      filter: true,
      header: t('datasets.columns.scanStatus'),
      key: 'scanStatus',
      sort: true,
      value: (version) => version.scanStatus ?? 'NOT_STARTED',
    },
    {
      className: 'min-w-56 max-w-md text-sm text-slate-600',
      header: t('datasets.columns.scanMessage'),
      key: 'scanMessage',
      value: (version) => version.scanMessage,
    },
  ]

  if (!selectable) return columns

  return [
    {
      cell: (version) => {
        const isSelected = selectedVersionUuids
          ? selectedVersionUuids.includes(version.datasetVersionUuid)
          : selectedVersionUuid === version.datasetVersionUuid

        return (
          <button
            className={`rounded-md px-3 py-1.5 text-xs font-semibold transition ${
              isSelected
                ? 'bg-primary/10 text-primary'
                : 'border border-slate-300 text-secondary hover:border-primary hover:text-primary'
            }`}
            onClick={(event) => {
              event.stopPropagation()
              onSelect?.(version)
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
