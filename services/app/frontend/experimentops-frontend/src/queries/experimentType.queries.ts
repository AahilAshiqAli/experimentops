import { useQuery } from '@tanstack/react-query'

import { useLogin } from '../context-api/logincontext'
import { getExperimentTypes } from '../services/experimentType.service'
import { PERMISSIONS_KEYS } from '../utils'

export function useQueryExperimentTypes(enabled = true) {
  const { accessToken, hasPermission } = useLogin()
  const canListExperimentTypes = hasPermission(
    PERMISSIONS_KEYS.EXPERIMENT_TYPE.GET_EXPERIMENT_TYPE,
  )

  return useQuery({
    enabled: Boolean(accessToken && canListExperimentTypes && enabled),
    queryFn: () => getExperimentTypes(accessToken as string),
    queryKey: ['experimentTypes'],
  })
}
