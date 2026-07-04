import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useLogin } from '../context-api/logincontext'
import {
  createExperimentConfig,
  type CreateExperimentConfigInput,
  type ExperimentConfig,
  getExperimentConfigs,
  type PaginatedResponse,
  type PaginationParams,
  updateExperimentConfig,
} from '../services/experimentConfig.service'
import { PERMISSIONS_KEYS } from '../utils'

export function useQueryExperimentConfigs(
  experimentUuid?: string,
  pagination?: PaginationParams,
) {
  const { accessToken, hasPermission } = useLogin()
  const canListExperimentConfigs = hasPermission(
    PERMISSIONS_KEYS.EXPERIMENT_CONFIG.GET_EXPERIMENT_CONFIG,
  )

  return useQuery({
    enabled: Boolean(
      accessToken && experimentUuid && canListExperimentConfigs,
    ),
    queryFn: () =>
      getExperimentConfigs(
        accessToken as string,
        experimentUuid as string,
        pagination,
      ),
    queryKey: ['experiments', experimentUuid, 'configs', pagination],
  })
}

export function useMutationCreateExperimentConfig(experimentUuid: string) {
  const { accessToken } = useLogin()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: CreateExperimentConfigInput) =>
      createExperimentConfig(accessToken as string, experimentUuid, input),
    onSuccess: (createdExperimentConfig) => {
      queryClient.setQueriesData<PaginatedResponse<ExperimentConfig>>(
        { queryKey: ['experiments', experimentUuid, 'configs'] },
        (current) =>
          current
            ? {
                ...current,
                data: [createdExperimentConfig, ...current.data],
                totalElements: current.totalElements + 1,
              }
            : current,
      )
    },
  })
}

export function useMutationUpdateExperimentConfig(experimentUuid: string) {
  const { accessToken } = useLogin()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      input,
      uuid,
    }: {
      input: CreateExperimentConfigInput
      uuid: string
    }) => updateExperimentConfig(accessToken as string, experimentUuid, uuid, input),
    onSuccess: (updatedExperimentConfig) => {
      queryClient.setQueriesData<PaginatedResponse<ExperimentConfig>>(
        { queryKey: ['experiments', experimentUuid, 'configs'] },
        (current) =>
          current
            ? {
                ...current,
                data: current.data.map((experimentConfig) =>
                  experimentConfig.uuid === updatedExperimentConfig.uuid
                    ? updatedExperimentConfig
                    : experimentConfig,
                ),
              }
            : current,
      )
    },
  })
}
