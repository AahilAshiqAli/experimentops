import { useState } from 'react'

import { useQueryProjects } from '../../queries'
import { ApiServiceError } from '../../utils/api.service'

const PROJECTS_PER_PAGE = 6

export function useProjectsContainer() {
  const [page, setPage] = useState(1)
  const { data: projectsPage, error, isLoading } = useQueryProjects({
    page: page - 1,
    size: PROJECTS_PER_PAGE,
  })
  const projects = projectsPage?.data ?? []
  const totalProjects = projectsPage?.totalElements ?? 0
  const totalPages = Math.max(1, Math.ceil(totalProjects / PROJECTS_PER_PAGE))
  const activePage = Math.min(page, totalPages)

  const firstProjectNumber =
    totalProjects === 0 ? 0 : (activePage - 1) * PROJECTS_PER_PAGE + 1
  const lastProjectNumber = Math.min(
    activePage * PROJECTS_PER_PAGE,
    totalProjects,
  )

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
    paginatedProjects: projects,
    setPage,
    totalPages,
    totalProjects,
  }
}
