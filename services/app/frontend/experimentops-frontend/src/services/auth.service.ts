import ApiService, { ApiServiceError } from '../utils/api.service'
import { ServicesUrlEndpoints } from './servicesEndpointConstant'

export type LoginCredentials = {
  username: string
  password: string
}

export type ForgotPasswordRequest = {
  workspaceName: string
  email: string
}

export type ResetPasswordRequest = {
  token: string
  newPassword: string
}

type PasswordResetRequiredErrorResponse = {
  entityId: string
  errorCode: number
  message: string
}

const PASSWORD_RESET_REQUIRED_ERROR_CODE = 30006

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

export function getPasswordResetToken(error: unknown): string | null {
  if (!(error instanceof ApiServiceError)) return null

  const payload = error.data
  if (
    typeof payload !== 'object' ||
    payload === null ||
    typeof (payload as PasswordResetRequiredErrorResponse).entityId !==
      'string' ||
    typeof (payload as PasswordResetRequiredErrorResponse).errorCode !==
      'number' ||
    typeof (payload as PasswordResetRequiredErrorResponse).message !== 'string'
  ) {
    return null
  }

  const resetError = payload as PasswordResetRequiredErrorResponse
  return resetError.errorCode === PASSWORD_RESET_REQUIRED_ERROR_CODE
    ? resetError.entityId
    : null
}

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

export async function generateForgotPassword(email: string): Promise<void> {
  await ApiService.post<void, ForgotPasswordRequest>(
    ServicesUrlEndpoints.AUTH_FORGOT_PASSWORD_GENERATE,
    {
      workspaceName: WORKSPACE_NAME,
      email,
    },
    {
      headers: {
        'Content-Type': 'application/json',
      },
    },
  )
}

export async function resetPassword(
  request: ResetPasswordRequest,
): Promise<void> {
  await ApiService.post<void, ResetPasswordRequest>(
    ServicesUrlEndpoints.AUTH_RESET_PASSWORD_VERIFY,
    request,
    {
      headers: {
        'Content-Type': 'application/json',
      },
    },
  )
}
