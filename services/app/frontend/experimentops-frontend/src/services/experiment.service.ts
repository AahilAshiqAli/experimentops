import ApiService, { ApiServiceError } from '../utils/api.service'
import { getAuthenticatedRequestHeaders } from './jwt.service'
import { ServicesUrlEndpoints } from './servicesEndpointConstant'

export type Experiment = {
  configCount: number
  createdAt: string
  description: string
  experimentType?: string
  experimentUuid: string
  name: string
  runCount: number
  status?: string
}

export type CreateExperimentInput = {
  description: string
  experimentType: string
  name: string
}

export type CreatedExperiment = CreateExperimentInput & {
  projectUuid: string
  uuid: string
}

export type PaginationParams = {
  page?: number
  size?: number
}

export type PaginatedResponse<T> = {
  data: T[]
  totalElements: number
}

function isExperiment(value: unknown): value is Experiment {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as Experiment).experimentUuid === 'string' &&
    typeof (value as Experiment).name === 'string' &&
    typeof (value as Experiment).description === 'string' &&
    (typeof (value as Experiment).experimentType === 'string' ||
      typeof (value as Experiment).status === 'string') &&
    typeof (value as Experiment).configCount === 'number' &&
    typeof (value as Experiment).runCount === 'number' &&
    typeof (value as Experiment).createdAt === 'string'
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

export async function getProjectExperiments(
  accessToken: string,
  projectUuid: string,
  pagination?: PaginationParams,
): Promise<PaginatedResponse<Experiment>> {
  const payload = await ApiService.get<unknown>(
    ServicesUrlEndpoints.GET_PROJECT_EXPERIMENTS.replace(
      ':projectUuid',
      encodeURIComponent(projectUuid),
    ),
    {
      headers: getAuthenticatedRequestHeaders(accessToken),
      params: pagination,
    },
  )

  const experiments = toPaginatedResponse(payload, isExperiment)

  if (!experiments) {
    throw new ApiServiceError(
      'The experiment service returned an invalid response.',
      500,
      payload,
    )
  }

  return experiments
}

export async function createProjectExperiment(
  accessToken: string,
  projectUuid: string,
  input: CreateExperimentInput,
) {
  return ApiService.post<CreatedExperiment, CreateExperimentInput>(
    ServicesUrlEndpoints.GET_PROJECT_EXPERIMENTS.replace(
      ':projectUuid',
      encodeURIComponent(projectUuid),
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
