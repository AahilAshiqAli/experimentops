import { useQuery } from '@tanstack/react-query'

import { useLogin } from '../context-api/logincontext'
import {
  getDataset,
  getProjectDatasets,
  type PaginationParams,
} from '../services/dataset.service'
import { PERMISSIONS_KEYS } from '../utils'

export function useQueryProjectDatasets(
  projectUuid?: string,
  pagination?: PaginationParams,
) {
  const { accessToken, hasPermission } = useLogin()
  const canListDatasets = hasPermission(PERMISSIONS_KEYS.DATASET.GET_DATASET)

  return useQuery({
    enabled: Boolean(accessToken && projectUuid && canListDatasets),
    queryFn: () =>
      getProjectDatasets(
        accessToken as string,
        projectUuid as string,
        pagination,
      ),
    queryKey: ['projects', projectUuid, 'datasets', pagination],
  })
}

export function useQueryDataset(
  projectUuid?: string,
  datasetUuid?: string,
  pagination: PaginationParams = { page: 0, size: 20 },
) {
  const { accessToken, hasPermission } = useLogin()
  const canGetDataset = hasPermission(PERMISSIONS_KEYS.DATASET.GET_DATASET)

  return useQuery({
    enabled: Boolean(
      accessToken && projectUuid && datasetUuid && canGetDataset,
    ),
    queryFn: () =>
      getDataset(
        accessToken as string,
        projectUuid as string,
        datasetUuid as string,
        pagination,
      ),
    queryKey: ['projects', projectUuid, 'datasets', datasetUuid, pagination],
  })
}
