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
  status: string
  updatedAt: string
}

export type DatasetDetail = {
  datasetUuid: string
  name: string
  projectUuid: string
  totalElements: number
  versions: DatasetVersion[]
}

export type DatasetVersionUploadTicket = {
  expiresAt: string
  method: string
  requiredHeaders: Record<string, string>
  uploadUrl: string
  uuid: string
}

export type DatasetVersionStatus = 'ACTIVE' | 'FAILED'

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
    typeof (value as DatasetVersion).status === 'string' &&
    typeof (value as DatasetVersion).updatedAt === 'string'
  )
}

function isDatasetVersionUploadTicket(
  value: unknown,
): value is DatasetVersionUploadTicket {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as DatasetVersionUploadTicket).uuid === 'string' &&
    typeof (value as DatasetVersionUploadTicket).uploadUrl === 'string'
  )
}

function isDatasetDetail(value: unknown): value is DatasetDetail {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as DatasetDetail).datasetUuid === 'string' &&
    typeof (value as DatasetDetail).name === 'string' &&
    typeof (value as DatasetDetail).projectUuid === 'string' &&
    typeof (value as DatasetDetail).totalElements === 'number' &&
    Array.isArray((value as DatasetDetail).versions) &&
    (value as DatasetDetail).versions.every(isDatasetVersion)
  )
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

export async function getDataset(
  accessToken: string,
  projectUuid: string,
  datasetUuid: string,
  pagination?: PaginationParams,
): Promise<DatasetDetail> {
  const payload = await ApiService.get<unknown>(
    ServicesUrlEndpoints.GET_DATASET.replace(
      ':projectUuid',
      encodeURIComponent(projectUuid),
    ).replace(':datasetUuid', encodeURIComponent(datasetUuid)),
    {
      headers: getAuthenticatedRequestHeaders(accessToken),
      params: pagination,
    },
  )

  if (!isDatasetDetail(payload)) {
    throw new ApiServiceError(
      'The dataset service returned an invalid response.',
      500,
      payload,
    )
  }

  return payload
}

export async function initiateDatasetVersionUpload(
  accessToken: string,
  datasetUuid: string,
  fileName: string,
): Promise<DatasetVersionUploadTicket> {
  const payload = await ApiService.post<unknown>(
    ServicesUrlEndpoints.INITIATE_DATASET_VERSION_UPLOAD.replace(
      ':datasetUuid',
      encodeURIComponent(datasetUuid),
    ),
    { fileName },
    { headers: getAuthenticatedRequestHeaders(accessToken) },
  )

  if (!isDatasetVersionUploadTicket(payload)) {
    throw new ApiServiceError(
      'The dataset service returned an invalid upload response.',
      500,
      payload,
    )
  }

  return payload
}

export async function uploadDatasetVersionFile(
  ticket: DatasetVersionUploadTicket,
  file: File,
  onProgress?: (percent: number) => void,
): Promise<void> {
  // Uploads directly to the presigned URL - must not send our API auth headers here.
  await ApiService.put<unknown, File>(ticket.uploadUrl, file, {
    headers: ticket.requiredHeaders,
    onUploadProgress: (progressEvent) => {
      if (!onProgress || !progressEvent.total) return
      onProgress(
        Math.round((progressEvent.loaded / progressEvent.total) * 100),
      )
    },
  })
}

export async function updateDatasetVersionStatus(
  accessToken: string,
  datasetUuid: string,
  datasetVersionUuid: string,
  status: DatasetVersionStatus,
  failureMessage?: string,
): Promise<void> {
  await ApiService.patch<unknown>(
    ServicesUrlEndpoints.UPDATE_DATASET_VERSION_STATUS.replace(
      ':datasetUuid',
      encodeURIComponent(datasetUuid),
    ).replace(':datasetVersionUuid', encodeURIComponent(datasetVersionUuid)),
    status === 'FAILED' ? { failureMessage, status } : { status },
    { headers: getAuthenticatedRequestHeaders(accessToken) },
  )
}

export async function downloadDatasetVersion(
  accessToken: string,
  datasetUuid: string,
  datasetVersionUuid: string,
): Promise<Blob> {
  const endpoint = ServicesUrlEndpoints.GET_DATASET_VERSION.replace(
    ':datasetUuid',
    encodeURIComponent(datasetUuid),
  ).replace(':datasetVersionUuid', encodeURIComponent(datasetVersionUuid))
  const { Accept: omittedAccept, ...downloadHeaders } =
    getAuthenticatedRequestHeaders(accessToken)
  void omittedAccept

  return ApiService.get<Blob>(endpoint, {
    headers: downloadHeaders,
    responseType: 'blob',
  })
}
