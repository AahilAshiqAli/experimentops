import ApiService from '../utils/api.service'
import { getAuthenticatedRequestHeaders } from './jwt.service'
import { ServicesUrlEndpoints } from './servicesEndpointConstant'

export const EXPERIMENT_RUN_LOG_LEVELS = ['INFO', 'ERROR'] as const

export type ExperimentRunLogLevel = (typeof EXPERIMENT_RUN_LOG_LEVELS)[number]

export type ExperimentRunLog = {
  experimentType: string
  level: ExperimentRunLogLevel
  message: string
  sequence: number
  timestamp: string
}

export type ExperimentRunLogListParams = {
  page?: number
  size?: number
}

export type PaginatedExperimentRunLogs = {
  data: ExperimentRunLog[]
  totalElements: number
}

export type ExperimentRunLogDownloadUrl = {
  downloadUrl: string
  expiresAt: string
}

function isExperimentRunLogLevel(
  value: unknown,
): value is ExperimentRunLogLevel {
  return (
    typeof value === 'string' &&
    EXPERIMENT_RUN_LOG_LEVELS.includes(value as ExperimentRunLogLevel)
  )
}

function isExperimentRunLog(value: unknown): value is ExperimentRunLog {
  if (typeof value !== 'object' || value === null) return false

  const log = value as ExperimentRunLog
  return (
    typeof log.sequence === 'number' &&
    typeof log.timestamp === 'string' &&
    isExperimentRunLogLevel(log.level) &&
    typeof log.experimentType === 'string' &&
    typeof log.message === 'string'
  )
}

function isPaginatedExperimentRunLogs(
  value: unknown,
): value is PaginatedExperimentRunLogs {
  return (
    typeof value === 'object' &&
    value !== null &&
    Array.isArray((value as PaginatedExperimentRunLogs).data) &&
    (value as PaginatedExperimentRunLogs).data.every(isExperimentRunLog) &&
    typeof (value as PaginatedExperimentRunLogs).totalElements === 'number'
  )
}

function isExperimentRunLogDownloadUrl(
  value: unknown,
): value is ExperimentRunLogDownloadUrl {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ExperimentRunLogDownloadUrl).downloadUrl === 'string' &&
    typeof (value as ExperimentRunLogDownloadUrl).expiresAt === 'string'
  )
}

export async function getExperimentRunLogs(
  accessToken: string,
  experimentRunUuid: string,
  params: ExperimentRunLogListParams = {},
): Promise<PaginatedExperimentRunLogs> {
  const payload = await ApiService.get<unknown>(
    ServicesUrlEndpoints.GET_EXPERIMENT_RUN_LOGS.replace(
      ':uuid',
      encodeURIComponent(experimentRunUuid),
    ),
    {
      headers: getAuthenticatedRequestHeaders(accessToken),
      params,
    },
  )

  if (!isPaginatedExperimentRunLogs(payload)) {
    throw new Error(
      'The experiment run log service returned an invalid response.',
    )
  }

  return payload
}

export async function getExperimentRunLogDownloadUrl(
  accessToken: string,
  experimentRunUuid: string,
): Promise<ExperimentRunLogDownloadUrl> {
  const payload = await ApiService.get<unknown>(
    ServicesUrlEndpoints.GET_EXPERIMENT_RUN_LOG_DOWNLOAD_URL.replace(
      ':uuid',
      encodeURIComponent(experimentRunUuid),
    ),
    { headers: getAuthenticatedRequestHeaders(accessToken) },
  )

  if (!isExperimentRunLogDownloadUrl(payload)) {
    throw new Error(
      'The experiment run log download service returned an invalid response.',
    )
  }

  return payload
}
