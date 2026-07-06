import { useNavigate } from 'react-router-dom'

import { DataTable } from '../../components/DataTable'
import { useDocumentTitle } from '../../hooks'
import { projectColumns } from './columns'
import { useProjectsContainer } from './useProjectsContainer'

export function Projects() {
  useDocumentTitle('Projects')
  const navigate = useNavigate()

  const {
    errorMessage,
    isLoading,
    page,
    paginatedProjects,
    setPage,
    totalPages,
    totalProjects,
  } = useProjectsContainer()

  return (
    <section className="rounded-2xl border border-slate-200 bg-surface shadow-card">
      <div className="flex flex-col gap-2 border-b border-slate-200 px-6 py-5 sm:px-8">
        <h1 className="font-heading text-3xl font-semibold text-secondary">
          Projects
        </h1>
        <p className="text-slate-600">Projects available in this workspace.</p>
      </div>

      <div className="p-6 sm:p-8">
        {!isLoading && errorMessage && (
          <div
            className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700"
            role="alert"
          >
            {errorMessage}
          </div>
        )}

        {!errorMessage ? (
          <DataTable
            columns={projectColumns}
            data={paginatedProjects}
            emptyMessage="Projects created for this workspace will appear here."
            getRowKey={(project) => project.uuid}
            isLoading={isLoading}
            onRowClick={(project) =>
              navigate(`/projects/${project.uuid}`, { state: { project } })
            }
            pagination={{
              onPageChange: setPage,
              page,
              totalItems: totalProjects,
              totalPages,
            }}
            searchPlaceholder="Search projects..."
          />
        ) : null}
      </div>
    </section>
  )
}
