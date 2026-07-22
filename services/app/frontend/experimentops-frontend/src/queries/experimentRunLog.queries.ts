import { useMutation, useQuery } from '@tanstack/react-query'

import { useLogin } from '../context-api/logincontext'
import {
  getExperimentRunLogDownloadUrl,
  getExperimentRunLogs,
  type ExperimentRunLogListParams,
} from '../services/experimentRunLog.service'
import { PERMISSIONS_KEYS } from '../utils'

export function useQueryExperimentRunLogs(
  experimentRunUuid?: string,
  params: ExperimentRunLogListParams = {},
) {
  const { accessToken, hasPermission } = useLogin()
  const canViewExperimentRun = hasPermission(
    PERMISSIONS_KEYS.EXPERIMENT_RUN.GET_EXPERIMENT_RUNS,
  )

  return useQuery({
    enabled: Boolean(accessToken && experimentRunUuid && canViewExperimentRun),
    placeholderData: (previousData) => previousData,
    queryFn: () =>
      getExperimentRunLogs(
        accessToken as string,
        experimentRunUuid as string,
        params,
      ),
    queryKey: ['experiment-runs', experimentRunUuid, 'logs', params],
  })
}

export function useMutationExperimentRunLogDownloadUrl() {
  const { accessToken } = useLogin()

  return useMutation({
    mutationFn: (experimentRunUuid: string) =>
      getExperimentRunLogDownloadUrl(accessToken as string, experimentRunUuid),
  })
}
