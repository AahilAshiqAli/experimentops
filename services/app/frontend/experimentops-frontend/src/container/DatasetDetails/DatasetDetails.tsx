import { type ChangeEvent, type FormEvent, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { useLogin } from '../../context-api/logincontext'
import { useDocumentTitle } from '../../hooks'
import { useQueryDataset, useQueryProject } from '../../queries'
import {
  downloadDatasetVersion,
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
  Pagination,
  ProjectFrame,
  SectionState,
} from '../ProjectDetails/projectDetails.shared'
import {
  formatBytes,
  formatLabel,
  getErrorMessage,
} from '../ProjectDetails/projectDetails.utils'

const VERSIONS_PER_PAGE = 20

export function DatasetDetails() {
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
  const [downloadingVersionUuid, setDownloadingVersionUuid] = useState<
    string | null
  >(null)
  const [downloadError, setDownloadError] = useState<string | null>(null)
  const [isAddOpen, setIsAddOpen] = useState(false)
  const [isUploading, setIsUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [uploadError, setUploadError] = useState<string | null>(null)
  const canGetDataset = hasPermission(PERMISSIONS_KEYS.DATASET.GET_DATASET)
  const canAddDataset = hasPermission(PERMISSIONS_KEYS.DATASET.ADD_DATASET)
  const totalPages = Math.max(
    1,
    Math.ceil((versionsQuery.data?.totalElements ?? 0) / VERSIONS_PER_PAGE),
  )

  useDocumentTitle('Dataset versions')

  const handleDownload = async (
    datasetVersionUuid: string,
    originalFileName: string,
  ) => {
    if (!accessToken || !datasetUuid) return

    setDownloadingVersionUuid(datasetVersionUuid)
    setDownloadError(null)
    try {
      const file = await downloadDatasetVersion(
        accessToken,
        datasetUuid,
        datasetVersionUuid,
      )
      const url = URL.createObjectURL(file)
      const link = document.createElement('a')
      link.href = url
      link.download = originalFileName
      link.click()
      URL.revokeObjectURL(url)
    } catch (error) {
      setDownloadError(
        getErrorMessage(error, 'Unable to download this dataset version.'),
      )
    } finally {
      setDownloadingVersionUuid(null)
    }
  }

  const handleAddDatasetVersion = async (file: File) => {
    if (!accessToken || !datasetUuid) return

    setIsUploading(true)
    setUploadProgress(0)
    setUploadError(null)
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
      Toaster.success('Dataset version uploaded successfully.')
      void versionsQuery.refetch()
    } catch (error) {
      const failureMessage = getErrorMessage(
        error,
        'Unable to upload this dataset version.',
      )

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
      setUploadError(failureMessage)
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
                'Unable to load this project.',
              )
            : 'This project is not available.'
        }
        title={
          projectQuery.error ? 'Unable to load project' : 'Project not found'
        }
        tone={projectQuery.error ? 'error' : 'neutral'}
      />
    )
  }

  return (
    <ProjectFrame activeTab="datasets" project={projectQuery.data}>
      {!canGetDataset ? (
        <SectionState message="You do not have permission to view this dataset." />
      ) : versionsQuery.isLoading ? (
        <div
          className="h-64 animate-pulse rounded-xl bg-slate-100"
          role="status"
        >
          <span className="sr-only">Loading dataset</span>
        </div>
      ) : versionsQuery.error ? (
        <SectionState
          message={getErrorMessage(
            versionsQuery.error,
            'Unable to load dataset versions. Please try again.',
          )}
          tone="error"
        />
      ) : versionsQuery.data ? (
        <>
          <nav aria-label="Breadcrumb" className="text-sm text-slate-500">
            <Link
              className="font-medium text-primary hover:underline"
              to={`/projects/${projectQuery.data.projectUuid}/datasets`}
            >
              Dataset folders
            </Link>{' '}
            <span aria-hidden="true">/</span> Dataset versions
          </nav>
          <div className="mt-5 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-4">
              <FolderIcon className="h-12 w-12" />
              <div>
                <h2 className="font-heading text-2xl font-semibold text-secondary">
                  {versionsQuery.data.name}
                </h2>
                <p className="mt-1 text-sm text-slate-500">
                  {versionsQuery.data.totalElements} versions
                </p>
              </div>
            </div>
            {canAddDataset ? (
              <button
                className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90"
                onClick={() => setIsAddOpen(true)}
                type="button"
              >
                + Add Dataset
              </button>
            ) : null}
          </div>

          {downloadError ? (
            <div className="mt-4">
              <SectionState message={downloadError} tone="error" />
            </div>
          ) : null}

          {versionsQuery.data.versions.length ? (
            <>
              <div className="mt-6 overflow-x-auto rounded-lg border border-slate-200">
                <table className="min-w-full divide-y divide-slate-200 text-left">
                  <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                    <tr>
                      <th className="px-5 py-3 font-semibold">File</th>
                      <th className="px-5 py-3 font-semibold">Version</th>
                      <th className="px-5 py-3 font-semibold">Format</th>
                      <th className="px-5 py-3 font-semibold">Size</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 bg-white">
                    {versionsQuery.data.versions.map((version, index) => (
                      <tr key={version.datasetVersionUuid}>
                        <td className="min-w-64 px-5 py-4 text-sm font-semibold text-secondary">
                          <button
                            className="text-left hover:text-primary hover:underline disabled:cursor-wait disabled:opacity-60"
                            disabled={downloadingVersionUuid !== null}
                            onClick={() =>
                              void handleDownload(
                                version.datasetVersionUuid,
                                version.originalFileName,
                              )
                            }
                            type="button"
                          >
                            {downloadingVersionUuid ===
                            version.datasetVersionUuid
                              ? `Downloading ${version.originalFileName}…`
                              : version.originalFileName}
                          </button>
                        </td>
                        <td className="px-5 py-4 text-sm text-slate-600">
                          <span className="rounded bg-primary/10 px-2 py-1 text-xs font-semibold text-primary">
                            v{versionsQuery.data.totalElements - index}
                          </span>
                        </td>
                        <td className="px-5 py-4 text-sm text-slate-600">
                          {formatLabel(version.format)}
                        </td>
                        <td className="whitespace-nowrap px-5 py-4 text-sm text-slate-600">
                          {formatBytes(version.size)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="mt-5">
                <Pagination
                  onPageChange={setPage}
                  page={page}
                  totalPages={totalPages}
                />
              </div>
            </>
          ) : (
            <div className="mt-6">
              <SectionState message="No versions have been uploaded to this dataset folder." />
            </div>
          )}
        </>
      ) : (
        <SectionState message="This dataset is not available." />
      )}

      {isAddOpen && (
        <AddDatasetVersionDialog
          isUploading={isUploading}
          onClose={() => {
            if (isUploading) return
            setIsAddOpen(false)
            setUploadError(null)
          }}
          onSubmit={(file) => void handleAddDatasetVersion(file)}
          uploadError={uploadError}
          uploadProgress={uploadProgress}
        />
      )}
    </ProjectFrame>
  )
}

function AddDatasetVersionDialog({
  isUploading,
  onClose,
  onSubmit,
  uploadError,
  uploadProgress,
}: {
  isUploading: boolean
  onClose: () => void
  onSubmit: (file: File) => void
  uploadError: string | null
  uploadProgress: number
}) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [fileError, setFileError] = useState<string | null>(null)

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0] ?? null

    if (file && !file.name.toLowerCase().endsWith('.csv')) {
      setFileError('Only CSV files are supported.')
      setSelectedFile(null)
      return
    }

    setFileError(null)
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
            Add Dataset
          </h2>
          <button
            aria-label="Close add dataset dialog"
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
            CSV file
            <input
              accept=".csv,text/csv"
              className="mt-1 block w-full text-sm text-slate-600 file:mr-4 file:rounded-md file:border-0 file:bg-primary/10 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-primary hover:file:bg-primary/20"
              disabled={isUploading}
              onChange={handleFileChange}
              required
              type="file"
            />
          </label>

          {fileError ? <p className="text-xs text-red-600">{fileError}</p> : null}
          {uploadError ? (
            <p className="text-xs text-red-600">{uploadError}</p>
          ) : null}

          {isUploading ? (
            <div>
              <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100">
                <div
                  className="h-full rounded-full bg-primary transition-all"
                  style={{ width: `${uploadProgress}%` }}
                />
              </div>
              <p className="mt-1 text-xs text-slate-500">
                Uploading… {uploadProgress}%
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
              Cancel
            </button>
            <button
              className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={isUploading || !selectedFile}
              type="submit"
            >
              {isUploading ? 'Uploading…' : 'Upload'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
