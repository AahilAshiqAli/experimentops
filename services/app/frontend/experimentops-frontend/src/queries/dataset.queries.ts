import { useQuery } from '@tanstack/react-query'

import { useLogin } from '../context-api/logincontext'
import {
  getDatasetDetails,
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

export function useQueryDataset(datasetUuid?: string) {
  const { accessToken, hasPermission } = useLogin()
  const canGetDataset = hasPermission(PERMISSIONS_KEYS.DATASET.GET_DATASET)

  return useQuery({
    enabled: Boolean(accessToken && datasetUuid && canGetDataset),
    queryFn: () =>
      getDatasetDetails(accessToken as string, datasetUuid as string),
    queryKey: ['datasets', datasetUuid],
  })
}
