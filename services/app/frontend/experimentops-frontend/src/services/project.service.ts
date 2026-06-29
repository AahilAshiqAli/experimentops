import ApiService, { ApiServiceError } from '../utils/api.service'
import { getAuthenticatedRequestHeaders, JwtTokenError } from './jwt.service'
import { ServicesUrlEndpoints } from './servicesEndpointConstant'

export type Project = {
  creationDate: string
  description: string
  name: string
  uuid: string
}

export type ProjectSummary = {
  createdAt: string
  datasetCount: number
  datasetVersionCount: number
  description: string
  experimentConfigCount: number
  experimentCount: number
  experimentRunCount: number
  name: string
  projectUuid: string
}

export type UpdateProjectInput = {
  description: string
  name: string
}

export type PaginationParams = {
  page?: number
  size?: number
}

export type PaginatedResponse<T> = {
  data: T[]
  totalElements: number
}

function isProjectSummary(value: unknown): value is ProjectSummary {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ProjectSummary).projectUuid === 'string' &&
    typeof (value as ProjectSummary).name === 'string' &&
    typeof (value as ProjectSummary).description === 'string' &&
    typeof (value as ProjectSummary).createdAt === 'string' &&
    typeof (value as ProjectSummary).datasetCount === 'number' &&
    typeof (value as ProjectSummary).experimentCount === 'number' &&
    typeof (value as ProjectSummary).experimentConfigCount === 'number' &&
    typeof (value as ProjectSummary).datasetVersionCount === 'number' &&
    typeof (value as ProjectSummary).experimentRunCount === 'number'
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

function isProject(value: unknown): value is Project {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as Project).uuid === 'string' &&
    typeof (value as Project).name === 'string' &&
    typeof (value as Project).description === 'string' &&
    typeof (value as Project).creationDate === 'string'
  )
}

export async function getProjects(
  accessToken: string,
  pagination?: PaginationParams,
): Promise<PaginatedResponse<Project>> {
  let headers: Record<string, string>

  try {
    headers = getAuthenticatedRequestHeaders(accessToken)
  } catch (error) {
    throw new ApiServiceError(
      error instanceof JwtTokenError
        ? error.message
        : 'Your session is invalid. Please sign in again.',
      401,
    )
  }

  const payload = await ApiService.get<unknown>(
    ServicesUrlEndpoints.GET_PROJECTS,
    {
      headers,
      params: pagination,
    },
  )

  const projects = toPaginatedResponse(payload, isProject)

  if (!projects) {
    throw new ApiServiceError(
      'The project service returned an invalid response.',
      500,
      payload,
    )
  }

  return projects
}

export async function getProjectSummary(
  accessToken: string,
  projectUuid: string,
): Promise<ProjectSummary> {
  const payload = await ApiService.get<unknown>(
    ServicesUrlEndpoints.GET_PROJECT.replace(
      ':projectUuid',
      encodeURIComponent(projectUuid),
    ),
    { headers: getAuthenticatedRequestHeaders(accessToken) },
  )

  if (!isProjectSummary(payload)) {
    throw new ApiServiceError(
      'The project service returned an invalid response.',
      500,
      payload,
    )
  }

  return payload
}

export async function updateProject(
  accessToken: string,
  projectUuid: string,
  input: UpdateProjectInput,
) {
  return ApiService.put<Project, UpdateProjectInput>(
    ServicesUrlEndpoints.GET_PROJECT.replace(
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
