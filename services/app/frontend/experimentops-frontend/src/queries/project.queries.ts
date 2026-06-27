import { useQuery } from '@tanstack/react-query'

import { useLogin } from '../context-api/logincontext'
import { getProjects } from '../services/project.service'
import { PERMISSIONS_KEYS, UtilService } from '../utils'

export function useQueryProjects() {
  const { accessToken, permissions } = useLogin()
  const canListProjects =
    permissions.length > 0 &&
    UtilService.checkPermission(PERMISSIONS_KEYS.PROJECT.GET_PROJECT)

  return useQuery({
    enabled: Boolean(accessToken) && canListProjects,
    queryFn: () => getProjects(accessToken as string),
    queryKey: ['projects'],
  })
}
