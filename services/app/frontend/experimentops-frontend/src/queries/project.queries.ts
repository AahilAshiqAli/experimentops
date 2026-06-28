import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useLogin } from '../context-api/logincontext'
import {
  getProjectSummary,
  getProjects,
  type PaginatedResponse,
  type PaginationParams,
  type Project,
  type ProjectSummary,
  type UpdateProjectInput,
  updateProject,
} from '../services/project.service'
import { PERMISSIONS_KEYS, UtilService } from '../utils'

export function useQueryProjects(pagination?: PaginationParams) {
  const { accessToken, permissions } = useLogin()
  const canListProjects =
    permissions.length > 0 &&
    UtilService.checkPermission(PERMISSIONS_KEYS.PROJECT.GET_PROJECT)

  return useQuery({
    enabled: Boolean(accessToken) && canListProjects,
    queryFn: () => getProjects(accessToken as string, pagination),
    queryKey: ['projects', pagination],
  })
}

export function useQueryProject(projectUuid?: string) {
  const { accessToken, hasPermission } = useLogin()
  const canGetProject = hasPermission(PERMISSIONS_KEYS.PROJECT.GET_PROJECT)

  return useQuery({
    enabled: Boolean(accessToken && projectUuid && canGetProject),
    queryFn: () =>
      getProjectSummary(accessToken as string, projectUuid as string),
    queryKey: ['projects', projectUuid, 'summary'],
  })
}

export function useMutationUpdateProject(projectUuid: string) {
  const { accessToken } = useLogin()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: UpdateProjectInput) =>
      updateProject(accessToken as string, projectUuid, input),
    onSuccess: (_project, input) => {
      queryClient.setQueryData<ProjectSummary>(
        ['projects', projectUuid, 'summary'],
        (current) => (current ? { ...current, ...input } : current),
      )
      queryClient.setQueriesData<PaginatedResponse<Project>>(
        { queryKey: ['projects'] },
        (current) =>
          current
            ? {
                ...current,
                data: current.data.map((project) =>
                  project.uuid === projectUuid
                    ? { ...project, ...input }
                    : project,
                ),
              }
            : current,
      )
    },
  })
}
