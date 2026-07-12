import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useLogin } from '../context-api/logincontext'
import {
  createExperimentRun,
  getExperimentRunStatuses,
  getExperimentRuns,
  type CreateExperimentRunInput,
  type ExperimentRunListParams,
  validateExperimentRun,
  type ValidateExperimentRunInput,
} from '../services/experimentRun.service'
import { trackExperimentRun } from '../services/experimentRunTracking.service'
import { PERMISSIONS_KEYS } from '../utils'

export function useMutationCreateExperimentRun(experimentUuid: string) {
  const { accessToken } = useLogin()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: CreateExperimentRunInput) =>
      createExperimentRun(accessToken as string, experimentUuid, input),
    onSuccess: (experimentRun) => {
      trackExperimentRun(experimentRun.experimentRunUuid)
      void queryClient.invalidateQueries({
        queryKey: ['experiments', experimentUuid, 'runs'],
      })
    },
  })
}

export function useQueryExperimentRuns(
  experimentUuid?: string,
  params: ExperimentRunListParams = {},
) {
  const { accessToken, hasPermission } = useLogin()
  const canListExperimentRuns = hasPermission(
    PERMISSIONS_KEYS.EXPERIMENT_RUN.GET_EXPERIMENT_RUNS,
  )

  return useQuery({
    enabled: Boolean(accessToken && experimentUuid && canListExperimentRuns),
    queryFn: () =>
      getExperimentRuns(
        accessToken as string,
        experimentUuid as string,
        params,
      ),
    queryKey: ['experiments', experimentUuid, 'runs', params],
  })
}

export function useQueryExperimentRunStatuses(experimentRunUuids: string[]) {
  const { accessToken, hasPermission } = useLogin()
  const canListExperimentRuns = hasPermission(
    PERMISSIONS_KEYS.EXPERIMENT_RUN.GET_EXPERIMENT_RUNS,
  )

  return useQuery({
    enabled: Boolean(
      accessToken && experimentRunUuids.length && canListExperimentRuns,
    ),
    queryFn: () =>
      getExperimentRunStatuses(accessToken as string, experimentRunUuids),
    queryKey: ['experiment-runs', 'statuses', experimentRunUuids],
    refetchInterval: 30_000,
  })
}

export function useMutationValidateExperimentRun(experimentUuid: string) {
  const { accessToken } = useLogin()

  return useMutation({
    mutationFn: (input: ValidateExperimentRunInput) =>
      validateExperimentRun(accessToken as string, experimentUuid, input),
  })
}
