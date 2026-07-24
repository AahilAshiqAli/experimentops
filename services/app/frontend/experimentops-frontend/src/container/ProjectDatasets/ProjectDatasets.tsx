import { useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import { useLogin } from '../../context-api/logincontext'
import { useDocumentTitle } from '../../hooks'
import { useQueryProject, useQueryProjectDatasets } from '../../queries'
import { PERMISSIONS_KEYS } from '../../utils'
import {
  DatasetFolderCard,
  PageSkeleton,
  PageState,
  Pagination,
  ProjectFrame,
  SectionState,
} from '../ProjectDetails/projectDetails.shared'
import { getErrorMessage } from '../ProjectDetails/projectDetails.utils'

const DATASETS_PER_PAGE = 9

export function ProjectDatasets() {
  const { t } = useTranslation()
  const { projectUuid } = useParams<{ projectUuid: string }>()
  const { hasPermission } = useLogin()
  const projectQuery = useQueryProject(projectUuid)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const datasetsQuery = useQueryProjectDatasets(projectUuid, {
    page: page - 1,
    size: DATASETS_PER_PAGE,
  })
  const canListDatasets = hasPermission(PERMISSIONS_KEYS.DATASET.GET_DATASET)
  const filteredDatasets = useMemo(() => {
    const term = search.trim().toLowerCase()
    const datasets = datasetsQuery.data?.data ?? []
    return term
      ? datasets.filter((dataset) => dataset.name.toLowerCase().includes(term))
      : datasets
  }, [datasetsQuery.data, search])
  const totalDatasets = search
    ? filteredDatasets.length
    : (datasetsQuery.data?.totalElements ?? 0)
  const totalPages = Math.max(1, Math.ceil(totalDatasets / DATASETS_PER_PAGE))
  const activePage = Math.min(page, totalPages)
  const visibleDatasets = search
    ? filteredDatasets.slice(
        (activePage - 1) * DATASETS_PER_PAGE,
        activePage * DATASETS_PER_PAGE,
      )
    : filteredDatasets

  useDocumentTitle(
    projectQuery.data
      ? t('datasets.titleWithProject', { project: projectQuery.data.name })
      : t('datasets.titleShort'),
  )

  if (projectQuery.isLoading) return <PageSkeleton />
  if (projectQuery.error) {
    return (
      <PageState
        message={getErrorMessage(
          projectQuery.error,
          t('datasets.errors.loadProject'),
        )}
        title={t('project.errors.loadTitle')}
        tone="error"
      />
    )
  }
  if (!projectQuery.data) {
    return (
      <PageState
        message={t('datasets.projectNotAvailable')}
        title={t('project.errors.notFoundTitle')}
      />
    )
  }

  return (
    <ProjectFrame activeTab="datasets" project={projectQuery.data}>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="font-heading text-2xl font-semibold text-secondary">
            {t('datasets.title')}
          </h2>
          <p className="mt-1 text-sm text-slate-600">
            {t('datasets.description')}
          </p>
        </div>
        <label className="block sm:w-72">
          <span className="sr-only">{t('datasets.searchLabel')}</span>
          <input
            className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none transition placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/20"
            onChange={(event) => {
              setSearch(event.target.value)
              setPage(1)
            }}
            placeholder={t('datasets.search')}
            type="search"
            value={search}
          />
        </label>
      </div>

      <div className="mt-4">
        {!canListDatasets ? (
          <SectionState message={t('datasets.permissionDenied')} />
        ) : datasetsQuery.isLoading ? (
          <FolderSkeleton />
        ) : datasetsQuery.error ? (
          <SectionState
            message={getErrorMessage(
              datasetsQuery.error,
              t('datasets.errors.loadFolders'),
            )}
            tone="error"
          />
        ) : visibleDatasets.length ? (
          <>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {visibleDatasets.map((dataset) => (
                <DatasetFolderCard
                  dataset={dataset}
                  key={dataset.datasetUuid}
                  projectUuid={projectQuery.data.projectUuid}
                />
              ))}
            </div>
            <div className="mt-6">
              <Pagination
                onPageChange={setPage}
                page={activePage}
                totalPages={totalPages}
              />
            </div>
          </>
        ) : (
          <SectionState
            message={
              search ? t('datasets.noSearchResults') : t('datasets.empty')
            }
          />
        )}
      </div>
    </ProjectFrame>
  )
}

function FolderSkeleton() {
  const { t } = useTranslation()
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3" role="status">
      {[1, 2, 3].map((item) => (
        <div
          className="h-40 animate-pulse rounded-xl bg-slate-100"
          key={item}
        />
      ))}
      <span className="sr-only">{t('datasets.loadingFolders')}</span>
    </div>
  )
}
