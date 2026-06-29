import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useLogin } from '../context-api/logincontext'
import {
  createProjectExperiment,
  type CreateExperimentInput,
  type Experiment,
  getProjectExperiments,
  type PaginatedResponse,
  type PaginationParams,
} from '../services/experiment.service'
import type { ProjectSummary } from '../services/project.service'
import { PERMISSIONS_KEYS } from '../utils'

export function useQueryProjectExperiments(
  projectUuid?: string,
  pagination?: PaginationParams,
) {
  const { accessToken, hasPermission } = useLogin()
  const canListExperiments = hasPermission(
    PERMISSIONS_KEYS.EXPERIMENT.GET_EXPERIMENT,
  )

  return useQuery({
    enabled: Boolean(accessToken && projectUuid && canListExperiments),
    queryFn: () =>
      getProjectExperiments(
        accessToken as string,
        projectUuid as string,
        pagination,
      ),
    queryKey: ['projects', projectUuid, 'experiments', pagination],
  })
}

export function useMutationCreateExperiment(projectUuid: string) {
  const { accessToken } = useLogin()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: CreateExperimentInput) =>
      createProjectExperiment(accessToken as string, projectUuid, input),
    onSuccess: (createdExperiment, input) => {
      queryClient.setQueriesData<PaginatedResponse<Experiment>>(
        { queryKey: ['projects', projectUuid, 'experiments'] },
        (current) =>
          current
            ? {
                ...current,
                data: [
                  {
                    configCount: 0,
                    createdAt: new Date().toISOString(),
                    description: input.description,
                    experimentType: input.experimentType,
                    experimentUuid: createdExperiment.uuid,
                    name: input.name,
                    runCount: 0,
                  },
                  ...current.data,
                ],
                totalElements: current.totalElements + 1,
              }
            : current,
      )
      queryClient.setQueryData<ProjectSummary>(
        ['projects', projectUuid, 'summary'],
        (current) =>
          current
            ? { ...current, experimentCount: current.experimentCount + 1 }
            : current,
      )
    },
  })
}
