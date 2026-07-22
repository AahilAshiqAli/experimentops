import { useMutation, useQuery } from '@tanstack/react-query'

import { useLogin } from '../context-api/logincontext'
import {
  getRunArtifactDownloadUrl,
  getRunArtifacts,
} from '../services/runArtifact.service'
import { PERMISSIONS_KEYS } from '../utils'

export function useQueryRunArtifacts(experimentRunUuid?: string) {
  const { accessToken, hasPermission } = useLogin()
  const canViewExperimentRuns = hasPermission(
    PERMISSIONS_KEYS.EXPERIMENT_RUN.GET_EXPERIMENT_RUNS,
  )

  return useQuery({
    enabled: Boolean(accessToken && experimentRunUuid && canViewExperimentRuns),
    queryFn: () =>
      getRunArtifacts(accessToken as string, experimentRunUuid as string),
    queryKey: ['experiment-runs', experimentRunUuid, 'artifacts'],
  })
}

export function useMutationRunArtifactDownloadUrl() {
  const { accessToken } = useLogin()

  return useMutation({
    mutationFn: (artifactUuid: string) =>
      getRunArtifactDownloadUrl(accessToken as string, artifactUuid),
  })
}
