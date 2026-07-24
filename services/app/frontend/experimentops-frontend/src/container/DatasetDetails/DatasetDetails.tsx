import { type ChangeEvent, type FormEvent, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import { CsvPreview, type CsvPreviewData } from '../../components/CsvPreview'
import { DataTable } from '../../components/DataTable'
import { useLogin } from '../../context-api/logincontext'
import { useDocumentTitle } from '../../hooks'
import { useQueryDataset, useQueryProject } from '../../queries'
import {
  getDatasetVersionPreview,
  initiateDatasetVersionUpload,
  updateDatasetVersionStatus,
  uploadDatasetVersionFile,
} from '../../services/dataset.service'
import { Toaster } from '../../services/toaster.service'
import { PERMISSIONS_KEYS } from '../../utils'
import {
  FolderIcon,
  PageSkeleton,
  PageState,
  ProjectFrame,
  SectionState,
} from '../ProjectDetails/projectDetails.shared'
import { getErrorMessage } from '../ProjectDetails/projectDetails.utils'
import { getDatasetVersionColumns } from './columns'

const VERSIONS_PER_PAGE = 20

export function DatasetDetails() {
  const { t } = useTranslation()
  const { datasetUuid, projectUuid } = useParams<{
    datasetUuid: string
    projectUuid: string
  }>()
  const { accessToken, hasPermission } = useLogin()
  const projectQuery = useQueryProject(projectUuid)
  const [page, setPage] = useState(1)
  const versionsQuery = useQueryDataset(projectUuid, datasetUuid, {
    page: page - 1,
    size: VERSIONS_PER_PAGE,
  })
  const [previewingVersionUuid, setPreviewingVersionUuid] = useState<
    string | null
  >(null)
  const [preview, setPreview] = useState<{
    data: CsvPreviewData
    fileName: string
  } | null>(null)
  const [isAddOpen, setIsAddOpen] = useState(false)
  const [isUploading, setIsUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const canGetDataset = hasPermission(PERMISSIONS_KEYS.DATASET.GET_DATASET)
  const canAddDataset = hasPermission(PERMISSIONS_KEYS.DATASET.ADD_DATASET)
  const totalPages = Math.max(
    1,
    Math.ceil((versionsQuery.data?.totalElements ?? 0) / VERSIONS_PER_PAGE),
  )

  useDocumentTitle(t('datasets.breadcrumbVersions'))

  const handlePreview = async (
    datasetVersionUuid: string,
    originalFileName: string,
  ) => {
    if (!accessToken || !datasetUuid) return

    setPreviewingVersionUuid(datasetVersionUuid)
    try {
      const data = await getDatasetVersionPreview(
        accessToken,
        datasetUuid,
        datasetVersionUuid,
      )
      setPreview({ data, fileName: originalFileName })
    } catch (error) {
      Toaster.error(getErrorMessage(error, t('datasets.errors.preview')))
    } finally {
      setPreviewingVersionUuid(null)
    }
  }

  const handleAddDatasetVersion = async (file: File) => {
    if (!accessToken || !datasetUuid) return

    setIsUploading(true)
    setUploadProgress(0)
    let uploadedVersionUuid: string | null = null

    try {
      const ticket = await initiateDatasetVersionUpload(
        accessToken,
        datasetUuid,
        file.name,
      )
      uploadedVersionUuid = ticket.uuid
      await uploadDatasetVersionFile(ticket, file, setUploadProgress)
      await updateDatasetVersionStatus(
        accessToken,
        datasetUuid,
        ticket.uuid,
        'ACTIVE',
      )
      setIsAddOpen(false)
      Toaster.success(t('datasets.uploaded'))
      void versionsQuery.refetch()
    } catch (error) {
      const failureMessage = getErrorMessage(error, t('datasets.errors.upload'))

      if (uploadedVersionUuid) {
        try {
          await updateDatasetVersionStatus(
            accessToken,
            datasetUuid,
            uploadedVersionUuid,
            'FAILED',
            failureMessage,
          )
        } catch {
          // best-effort cleanup; the primary error below is what matters to the user
        }
        void versionsQuery.refetch()
      }
      Toaster.error(failureMessage)
    } finally {
      setIsUploading(false)
    }
  }

  if (projectQuery.isLoading) return <PageSkeleton />
  if (projectQuery.error || !projectQuery.data) {
    return (
      <PageState
        message={
          projectQuery.error
            ? getErrorMessage(
                projectQuery.error,
                t('datasets.errors.loadProject'),
              )
            : t('project.notAvailable')
        }
        title={
          projectQuery.error
            ? t('project.errors.loadTitle')
            : t('project.errors.notFoundTitle')
        }
        tone={projectQuery.error ? 'error' : 'neutral'}
      />
    )
  }

  return (
    <ProjectFrame activeTab="datasets" project={projectQuery.data}>
      {!canGetDataset ? (
        <SectionState message={t('datasets.permissionDeniedSingle')} />
      ) : versionsQuery.isLoading ? (
        <div
          className="h-64 animate-pulse rounded-xl bg-slate-100"
          role="status"
        >
          <span className="sr-only">{t('datasets.loadingFolders')}</span>
        </div>
      ) : versionsQuery.error ? (
        <SectionState
          message={getErrorMessage(
            versionsQuery.error,
            t('datasets.errors.loadVersions'),
          )}
          tone="error"
        />
      ) : versionsQuery.data ? (
        <>
          <nav
            aria-label={t('common.navigation.breadcrumb')}
            className="text-sm text-slate-500"
          >
            <Link
              className="font-medium text-primary hover:underline"
              to={`/projects/${projectQuery.data.projectUuid}/datasets`}
            >
              {t('datasets.title')}
            </Link>{' '}
            <span aria-hidden="true">/</span> {t('datasets.breadcrumbVersions')}
          </nav>
          <div className="mt-4 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-4">
              <FolderIcon className="h-12 w-12" />
              <div>
                <h2 className="font-heading text-2xl font-semibold text-secondary">
                  {versionsQuery.data.name}
                </h2>
                <p className="mt-1 text-sm text-slate-500">
                  {t('datasets.versions', {
                    count: versionsQuery.data.totalElements,
                  })}
                </p>
              </div>
            </div>
          </div>

          <div className="mt-4">
            <DataTable
              columns={getDatasetVersionColumns({
                onPreview: (version) =>
                  void handlePreview(
                    version.datasetVersionUuid,
                    version.originalFileName,
                  ),
                pendingUuid: previewingVersionUuid,
                totalElements: versionsQuery.data.totalElements,
                t,
              })}
              data={versionsQuery.data.versions}
              emptyMessage={t('datasets.versionsEmpty')}
              getRowKey={(version) => version.datasetVersionUuid}
              pagination={{
                onPageChange: setPage,
                page,
                totalItems: versionsQuery.data.totalElements,
                totalPages,
              }}
              searchPlaceholder={t('datasets.searchVersions')}
              toolbarEnd={
                <>
                  {canAddDataset ? (
                    <button
                      className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90"
                      onClick={() => setIsAddOpen(true)}
                      type="button"
                    >
                      + {t('datasets.add')}
                    </button>
                  ) : null}
                </>
              }
            />
          </div>
        </>
      ) : (
        <SectionState message={t('datasets.unavailable')} />
      )}

      {isAddOpen && (
        <AddDatasetVersionDialog
          isUploading={isUploading}
          onClose={() => {
            if (isUploading) return
            setIsAddOpen(false)
          }}
          onSubmit={(file) => void handleAddDatasetVersion(file)}
          uploadProgress={uploadProgress}
        />
      )}
      {preview ? (
        <DatasetPreviewDialog
          data={preview.data}
          fileName={preview.fileName}
          onClose={() => setPreview(null)}
        />
      ) : null}
    </ProjectFrame>
  )
}

function DatasetPreviewDialog({
  data,
  fileName,
  onClose,
}: {
  data: CsvPreviewData
  fileName: string
  onClose: () => void
}) {
  const { t } = useTranslation()
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  return (
    <div
      aria-labelledby="dataset-preview-title"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-secondary/50 p-4 sm:p-6"
      role="dialog"
    >
      <div className="flex max-h-[92vh] w-full max-w-7xl flex-col rounded-xl bg-white p-5 shadow-2xl sm:p-6">
        <div className="mb-5 flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h2
              className="truncate font-heading text-xl font-semibold text-secondary"
              id="dataset-preview-title"
              title={fileName}
            >
              {fileName}
            </h2>
            <p className="mt-1 text-sm text-slate-500">
              {t('datasets.previewDescription')}
            </p>
          </div>
          <button
            aria-label={t('datasets.closePreview')}
            className="rounded-md p-2 text-xl leading-none text-slate-500 hover:bg-slate-100"
            onClick={onClose}
            type="button"
          >
            ×
          </button>
        </div>
        <CsvPreview data={data} />
      </div>
    </div>
  )
}

function AddDatasetVersionDialog({
  isUploading,
  onClose,
  onSubmit,
  uploadProgress,
}: {
  isUploading: boolean
  onClose: () => void
  onSubmit: (file: File) => void
  uploadProgress: number
}) {
  const { t } = useTranslation()
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0] ?? null

    if (file && !file.name.toLowerCase().endsWith('.csv')) {
      Toaster.error(t('datasets.errors.csvOnly'))
      setSelectedFile(null)
      return
    }

    setSelectedFile(file)
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selectedFile) return

    onSubmit(selectedFile)
  }

  return (
    <div
      aria-labelledby="add-dataset-version-title"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-secondary/40 p-4"
      role="dialog"
    >
      <div className="w-full max-w-lg rounded-xl bg-white p-6 shadow-2xl">
        <div className="flex items-center justify-between gap-4">
          <h2
            className="font-heading text-2xl font-semibold text-secondary"
            id="add-dataset-version-title"
          >
            {t('datasets.add')}
          </h2>
          <button
            aria-label={t('datasets.closeAddDialog')}
            className="rounded-md p-2 text-slate-500 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={isUploading}
            onClick={onClose}
            type="button"
          >
            ×
          </button>
        </div>

        <form className="mt-5 space-y-4" onSubmit={handleSubmit}>
          <label className="block text-sm font-medium text-secondary">
            {t('datasets.csvFile')}
            <input
              accept=".csv,text/csv"
              className="mt-1 block w-full text-sm text-slate-600 file:mr-4 file:rounded-md file:border-0 file:bg-primary/10 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-primary hover:file:bg-primary/20"
              disabled={isUploading}
              onChange={handleFileChange}
              required
              type="file"
            />
          </label>

          {isUploading ? (
            <div>
              <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100">
                <div
                  className="h-full rounded-full bg-primary transition-all"
                  style={{ width: `${uploadProgress}%` }}
                />
              </div>
              <p className="mt-1 text-xs text-slate-500">
                {t('datasets.uploadProgress', { progress: uploadProgress })}
              </p>
            </div>
          ) : null}

          <div className="flex justify-end gap-3 pt-2">
            <button
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-semibold text-secondary hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={isUploading}
              onClick={onClose}
              type="button"
            >
              {t('common.actions.cancel')}
            </button>
            <button
              className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={isUploading || !selectedFile}
              type="submit"
            >
              {isUploading ? t('datasets.uploading') : t('datasets.upload')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
