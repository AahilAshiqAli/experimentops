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
import { getExperimentRunColumns } from './columns'
import { useExperimentRunsContainer } from './useExperimentRunsContainer'

export function ExperimentRuns() {
  const container = useExperimentRunsContainer()

  useDocumentTitle('Experiment runs')

  if (container.projectQuery.isLoading) return <PageSkeleton />
  if (container.projectQuery.error) {
    return (
      <PageState
        message={getErrorMessage(
          container.projectQuery.error,
          'Unable to load this project.',
        )}
        title="Unable to load project"
        tone="error"
      />
    )
  }
  if (!container.projectQuery.data) {
    return (
      <PageState
        message="This project is not available."
        title="Project not found"
      />
    )
  }

  return (
    <ProjectFrame activeTab="experiments" project={container.projectQuery.data}>
      <ExperimentRunsPanel onAdd={container.handleAddRun}>
        {!container.canListExperimentRuns ? (
          <PageState
            message="You do not have permission to view experiment runs."
            title="Experiment runs unavailable"
          />
        ) : container.experimentRunsQuery.isLoading ? (
          <TableSkeleton />
        ) : container.experimentRunsQuery.error ? (
          <PageState
            message={getErrorMessage(
              container.experimentRunsQuery.error,
              'Unable to load experiment runs.',
            )}
            title="Unable to load experiment runs"
            tone="error"
          />
        ) : (
          <DataTable
            columns={getExperimentRunColumns()}
            data={container.experimentRunsQuery.data?.data ?? []}
            emptyMessage="No experiment runs match the current filters."
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
                  <span className="sr-only">Search experiment runs</span>
                  <input
                    className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/20"
                    onChange={(event) =>
                      container.handleNameChange(event.target.value)
                    }
                    placeholder="Search runs by name..."
                    type="search"
                    value={container.name}
                  />
                </label>
                <fieldset className="flex flex-wrap items-center gap-x-3 gap-y-2">
                  <legend className="sr-only">
                    Filter experiment runs by status
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
                      {status}
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
