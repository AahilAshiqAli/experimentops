import { Link, useParams } from 'react-router-dom'

import { useLogin } from '../../context-api/logincontext'
import { useDocumentTitle } from '../../hooks'
import { useQueryDataset, useQueryProject } from '../../queries'
import { PERMISSIONS_KEYS } from '../../utils'
import {
  FolderIcon,
  PageSkeleton,
  PageState,
  ProjectFrame,
  SectionState,
} from '../ProjectDetails/projectDetails.shared'
import {
  formatBytes,
  formatDate,
  formatLabel,
  getErrorMessage,
} from '../ProjectDetails/projectDetails.utils'

export function DatasetDetails() {
  const { datasetUuid, projectUuid } = useParams<{
    datasetUuid: string
    projectUuid: string
  }>()
  const { hasPermission } = useLogin()
  const projectQuery = useQueryProject(projectUuid)
  const datasetQuery = useQueryDataset(datasetUuid)
  const canGetDataset = hasPermission(PERMISSIONS_KEYS.DATASET.GET_DATASET)

  useDocumentTitle(datasetQuery.data?.name ?? 'Dataset folder')

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
      ) : datasetQuery.isLoading ? (
        <div
          className="h-64 animate-pulse rounded-xl bg-slate-100"
          role="status"
        >
          <span className="sr-only">Loading dataset</span>
        </div>
      ) : datasetQuery.error ? (
        <SectionState
          message={getErrorMessage(
            datasetQuery.error,
            'Unable to load this dataset. Please try again.',
          )}
          tone="error"
        />
      ) : datasetQuery.data ? (
        <>
          <nav aria-label="Breadcrumb" className="text-sm text-slate-500">
            <Link
              className="font-medium text-primary hover:underline"
              to={`/projects/${projectQuery.data.projectUuid}/datasets`}
            >
              Dataset folders
            </Link>{' '}
            <span aria-hidden="true">/</span> {datasetQuery.data.name}
          </nav>
          <div className="mt-5 flex items-center gap-4">
            <FolderIcon className="h-12 w-12" />
            <div>
              <h2 className="font-heading text-2xl font-semibold text-secondary">
                {datasetQuery.data.name}
              </h2>
              <p className="mt-1 text-sm text-slate-500">
                {datasetQuery.data.versions.length} versions
              </p>
            </div>
          </div>

          {datasetQuery.data.versions.length ? (
            <div className="mt-6 overflow-x-auto rounded-lg border border-slate-200">
              <table className="min-w-full divide-y divide-slate-200 text-left">
                <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="px-5 py-3 font-semibold">File</th>
                    <th className="px-5 py-3 font-semibold">Version</th>
                    <th className="px-5 py-3 font-semibold">Format</th>
                    <th className="px-5 py-3 font-semibold">Size</th>
                    <th className="px-5 py-3 font-semibold">Updated</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 bg-white">
                  {datasetQuery.data.versions.map((version, index) => (
                    <tr key={version.datasetVersionUuid}>
                      <td className="min-w-64 px-5 py-4 text-sm font-semibold text-secondary">
                        {version.originalFileName}
                      </td>
                      <td className="px-5 py-4 text-sm text-slate-600">
                        <span className="rounded bg-primary/10 px-2 py-1 text-xs font-semibold text-primary">
                          v{datasetQuery.data.versions.length - index}
                        </span>
                      </td>
                      <td className="px-5 py-4 text-sm text-slate-600">
                        {formatLabel(version.format)}
                      </td>
                      <td className="whitespace-nowrap px-5 py-4 text-sm text-slate-600">
                        {formatBytes(version.size)}
                      </td>
                      <td className="whitespace-nowrap px-5 py-4 text-sm text-slate-500">
                        {formatDate(version.updatedAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="mt-6">
              <SectionState message="No versions have been uploaded to this dataset folder." />
            </div>
          )}
        </>
      ) : (
        <SectionState message="This dataset is not available." />
      )}
    </ProjectFrame>
  )
}
