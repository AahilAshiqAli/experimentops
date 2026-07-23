import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import { DataTable } from '../../components/DataTable'
import { useDocumentTitle } from '../../hooks'
import { getProjectColumns } from './columns'
import { useProjectsContainer } from './useProjectsContainer'

export function Projects() {
  const { t } = useTranslation()
  useDocumentTitle(t('projects.title'))
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
          {t('projects.title')}
        </h1>
      </div>

      <DataTable
        columns={getProjectColumns(t)}
        data={paginatedProjects}
        emptyMessage={t('projects.empty')}
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
        searchPlaceholder={t('projects.search')}
      />
    </section>
  )
}
