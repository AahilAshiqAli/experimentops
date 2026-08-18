import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useParams } from 'react-router-dom'

import { useLogin } from '../../context-api/logincontext'
import { useDebouncedValue } from '../../hooks'
import {
  useMutationCompareExperimentRuns,
  useQueryExperimentRuns,
  useQueryProject,
} from '../../queries'
import {
  EXPERIMENT_RUN_STATUSES,
  type ExperimentRun,
  type ExperimentRunComparisonAxis,
  type ExperimentRunStatus,
} from '../../services/experimentRun.service'
import { Toaster } from '../../services/toaster.service'
import { trackExperimentRun } from '../../services/experimentRunTracking.service'
import { PERMISSIONS_KEYS } from '../../utils'

const RUNS_PER_PAGE = 20
const ACTIVE_EXPERIMENT_RUN_STATUSES: ExperimentRunStatus[] = [
  'PENDING',
  'RUNNING',
]

export function useExperimentRunsContainer() {
  const { t } = useTranslation()
  const { experimentUuid, projectUuid } = useParams<{
    experimentUuid: string
    projectUuid: string
  }>()
  const navigate = useNavigate()
  const { hasPermission } = useLogin()
  const projectQuery = useQueryProject(projectUuid)
  const compareExperimentRunsMutation = useMutationCompareExperimentRuns()
  const [page, setPage] = useState(1)
  const [name, setName] = useState('')
  const [selectedStatuses, setSelectedStatuses] = useState<
    ExperimentRunStatus[]
  >(ACTIVE_EXPERIMENT_RUN_STATUSES)
  const [selectedRuns, setSelectedRuns] = useState<Map<string, string>>(
    () => new Map(),
  )
  const [comparisonAxis, setComparisonAxis] =
    useState<ExperimentRunComparisonAxis>('CONFIG')
  const [isCompareDialogOpen, setIsCompareDialogOpen] = useState(false)
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
  const canCompareExperimentRuns = hasPermission(
    PERMISSIONS_KEYS.EXPERIMENT_RUN.COMPARE_EXPERIMENT_RUNS,
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

  const handleToggleRun = (run: ExperimentRun, selected: boolean) => {
    if (run.status !== 'SUCCEEDED') return

    setSelectedRuns((current) => {
      const next = new Map(current)
      if (selected) next.set(run.uuid, run.name)
      else next.delete(run.uuid)
      return next
    })
  }

  const handleCompareRuns = () => {
    const experimentRunUuids = [...selectedRuns.keys()]
    if (experimentRunUuids.length < 2) return

    compareExperimentRunsMutation.mutate(
      { comparisonAxis, experimentRunUuids },
      {
        onError: (error) => {
          Toaster.error(
            error instanceof Error ? error.message : t('runs.compare.error'),
          )
        },
        onSuccess: () => {
          const searchParams = new URLSearchParams({ axis: comparisonAxis })
          experimentRunUuids.forEach((uuid) => searchParams.append('run', uuid))
          navigate(
            `/projects/${projectUuid}/experiments/${experimentUuid}/experiment-runs/compare?${searchParams.toString()}`,
            {
              state: {
                runNames: Object.fromEntries(selectedRuns),
              },
            },
          )
        },
      },
    )
  }

  return {
    activePage,
    canCompareExperimentRuns,
    canListExperimentRuns,
    compareExperimentRunsMutation,
    comparisonAxis,
    experimentRunQueryParams,
    experimentRunsQuery,
    experimentRunStatuses: EXPERIMENT_RUN_STATUSES,
    handleAddRun,
    handleCompareRuns,
    handleNameChange,
    handleOpenRun,
    handleStatusChange,
    handleToggleRun,
    isCompareDialogOpen,
    isRunSelected: (runUuid: string) => selectedRuns.has(runUuid),
    name,
    page,
    projectQuery,
    selectedStatuses,
    selectedRunCount: selectedRuns.size,
    setComparisonAxis,
    setIsCompareDialogOpen,
    setPage,
    totalPages,
    totalRuns,
  }
}
