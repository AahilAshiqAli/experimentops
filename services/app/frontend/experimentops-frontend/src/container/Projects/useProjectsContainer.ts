import { useMemo, useState } from 'react'

import { useQueryProjects } from '../../queries'
import { ApiServiceError } from '../../utils/api.service'

const PROJECTS_PER_PAGE = 6

export function useProjectsContainer() {
  const [page, setPage] = useState(1)
  const { data: projects = [], error, isLoading } = useQueryProjects()
  const totalPages = Math.max(1, Math.ceil(projects.length / PROJECTS_PER_PAGE))
  const activePage = Math.min(page, totalPages)

  const paginatedProjects = useMemo(() => {
    const startIndex = (activePage - 1) * PROJECTS_PER_PAGE
    return projects.slice(startIndex, startIndex + PROJECTS_PER_PAGE)
  }, [activePage, projects])

  const firstProjectNumber =
    projects.length === 0 ? 0 : (activePage - 1) * PROJECTS_PER_PAGE + 1
  const lastProjectNumber = Math.min(activePage * PROJECTS_PER_PAGE, projects.length)

  return {
    errorMessage:
      error instanceof ApiServiceError
        ? error.message
        : error
          ? 'Unable to load projects. Please try again.'
          : null,
    firstProjectNumber,
    isLoading,
    lastProjectNumber,
    page: activePage,
    paginatedProjects,
    setPage,
    totalPages,
    totalProjects: projects.length,
  }
}
