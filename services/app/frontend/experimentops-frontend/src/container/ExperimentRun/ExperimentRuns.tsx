import { useTranslation } from 'react-i18next'

import { DataTable } from '../../components/DataTable'
import { TableSkeleton } from '../../components/TableSkeleton'
import { useDocumentTitle } from '../../hooks'
import {
  PageSkeleton,
  PageState,
  ProjectFrame,
} from '../ProjectDetails/projectDetails.shared'
import { getErrorMessage } from '../ProjectDetails/projectDetails.utils'
import { ExperimentRunsPanel } from './ExperimentRunsPanel'
import { getExperimentRunColumns, RUN_STATUS_TRANSLATION_KEYS } from './columns'
import { useExperimentRunsContainer } from './useExperimentRunsContainer'

export function ExperimentRuns() {
  const { t } = useTranslation()
  const container = useExperimentRunsContainer()

  useDocumentTitle(t('runs.title'))

  if (container.projectQuery.isLoading) return <PageSkeleton />
  if (container.projectQuery.error) {
    return (
      <PageState
        message={getErrorMessage(
          container.projectQuery.error,
          t('datasets.errors.loadProject'),
        )}
        title={t('project.errors.loadTitle')}
        tone="error"
      />
    )
  }
  if (!container.projectQuery.data) {
    return (
      <PageState
        message={t('project.notAvailable')}
        title={t('project.errors.notFoundTitle')}
      />
    )
  }

  return (
    <ProjectFrame activeTab="experiments" project={container.projectQuery.data}>
      <ExperimentRunsPanel onAdd={container.handleAddRun}>
        {!container.canListExperimentRuns ? (
          <PageState
            message={t('runs.permissionDenied')}
            title={t('runs.unavailableTitle')}
          />
        ) : container.experimentRunsQuery.isLoading ? (
          <TableSkeleton />
        ) : container.experimentRunsQuery.error ? (
          <PageState
            message={getErrorMessage(
              container.experimentRunsQuery.error,
              t('runs.errors.load'),
            )}
            title={t('runs.errors.loadTitle')}
            tone="error"
          />
        ) : (
          <DataTable
            columns={getExperimentRunColumns(t)}
            data={container.experimentRunsQuery.data?.data ?? []}
            emptyMessage={t('runs.empty')}
            getRowKey={(run) => run.uuid}
            onRowClick={(run) => container.handleOpenRun(run.uuid)}
            pagination={{
              onPageChange: container.setPage,
              page: container.activePage,
              totalItems: container.totalRuns,
              totalPages: container.totalPages,
            }}
            searchable={false}
            toolbarStart={
              <div className="flex flex-wrap items-center gap-4">
                <label className="block min-w-56 sm:w-72">
                  <span className="sr-only">{t('runs.searchLabel')}</span>
                  <input
                    className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/20"
                    onChange={(event) =>
                      container.handleNameChange(event.target.value)
                    }
                    placeholder={t('runs.search')}
                    type="search"
                    value={container.name}
                  />
                </label>
                <fieldset className="flex flex-wrap items-center gap-x-3 gap-y-2">
                  <legend className="sr-only">
                    {t('runs.filterByStatus')}
                  </legend>
                  {container.experimentRunStatuses.map((status) => (
                    <label
                      className="inline-flex items-center gap-1.5 text-sm text-slate-700"
                      key={status}
                    >
                      <input
                        checked={container.selectedStatuses.includes(status)}
                        className="h-4 w-4 rounded border-slate-300 text-primary focus:ring-primary"
                        onChange={(event) =>
                          container.handleStatusChange(
                            status,
                            event.target.checked,
                          )
                        }
                        type="checkbox"
                      />
                      {t(RUN_STATUS_TRANSLATION_KEYS[status])}
                    </label>
                  ))}
                </fieldset>
              </div>
            }
          />
        )}
      </ExperimentRunsPanel>
    </ProjectFrame>
  )
}
