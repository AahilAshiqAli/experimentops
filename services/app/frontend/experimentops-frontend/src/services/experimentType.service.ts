import i18n from '../i18n'
import ApiService, { ApiServiceError } from '../utils/api.service'
import { getAuthenticatedRequestHeaders } from './jwt.service'
import { ServicesUrlEndpoints } from './servicesEndpointConstant'

export type ExperimentTypeDefaultConfig = {
  datatype: 'boolean' | 'list' | 'number' | 'string'
  defaultValue: unknown
  name: string
  regex: string | null
}

export type ExperimentTypeInputContract = {
  dataKind: string
  acceptedFormats: string[]
}

export type ExperimentTypeInputManifest = {
  portName: string
  required: boolean
  cardinality: string
  contract: ExperimentTypeInputContract
}

export type ExperimentTypeInputRelationship = {
  type: string
  ports: string[]
}

export type ExperimentTypeFormatStrategy = {
  type: string
  sourceInputPort?: string | null
  format?: string | null
}

export type ExperimentTypeOutputManifest = {
  name: string
  required: boolean
  dataKind: string
  type: ExperimentTypeFormatStrategy
  downStreamPolicy: string
}

export type ExperimentTypeManifest = {
  inputs: ExperimentTypeInputManifest[]
  inputRelationships: ExperimentTypeInputRelationship[]
  outputs: ExperimentTypeOutputManifest[]
  /** @deprecated Kept for compatibility with existing consumers. */
  inputFormat?: string
  /** @deprecated Kept for compatibility with existing consumers. */
  outputFormat?: string
}

export type ExperimentType = {
  defaultConfig: ExperimentTypeDefaultConfig[]
  formatMappings: ExperimentTypeManifest[]
  name: string
  status: string
  timeWeight: number
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

function isExperimentTypeInputContract(
  value: unknown,
): value is ExperimentTypeInputContract {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ExperimentTypeInputContract).dataKind === 'string' &&
    Array.isArray((value as ExperimentTypeInputContract).acceptedFormats) &&
    (value as ExperimentTypeInputContract).acceptedFormats.every(
      (format) => typeof format === 'string',
    )
  )
}

export function isExperimentTypeManifest(
  value: unknown,
): value is ExperimentTypeManifest {
  if (typeof value !== 'object' || value === null) return false

  const manifest = value as ExperimentTypeManifest
  return (
    Array.isArray(manifest.inputs) &&
    manifest.inputs.every(
      (input) =>
        typeof input === 'object' &&
        input !== null &&
        typeof input.portName === 'string' &&
        typeof input.required === 'boolean' &&
        typeof input.cardinality === 'string' &&
        isExperimentTypeInputContract(input.contract),
    ) &&
    Array.isArray(manifest.inputRelationships) &&
    manifest.inputRelationships.every(
      (relationship) =>
        typeof relationship === 'object' &&
        relationship !== null &&
        typeof relationship.type === 'string' &&
        Array.isArray(relationship.ports) &&
        relationship.ports.every((port) => typeof port === 'string'),
    ) &&
    Array.isArray(manifest.outputs) &&
    manifest.outputs.every(
      (output) =>
        typeof output === 'object' &&
        output !== null &&
        typeof output.name === 'string' &&
        typeof output.required === 'boolean' &&
        typeof output.dataKind === 'string' &&
        typeof output.type === 'object' &&
        output.type !== null &&
        typeof output.type.type === 'string' &&
        (typeof output.type.sourceInputPort === 'undefined' ||
          output.type.sourceInputPort === null ||
          typeof output.type.sourceInputPort === 'string') &&
        (typeof output.type.format === 'undefined' ||
          output.type.format === null ||
          typeof output.type.format === 'string') &&
        typeof output.downStreamPolicy === 'string',
    )
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
    (value as ExperimentType).formatMappings.every(isExperimentTypeManifest) &&
    typeof (value as ExperimentType).status === 'string' &&
    typeof (value as ExperimentType).timeWeight === 'number' &&
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

  const data = (payload as PaginatedResponse<T>).data
  if (!data.every(isItem)) return null

  return {
    data,
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
      i18n.t('serviceErrors.experimentTypeInvalid'),
      500,
      payload,
    )
  }

  return experimentTypes
}
