import ApiService, { ApiServiceError } from '../utils/api.service'
import { getAuthenticatedRequestHeaders } from './jwt.service'
import { ServicesUrlEndpoints } from './servicesEndpointConstant'

export type Dataset = {
  datasetUuid: string
  name: string
  projectUuid: string
  status: string
  updatedAt: string
  versionCount: number
}

export type DatasetVersion = {
  datasetVersionUuid: string
  format: string
  originalFileName: string
  size: number
  updatedAt: string
}

export type DatasetDetails = {
  datasetUuid: string
  name: string
  projectUuid: string
  versions: DatasetVersion[]
}

export type PaginationParams = {
  page?: number
  size?: number
}

export type PaginatedResponse<T> = {
  data: T[]
  totalElements: number
}

function isDataset(value: unknown): value is Dataset {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as Dataset).datasetUuid === 'string' &&
    typeof (value as Dataset).name === 'string' &&
    typeof (value as Dataset).projectUuid === 'string' &&
    typeof (value as Dataset).status === 'string' &&
    typeof (value as Dataset).updatedAt === 'string' &&
    typeof (value as Dataset).versionCount === 'number'
  )
}

function toPaginatedResponse<T>(
  payload: unknown,
  isItem: (value: unknown) => value is T,
): PaginatedResponse<T> | null {
  if (
    typeof payload !== 'object' ||
    payload === null ||
    !Array.isArray((payload as PaginatedResponse<T>).data) ||
    typeof (payload as PaginatedResponse<T>).totalElements !== 'number'
  ) {
    return null
  }

  return {
    data: (payload as PaginatedResponse<T>).data.filter(isItem),
    totalElements: (payload as PaginatedResponse<T>).totalElements,
  }
}

function isDatasetVersion(value: unknown): value is DatasetVersion {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as DatasetVersion).datasetVersionUuid === 'string' &&
    typeof (value as DatasetVersion).originalFileName === 'string' &&
    typeof (value as DatasetVersion).format === 'string' &&
    typeof (value as DatasetVersion).size === 'number' &&
    typeof (value as DatasetVersion).updatedAt === 'string'
  )
}

function toDatasetDetails(value: unknown): DatasetDetails | null {
  if (
    typeof value !== 'object' ||
    value === null ||
    typeof (value as DatasetDetails).datasetUuid !== 'string' ||
    typeof (value as DatasetDetails).projectUuid !== 'string' ||
    typeof (value as DatasetDetails).name !== 'string' ||
    !Array.isArray((value as DatasetDetails).versions)
  ) {
    return null
  }

  return {
    datasetUuid: (value as DatasetDetails).datasetUuid,
    name: (value as DatasetDetails).name,
    projectUuid: (value as DatasetDetails).projectUuid,
    versions: (value as DatasetDetails).versions.filter(isDatasetVersion),
  }
}

export async function getProjectDatasets(
  accessToken: string,
  projectUuid: string,
  pagination?: PaginationParams,
): Promise<PaginatedResponse<Dataset>> {
  const payload = await ApiService.get<unknown>(
    ServicesUrlEndpoints.GET_PROJECT_DATASETS.replace(
      ':projectUuid',
      encodeURIComponent(projectUuid),
    ),
    {
      headers: getAuthenticatedRequestHeaders(accessToken),
      params: pagination,
    },
  )

  const datasets = toPaginatedResponse(payload, isDataset)

  if (!datasets) {
    throw new ApiServiceError(
      'The dataset service returned an invalid response.',
      500,
      payload,
    )
  }

  return datasets
}

export async function getDatasetDetails(
  accessToken: string,
  datasetUuid: string,
): Promise<DatasetDetails> {
  const payload = await ApiService.get<unknown>(
    ServicesUrlEndpoints.GET_DATASET.replace(
      ':datasetUuid',
      encodeURIComponent(datasetUuid),
    ),
    { headers: getAuthenticatedRequestHeaders(accessToken) },
  )
  const dataset = toDatasetDetails(payload)

  if (!dataset) {
    throw new ApiServiceError(
      'The dataset service returned an invalid response.',
      500,
      payload,
    )
  }

  return dataset
}
