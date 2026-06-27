import ApiService, { ApiServiceError } from '../utils/api.service'
import { ServicesUrlEndpoints } from './servicesEndpointConstant'

export type LoginCredentials = {
  username: string
  password: string
}

export type AuthLoginResponse = {
  access_token: string
  expires_in?: number
  refresh_token?: string
  refresh_expires_in?: number
  token_type?: string
  session_state?: string
  scope?: string
}

const WORKSPACE_NAME =
  import.meta.env.VITE_AUTH_WORKSPACE_NAME ?? 'AahilAshiqResearchLab'

export async function login(credentials: LoginCredentials) {
  const authResponse = await ApiService.post<
    AuthLoginResponse,
    LoginCredentials
  >(
    ServicesUrlEndpoints.AUTH_LOGIN.replace(
      ':workspaceName',
      encodeURIComponent(WORKSPACE_NAME),
    ),
    credentials,
    {
      headers: {
        'Content-Type': 'application/json',
      },
    },
  )

  if (!authResponse.access_token) {
    throw new ApiServiceError(
      'The login service did not return an access token.',
      500,
      authResponse,
    )
  }

  return authResponse
}
