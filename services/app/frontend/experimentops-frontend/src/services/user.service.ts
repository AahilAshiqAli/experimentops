import ApiService, { ApiServiceError } from '../utils/api.service'
import { getAuthenticatedRequestHeaders, JwtTokenError } from './jwt.service'
import { ServicesUrlEndpoints } from './servicesEndpointConstant'

export type User = {
  email: string
  firstName: string
  lastName: string
  userRole: string
  uuid: string
}

export type CreateUserInput = {
  email: string
  firstName: string
  lastName: string
  userRole: 'RESEARCHER' | 'WORKSPACE_ADMIN'
}

export type UserListPagination = {
  page: number
  size: number
}

export type UserListResponse = {
  data: User[]
  totalElements: number
}

function isUser(value: unknown): value is User {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as User).uuid === 'string' &&
    typeof (value as User).email === 'string' &&
    typeof (value as User).firstName === 'string' &&
    typeof (value as User).lastName === 'string' &&
    typeof (value as User).userRole === 'string'
  )
}

function toUserListResponse(payload: unknown): UserListResponse | null {
  if (
    typeof payload !== 'object' ||
    payload === null ||
    !Array.isArray((payload as UserListResponse).data) ||
    typeof (payload as UserListResponse).totalElements !== 'number'
  ) {
    return null
  }

  const data = (payload as UserListResponse).data
  if (!data.every(isUser)) return null

  return {
    data,
    totalElements: (payload as UserListResponse).totalElements,
  }
}

export async function getUsers(
  accessToken: string,
  pagination: UserListPagination,
): Promise<UserListResponse> {
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
    ServicesUrlEndpoints.GET_USERS,
    {
      headers,
      params: pagination,
    },
  )
  const users = toUserListResponse(payload)

  if (!users) {
    throw new ApiServiceError(
      'The user service returned an invalid response.',
      500,
      payload,
    )
  }

  return users
}

export async function createUser(
  accessToken: string,
  input: CreateUserInput,
): Promise<User> {
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

  const payload = await ApiService.post<unknown, CreateUserInput>(
    ServicesUrlEndpoints.GET_USERS,
    input,
    {
      headers: {
        ...headers,
        'Content-Type': 'application/json',
      },
    },
  )

  if (!isUser(payload)) {
    throw new ApiServiceError(
      'The user service returned an invalid response.',
      500,
      payload,
    )
  }

  return payload
}
