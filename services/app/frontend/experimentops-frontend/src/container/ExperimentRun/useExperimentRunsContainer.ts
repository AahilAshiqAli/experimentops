import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'

import { useLogin } from '../../context-api/logincontext'
import { useDebouncedValue } from '../../hooks'
import { useQueryExperimentRuns, useQueryProject } from '../../queries'
import {
  EXPERIMENT_RUN_STATUSES,
  type ExperimentRunStatus,
} from '../../services/experimentRun.service'
import { trackExperimentRun } from '../../services/experimentRunTracking.service'
import { PERMISSIONS_KEYS } from '../../utils'

const RUNS_PER_PAGE = 20
const ACTIVE_EXPERIMENT_RUN_STATUSES: ExperimentRunStatus[] = [
  'PENDING',
  'RUNNING',
]

export function useExperimentRunsContainer() {
  const { experimentUuid, projectUuid } = useParams<{
    experimentUuid: string
    projectUuid: string
  }>()
  const navigate = useNavigate()
  const { hasPermission } = useLogin()
  const projectQuery = useQueryProject(projectUuid)
  const [page, setPage] = useState(1)
  const [name, setName] = useState('')
  const [selectedStatuses, setSelectedStatuses] = useState<
    ExperimentRunStatus[]
  >(ACTIVE_EXPERIMENT_RUN_STATUSES)
  const debouncedName = useDebouncedValue(name.trim())
  const experimentRunQueryParams = useMemo(
    () => ({
      name: debouncedName || undefined,
      page: page - 1,
      size: RUNS_PER_PAGE,
      status: selectedStatuses,
    }),
    [debouncedName, page, selectedStatuses],
  )
  const experimentRunsQuery = useQueryExperimentRuns(
    experimentUuid,
    experimentRunQueryParams,
  )
  const canListExperimentRuns = hasPermission(
    PERMISSIONS_KEYS.EXPERIMENT_RUN.GET_EXPERIMENT_RUNS,
  )
  const totalRuns = experimentRunsQuery.data?.totalElements ?? 0
  const totalPages = Math.max(1, Math.ceil(totalRuns / RUNS_PER_PAGE))
  const activePage = Math.min(page, totalPages)

  useEffect(() => {
    experimentRunsQuery.data?.data.forEach((run) => {
      if (ACTIVE_EXPERIMENT_RUN_STATUSES.includes(run.status)) {
        trackExperimentRun(run.uuid)
      }
    })
  }, [experimentRunsQuery.data])

  const handleStatusChange = (
    status: ExperimentRunStatus,
    isSelected: boolean,
  ) => {
    setPage(1)
    setSelectedStatuses((current) =>
      isSelected
        ? [...current, status]
        : current.filter((currentStatus) => currentStatus !== status),
    )
  }

  const handleNameChange = (nextName: string) => {
    setPage(1)
    setName(nextName)
  }

  const handleAddRun = () => {
    navigate(
      `/projects/${projectUuid}/experiments/${experimentUuid}/experiment-runs/new`,
    )
  }

  const handleOpenRun = (experimentRunUuid: string) => {
    navigate(
      `/projects/${projectUuid}/experiments/${experimentUuid}/experiment-runs/${experimentRunUuid}`,
    )
  }

  return {
    activePage,
    canListExperimentRuns,
    experimentRunQueryParams,
    experimentRunsQuery,
    experimentRunStatuses: EXPERIMENT_RUN_STATUSES,
    handleAddRun,
    handleNameChange,
    handleOpenRun,
    handleStatusChange,
    name,
    page,
    projectQuery,
    selectedStatuses,
    setPage,
    totalPages,
    totalRuns,
  }
}
