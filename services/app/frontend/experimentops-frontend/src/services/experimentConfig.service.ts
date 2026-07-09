import ApiService, { ApiServiceError } from '../utils/api.service'
import { getAuthenticatedRequestHeaders } from './jwt.service'
import { ServicesUrlEndpoints } from './servicesEndpointConstant'

export type ExperimentConfig = {
  config: Record<string, unknown>
  experimentType: string
  experimentUuid: string
  name: string
  status: string
  updatedAt: string
  uuid: string
}

export type CreateExperimentConfigInput = {
  config: Record<string, unknown>
  experimentType: string
  name: string
}

export type CreatedExperimentConfig = ExperimentConfig

export type PaginationParams = {
  experimentType?: string
  page?: number
  size?: number
}

export type PaginatedResponse<T> = {
  data: T[]
  totalElements: number
}

function isExperimentConfig(value: unknown): value is ExperimentConfig {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ExperimentConfig).uuid === 'string' &&
    typeof (value as ExperimentConfig).name === 'string' &&
    typeof (value as ExperimentConfig).experimentType === 'string' &&
    typeof (value as ExperimentConfig).config === 'object' &&
    (value as ExperimentConfig).config !== null &&
    typeof (value as ExperimentConfig).experimentUuid === 'string' &&
    typeof (value as ExperimentConfig).status === 'string' &&
    typeof (value as ExperimentConfig).updatedAt === 'string'
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

export async function getExperimentConfigs(
  accessToken: string,
  experimentUuid: string,
  pagination?: PaginationParams,
): Promise<PaginatedResponse<ExperimentConfig>> {
  const payload = await ApiService.get<unknown>(
    ServicesUrlEndpoints.GET_EXPERIMENT_CONFIGS.replace(
      ':experimentUuid',
      encodeURIComponent(experimentUuid),
    ),
    {
      headers: getAuthenticatedRequestHeaders(accessToken),
      params: pagination,
    },
  )

  const experimentConfigs = toPaginatedResponse(payload, isExperimentConfig)

  if (!experimentConfigs) {
    throw new ApiServiceError(
      'The experiment config service returned an invalid response.',
      500,
      payload,
    )
  }

  return experimentConfigs
}

export async function createExperimentConfig(
  accessToken: string,
  experimentUuid: string,
  input: CreateExperimentConfigInput,
) {
  return ApiService.post<CreatedExperimentConfig, CreateExperimentConfigInput>(
    ServicesUrlEndpoints.GET_EXPERIMENT_CONFIGS.replace(
      ':experimentUuid',
      encodeURIComponent(experimentUuid),
    ),
    input,
    {
      headers: {
        ...getAuthenticatedRequestHeaders(accessToken),
        'Content-Type': 'application/json',
      },
    },
  )
}

export async function updateExperimentConfig(
  accessToken: string,
  experimentUuid: string,
  uuid: string,
  input: CreateExperimentConfigInput,
) {
  return ApiService.put<ExperimentConfig, CreateExperimentConfigInput>(
    ServicesUrlEndpoints.UPDATE_EXPERIMENT_CONFIG.replace(
      ':experimentUuid',
      encodeURIComponent(experimentUuid),
    ).replace(':uuid', encodeURIComponent(uuid)),
    input,
    {
      headers: {
        ...getAuthenticatedRequestHeaders(accessToken),
        'Content-Type': 'application/json',
      },
    },
  )
}
