import ApiService, { ApiServiceError } from '../utils/api.service'
import {
  getAuthenticatedRequestHeaders,
  JwtTokenError,
} from './jwt.service'
import { ServicesUrlEndpoints } from './servicesEndpointConstant'

export type Project = {
  description: string
  name: string
  uuid: string
}

export async function getProjects(accessToken: string): Promise<Project[]> {
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
    },
  )

  if (!Array.isArray(payload)) {
    throw new ApiServiceError(
      'The project service returned an invalid response.',
      500,
      payload,
    )
  }

  return payload.filter(
    (project): project is Project =>
      typeof project === 'object' &&
      project !== null &&
      typeof (project as Project).uuid === 'string' &&
      typeof (project as Project).name === 'string' &&
      typeof (project as Project).description === 'string',
  )
}
