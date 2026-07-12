import ApiService from '../utils/api.service'
import { getAuthenticatedRequestHeaders } from './jwt.service'
import { ServicesUrlEndpoints } from './servicesEndpointConstant'

export const EXPERIMENT_RUN_STATUSES = [
  'PENDING',
  'RUNNING',
  'SUCCEEDED',
  'FAILED',
] as const

export type ExperimentRunStatus = (typeof EXPERIMENT_RUN_STATUSES)[number]

export type ExperimentRunStep = {
  experimentConfigUuid: string
  stepCount: number
}

export type CreateExperimentRunInput = {
  datasetVersionUuid: string
  executionMode: ExperimentRunStep[]
}

export type ValidateExperimentRunInput = CreateExperimentRunInput

export type ExperimentRun = {
  datasetCount: number
  duration: string
  executionMode: ExperimentRunStep[]
  name: string
  progress: number
  status: ExperimentRunStatus
  uuid: string
}

export type ExperimentRunListParams = {
  name?: string
  page?: number
  size?: number
  status?: ExperimentRunStatus[]
}

export type PaginatedExperimentRuns = {
  data: ExperimentRun[]
  totalElements: number
}

export type ExperimentRunStatusResponse = {
  experimentRunUuid: string
  progress: number
  status: ExperimentRunStatus
}

export type ExperimentRunCreationResponse = {
  experimentRunUuid: string
  runDatasetUuid: string
  status: ExperimentRunStatus
}

function isExperimentRunStatus(value: unknown): value is ExperimentRunStatus {
  return (
    typeof value === 'string' &&
    EXPERIMENT_RUN_STATUSES.includes(value as ExperimentRunStatus)
  )
}

function isExperimentRunStep(value: unknown): value is ExperimentRunStep {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ExperimentRunStep).experimentConfigUuid === 'string' &&
    typeof (value as ExperimentRunStep).stepCount === 'number'
  )
}

function isExperimentRun(value: unknown): value is ExperimentRun {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ExperimentRun).uuid === 'string' &&
    typeof (value as ExperimentRun).name === 'string' &&
    typeof (value as ExperimentRun).progress === 'number' &&
    typeof (value as ExperimentRun).datasetCount === 'number' &&
    typeof (value as ExperimentRun).duration === 'string' &&
    isExperimentRunStatus((value as ExperimentRun).status) &&
    Array.isArray((value as ExperimentRun).executionMode) &&
    (value as ExperimentRun).executionMode.every(isExperimentRunStep)
  )
}

function isPaginatedExperimentRuns(
  value: unknown,
): value is PaginatedExperimentRuns {
  return (
    typeof value === 'object' &&
    value !== null &&
    Array.isArray((value as PaginatedExperimentRuns).data) &&
    (value as PaginatedExperimentRuns).data.every(isExperimentRun) &&
    typeof (value as PaginatedExperimentRuns).totalElements === 'number'
  )
}

function isExperimentRunStatusResponse(
  value: unknown,
): value is ExperimentRunStatusResponse {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ExperimentRunStatusResponse).experimentRunUuid ===
      'string' &&
    typeof (value as ExperimentRunStatusResponse).progress === 'number' &&
    isExperimentRunStatus((value as ExperimentRunStatusResponse).status)
  )
}

export function createExperimentRun(
  accessToken: string,
  experimentUuid: string,
  input: CreateExperimentRunInput,
): Promise<ExperimentRunCreationResponse> {
  return ApiService.post<
    ExperimentRunCreationResponse,
    CreateExperimentRunInput
  >(
    ServicesUrlEndpoints.CREATE_EXPERIMENT_RUN.replace(
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

export async function getExperimentRuns(
  accessToken: string,
  experimentUuid: string,
  params: ExperimentRunListParams = {},
): Promise<PaginatedExperimentRuns> {
  const payload = await ApiService.get<unknown>(
    ServicesUrlEndpoints.GET_EXPERIMENT_RUNS.replace(
      ':experimentUuid',
      encodeURIComponent(experimentUuid),
    ),
    {
      headers: getAuthenticatedRequestHeaders(accessToken),
      params: {
        ...params,
        status: params.status?.length ? params.status.join(',') : undefined,
      },
    },
  )

  if (!isPaginatedExperimentRuns(payload)) {
    throw new Error('The experiment run service returned an invalid response.')
  }

  return payload
}

export async function getExperimentRunStatuses(
  accessToken: string,
  experimentRunUuids: string[],
): Promise<ExperimentRunStatusResponse[]> {
  const payload = await ApiService.post<
    unknown,
    { experimentRunUuids: string[] }
  >(
    ServicesUrlEndpoints.GET_EXPERIMENT_RUN_STATUSES,
    { experimentRunUuids },
    {
      headers: {
        ...getAuthenticatedRequestHeaders(accessToken),
        'Content-Type': 'application/json',
      },
    },
  )

  if (
    !Array.isArray(payload) ||
    !payload.every(isExperimentRunStatusResponse)
  ) {
    throw new Error(
      'The experiment run status service returned an invalid response.',
    )
  }

  return payload
}

export function validateExperimentRun(
  accessToken: string,
  experimentUuid: string,
  input: ValidateExperimentRunInput,
) {
  return ApiService.post<unknown, ValidateExperimentRunInput>(
    ServicesUrlEndpoints.VALIDATE_EXPERIMENT_RUN.replace(
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
