import { useEffect, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'

import { useQueryExperimentRunStatuses } from '../../../queries'
import type { PaginatedExperimentRuns } from '../../../services/experimentRun.service'
import {
  getTrackedExperimentRuns,
  subscribeToTrackedExperimentRuns,
  untrackExperimentRun,
} from '../../../services/experimentRunTracking.service'
import { Toaster } from '../../../services/toaster.service'

const TERMINAL_STATUSES = new Set(['SUCCEEDED', 'FAILED'])

export function useExperimentRunStatusMonitor() {
  const queryClient = useQueryClient()
  const [experimentRunUuids, setExperimentRunUuids] = useState(
    getTrackedExperimentRuns,
  )
  const statusQuery = useQueryExperimentRunStatuses(experimentRunUuids)

  useEffect(
    () =>
      subscribeToTrackedExperimentRuns(() =>
        setExperimentRunUuids(getTrackedExperimentRuns()),
      ),
    [],
  )

  useEffect(() => {
    if (!statusQuery.data?.length) return

    const statusesByUuid = new Map(
      statusQuery.data.map((run) => [run.experimentRunUuid, run]),
    )

    queryClient.setQueriesData<PaginatedExperimentRuns>(
      {
        predicate: (query) =>
          query.queryKey[0] === 'experiments' && query.queryKey[2] === 'runs',
      },
      (current) => {
        if (!current || !Array.isArray(current.data)) return current

        const data = current.data.map((run) => {
          const status = statusesByUuid.get(run.uuid)
          return status
            ? {
                ...run,
                duration: status.duration,
                progress: status.progress,
                status: status.status,
              }
            : run
        })

        return { ...current, data }
      },
    )

    statusQuery.data.forEach((run) => {
      if (!TERMINAL_STATUSES.has(run.status)) return

      untrackExperimentRun(run.experimentRunUuid)
      Toaster[run.status === 'SUCCEEDED' ? 'success' : 'error'](
        `Experiment run ${run.experimentRunUuid} ${
          run.status === 'SUCCEEDED' ? 'succeeded.' : 'failed.'
        }`,
      )
    })
  }, [queryClient, statusQuery.data])

  return statusQuery
}
