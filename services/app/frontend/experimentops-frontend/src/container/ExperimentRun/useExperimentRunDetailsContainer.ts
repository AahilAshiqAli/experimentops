import { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'

import { useLogin } from '../../context-api/logincontext'
import {
  useQueryExperimentConfigsByUuids,
  useQueryExperimentRun,
  useMutationExperimentRunLogDownloadUrl,
  useQueryExperimentRunLogs,
  useQueryRunArtifacts,
} from '../../queries'
import { Toaster } from '../../services/toaster.service'
import { PERMISSIONS_KEYS } from '../../utils'

export function useExperimentRunDetailsContainer() {
  const { experimentRunUuid, experimentUuid, projectUuid } = useParams<{
    experimentRunUuid: string
    experimentUuid: string
    projectUuid: string
  }>()
  const { hasPermission } = useLogin()
  const [logPagination, setLogPagination] = useState({
    experimentRunUuid,
    page: 0,
  })
  const logPage =
    logPagination.experimentRunUuid === experimentRunUuid
      ? logPagination.page
      : 0
  const setLogPage = (page: number) =>
    setLogPagination({ experimentRunUuid, page })
  const experimentRunQuery = useQueryExperimentRun(experimentRunUuid)
  const runArtifactsQuery = useQueryRunArtifacts(experimentRunUuid)
  const experimentRunLogsQuery = useQueryExperimentRunLogs(experimentRunUuid, {
    page: logPage,
    size: 20,
  })
  const logDownloadMutation = useMutationExperimentRunLogDownloadUrl()
  const experimentConfigUuids = useMemo(
    () => [
      ...new Set(
        (experimentRunQuery.data?.executionMode ?? []).map(
          (step) => step.experimentConfigUuid,
        ),
      ),
    ],
    [experimentRunQuery.data?.executionMode],
  )
  const experimentConfigsQuery = useQueryExperimentConfigsByUuids(
    experimentUuid,
    experimentConfigUuids,
  )

  useEffect(() => {
    if (!experimentRunLogsQuery.error) return
    Toaster.error(
      experimentRunLogsQuery.error instanceof Error
        ? experimentRunLogsQuery.error.message
        : 'Unable to load logs for this run.',
    )
  }, [experimentRunLogsQuery.error])

  const handleDownloadLogs = async () => {
    if (!experimentRunUuid) return

    try {
      const download = await logDownloadMutation.mutateAsync(experimentRunUuid)
      const anchor = document.createElement('a')
      anchor.href = download.downloadUrl
      anchor.download = `experiment-run-${experimentRunUuid}-logs.txt`
      anchor.rel = 'noopener'
      document.body.appendChild(anchor)
      anchor.click()
      anchor.remove()
    } catch (error) {
      Toaster.error(
        error instanceof Error
          ? error.message
          : 'Unable to download logs for this run.',
      )
    }
  }

  return {
    canViewExperimentRun: hasPermission(
      PERMISSIONS_KEYS.EXPERIMENT_RUN.GET_EXPERIMENT_RUNS,
    ),
    experimentRunQuery,
    experimentRunUuid,
    experimentRunLogsQuery,
    experimentConfigsQuery,
    experimentUuid,
    projectUuid,
    handleDownloadLogs,
    isDownloadingLogs: logDownloadMutation.isPending,
    logPage,
    runArtifactsQuery,
    setLogPage,
  }
}
