import ApiService, { ApiServiceError } from '../utils/api.service'
import { getAuthenticatedRequestHeaders } from './jwt.service'
import { ServicesUrlEndpoints } from './servicesEndpointConstant'

export type ExperimentTypeDefaultConfig = {
  datatype: 'boolean' | 'list' | 'number' | 'string'
  defaultValue: unknown
  name: string
  regex: string | null
}

export type ExperimentTypeFormatMapping = {
  inputFormat: string
  outputFormat: string
}

export type ExperimentType = {
  defaultConfig: ExperimentTypeDefaultConfig[]
  formatMappings: ExperimentTypeFormatMapping[]
  name: string
  status: string
  updatedAt: string
  uuid: string
}

export type PaginatedResponse<T> = {
  data: T[]
  totalElements: number
}

function isExperimentTypeDefaultConfig(
  value: unknown,
): value is ExperimentTypeDefaultConfig {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ExperimentTypeDefaultConfig).name === 'string' &&
    typeof (value as ExperimentTypeDefaultConfig).datatype === 'string' &&
    ((value as ExperimentTypeDefaultConfig).regex === null ||
      typeof (value as ExperimentTypeDefaultConfig).regex === 'string')
  )
}

function isExperimentTypeFormatMapping(
  value: unknown,
): value is ExperimentTypeFormatMapping {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ExperimentTypeFormatMapping).inputFormat === 'string' &&
    typeof (value as ExperimentTypeFormatMapping).outputFormat === 'string'
  )
}

function isExperimentType(value: unknown): value is ExperimentType {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ExperimentType).uuid === 'string' &&
    typeof (value as ExperimentType).name === 'string' &&
    Array.isArray((value as ExperimentType).defaultConfig) &&
    (value as ExperimentType).defaultConfig.every(
      isExperimentTypeDefaultConfig,
    ) &&
    Array.isArray((value as ExperimentType).formatMappings) &&
    (value as ExperimentType).formatMappings.every(
      isExperimentTypeFormatMapping,
    ) &&
    typeof (value as ExperimentType).status === 'string' &&
    typeof (value as ExperimentType).updatedAt === 'string'
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

export async function getExperimentTypes(
  accessToken: string,
): Promise<PaginatedResponse<ExperimentType>> {
  const payload = await ApiService.get<unknown>(
    ServicesUrlEndpoints.GET_EXPERIMENT_TYPES,
    { headers: getAuthenticatedRequestHeaders(accessToken) },
  )

  const experimentTypes = toPaginatedResponse(payload, isExperimentType)

  if (!experimentTypes) {
    throw new ApiServiceError(
      'The experiment type service returned an invalid response.',
      500,
      payload,
    )
  }

  return experimentTypes
}
