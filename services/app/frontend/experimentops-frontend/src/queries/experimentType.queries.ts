import { useQueries, useQuery } from '@tanstack/react-query'

import { useLogin } from '../context-api/logincontext'
import {
  getExperimentTypes,
  type ExperimentTypeListParams,
} from '../services/experimentType.service'
import { PERMISSIONS_KEYS } from '../utils'

export function useQueryExperimentTypes(
  params: ExperimentTypeListParams = {},
  enabled = true,
) {
  const { accessToken, hasPermission } = useLogin()
  const canListExperimentTypes = hasPermission(
    PERMISSIONS_KEYS.EXPERIMENT_TYPE.GET_EXPERIMENT_TYPE,
  )

  return useQuery({
    enabled: Boolean(accessToken && canListExperimentTypes && enabled),
    queryFn: () => getExperimentTypes(accessToken as string, params),
    queryKey: ['experimentTypes', params],
  })
}

export function useQueryExperimentTypesByNames(
  names: string[],
  enabled = true,
) {
  const { accessToken, hasPermission } = useLogin()
  const canListExperimentTypes = hasPermission(
    PERMISSIONS_KEYS.EXPERIMENT_TYPE.GET_EXPERIMENT_TYPE,
  )

  return useQueries({
    queries: names.map((name) => ({
      enabled: Boolean(accessToken && canListExperimentTypes && enabled),
      queryFn: () =>
        getExperimentTypes(accessToken as string, {
          name,
          page: 0,
          size: 20,
        }),
      queryKey: ['experimentTypes', { name, page: 0, size: 20 }],
    })),
  })
}
