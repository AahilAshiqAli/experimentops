import i18n from '../i18n'
import ApiService, { ApiServiceError } from '../utils/api.service'
import {
  getAuthenticatedRequestHeaders,
  type JwtTokenClaims,
} from './jwt.service'
import { ServicesUrlEndpoints } from './servicesEndpointConstant'

export type Permission = {
  code: string
  permissionName: string
}

export type Role = {
  permissionList: Permission[]
  roleName: string
}

function isPermission(value: unknown): value is Permission {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as Permission).code === 'string' &&
    typeof (value as Permission).permissionName === 'string'
  )
}

function toRole(value: unknown): Role | null {
  if (
    typeof value !== 'object' ||
    value === null ||
    typeof (value as Role).roleName !== 'string' ||
    !Array.isArray((value as Role).permissionList)
  ) {
    return null
  }

  return {
    roleName: (value as Role).roleName,
    permissionList: (value as Role).permissionList.filter(isPermission),
  }
}

export async function getRoles(
  accessToken: string,
  tokenParsed: JwtTokenClaims,
  role: string,
  signal?: AbortSignal,
) {
  const payload = await ApiService.get<unknown>(
    ServicesUrlEndpoints.GET_ROLES,
    {
      headers: getAuthenticatedRequestHeaders(accessToken, tokenParsed, role),
      signal,
    },
  )

  if (!Array.isArray(payload)) {
    throw new ApiServiceError(i18n.t('serviceErrors.roleInvalid'), 500, payload)
  }

  return payload.map(toRole).filter((item): item is Role => item !== null)
}
