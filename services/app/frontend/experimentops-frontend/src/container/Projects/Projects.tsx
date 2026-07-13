import { useNavigate } from 'react-router-dom'

import { DataTable } from '../../components/DataTable'
import { useDocumentTitle } from '../../hooks'
import { projectColumns } from './columns'
import { useProjectsContainer } from './useProjectsContainer'

export function Projects() {
  useDocumentTitle('Projects')
  const navigate = useNavigate()

  const {
    isLoading,
    page,
    paginatedProjects,
    setPage,
    totalPages,
    totalProjects,
  } = useProjectsContainer()

  return (
    <section>
      <div className="mb-3 border-b border-slate-200 pb-3">
        <h1 className="font-heading text-2xl font-semibold text-secondary">
          Projects
        </h1>
      </div>

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
    </section>
  )
}
