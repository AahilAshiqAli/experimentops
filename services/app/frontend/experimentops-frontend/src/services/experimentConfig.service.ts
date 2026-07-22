import ApiService, { ApiServiceError } from '../utils/api.service'
import { getAuthenticatedRequestHeaders } from './jwt.service'
import { ServicesUrlEndpoints } from './servicesEndpointConstant'
import {
  isExperimentTypeManifest,
  type ExperimentTypeManifest,
} from './experimentType.service'

export type ExperimentConfig = {
  config: Record<string, unknown>
  experimentType: string
  experimentUuid: string
  formatMappings: ExperimentTypeManifest[]
  name: string
  status: string
  updatedAt: string
  uuid: string
}

export type ExperimentConfigDetail = Pick<
  ExperimentConfig,
  'config' | 'experimentType' | 'formatMappings' | 'name' | 'uuid'
>

export type CreateExperimentConfigInput = {
  config: Record<string, unknown>
  experimentType: string
  name: string
}

export type ExperimentConfigDetailsInput = {
  experimentConfigUuids: string[]
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

type ExperimentConfigDetailPayload = Omit<
  ExperimentConfigDetail,
  'formatMappings'
> & {
  formatMappings?: unknown
}

type ExperimentConfigPayload = ExperimentConfigDetailPayload & {
  experimentUuid?: unknown
  status: string
  updatedAt: string
}

function isExperimentConfigDetailPayload(
  value: unknown,
): value is ExperimentConfigDetailPayload {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ExperimentConfigDetailPayload).uuid === 'string' &&
    typeof (value as ExperimentConfigDetailPayload).name === 'string' &&
    typeof (value as ExperimentConfigDetailPayload).experimentType ===
      'string' &&
    typeof (value as ExperimentConfigDetailPayload).config === 'object' &&
    (value as ExperimentConfigDetailPayload).config !== null
  )
}

function isExperimentConfigPayload(
  value: unknown,
): value is ExperimentConfigPayload {
  return (
    isExperimentConfigDetailPayload(value) &&
    typeof (value as ExperimentConfigPayload).status === 'string' &&
    typeof (value as ExperimentConfigPayload).updatedAt === 'string'
  )
}

function toExperimentConfigDetail(
  value: unknown,
): ExperimentConfigDetail | null {
  if (!isExperimentConfigDetailPayload(value)) return null

  return {
    config: value.config,
    experimentType: value.experimentType,
    formatMappings: normalizeFormatMappings(value.formatMappings),
    name: value.name,
    uuid: value.uuid,
  }
}

function normalizeFormatMappings(value: unknown): ExperimentTypeManifest[] {
  if (!Array.isArray(value)) return []

  return value
    .map((manifest) => {
      if (typeof manifest !== 'object' || manifest === null) return manifest
      const inputRelationships = (manifest as { inputRelationships?: unknown })
        .inputRelationships

      return inputRelationships === undefined || inputRelationships === null
        ? { ...manifest, inputRelationships: [] }
        : manifest
    })
    .filter(isExperimentTypeManifest)
}

function toExperimentConfig(
  value: unknown,
  experimentUuid: string,
): ExperimentConfig | null {
  if (!isExperimentConfigPayload(value)) return null

  const detail = toExperimentConfigDetail(value)
  if (!detail) return null

  return {
    ...detail,
    experimentUuid:
      typeof value.experimentUuid === 'string'
        ? value.experimentUuid
        : experimentUuid,
    status: value.status,
    updatedAt: value.updatedAt,
  }
}

function toPaginatedResponse(
  payload: unknown,
  experimentUuid: string,
): PaginatedResponse<ExperimentConfig> | null {
  if (
    typeof payload !== 'object' ||
    payload === null ||
    !Array.isArray((payload as PaginatedResponse<unknown>).data) ||
    typeof (payload as PaginatedResponse<unknown>).totalElements !== 'number'
  ) {
    return null
  }

  return {
    data: (payload as PaginatedResponse<unknown>).data
      .map((value) => toExperimentConfig(value, experimentUuid))
      .filter((value): value is ExperimentConfig => value !== null),
    totalElements: (payload as PaginatedResponse<unknown>).totalElements,
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

  const experimentConfigs = toPaginatedResponse(payload, experimentUuid)

  if (!experimentConfigs) {
    throw new ApiServiceError(
      'The experiment config service returned an invalid response.',
      500,
      payload,
    )
  }

  return experimentConfigs
}

export async function getExperimentConfigsByUuids(
  accessToken: string,
  experimentUuid: string,
  experimentConfigUuids: string[],
): Promise<ExperimentConfigDetail[]> {
  const payload = await ApiService.post<unknown, ExperimentConfigDetailsInput>(
    ServicesUrlEndpoints.POST_EXPERIMENT_CONFIG_DETAILS.replace(
      ':experimentUuid',
      encodeURIComponent(experimentUuid),
    ),
    { experimentConfigUuids },
    {
      headers: {
        ...getAuthenticatedRequestHeaders(accessToken),
        'Content-Type': 'application/json',
      },
    },
  )
  const experimentConfigs = Array.isArray(payload)
    ? payload
        .map(toExperimentConfigDetail)
        .filter((config): config is ExperimentConfigDetail => Boolean(config))
    : []

  if (
    !Array.isArray(payload) ||
    experimentConfigs.length !== payload.length ||
    experimentConfigs.some((config) => config.formatMappings.length === 0) ||
    experimentConfigUuids.some(
      (uuid) => !experimentConfigs.some((config) => config.uuid === uuid),
    )
  ) {
    throw new ApiServiceError(
      'The experiment config detail service returned an invalid response.',
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
