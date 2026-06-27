export type JwtTokenClaims = {
  email?: string
  exp?: number
  family_name?: string
  given_name?: string
  iat?: number
  name?: string
  preferred_username?: string
  realm_access?: {
    roles?: string[]
  }
  role?: string
  roles?: string[]
  user_uuid?: string
  workspace_uuid?: string
}

export class JwtTokenError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'JwtTokenError'
  }
}

const KEYCLOAK_SYSTEM_ROLES = new Set(['offline_access', 'uma_authorization'])

export function decodeJwt(accessToken: string): JwtTokenClaims {
  try {
    const payload = accessToken.split('.')[1]

    if (!payload) {
      throw new Error('Token payload is missing')
    }

    const normalizedPayload = payload
      .replace(/-/g, '+')
      .replace(/_/g, '/')
      .padEnd(Math.ceil(payload.length / 4) * 4, '=')
    const payloadBytes = Uint8Array.from(atob(normalizedPayload), (character) =>
      character.charCodeAt(0),
    )

    return JSON.parse(new TextDecoder().decode(payloadBytes)) as JwtTokenClaims
  } catch {
    throw new JwtTokenError('Your session is invalid. Please sign in again.')
  }
}

export function isJwtExpired(claims: JwtTokenClaims) {
  return typeof claims.exp === 'number' && claims.exp * 1000 <= Date.now()
}

export function getApplicationRole(claims: JwtTokenClaims) {
  if (claims.role) {
    return claims.role
  }

  const roles = [...(claims.roles ?? []), ...(claims.realm_access?.roles ?? [])]
  const uniqueRoles = [...new Set(roles)]

  return (
    uniqueRoles.find((role) => role.startsWith('WORKSPACE_')) ??
    uniqueRoles.find(
      (role) =>
        !KEYCLOAK_SYSTEM_ROLES.has(role) && !role.startsWith('default-roles-'),
    ) ??
    null
  )
}

function requireClaim(value: string | undefined, claimName: string) {
  if (!value) {
    throw new JwtTokenError(
      `Your session is missing ${claimName}. Please sign in again.`,
    )
  }

  return value
}

export function getAuthenticatedRequestHeaders(
  accessToken: string,
  claims = decodeJwt(accessToken),
  role = getApplicationRole(claims),
) {
  const userUuid = requireClaim(claims.user_uuid, 'the user identifier')
  const workspaceUuid = requireClaim(
    claims.workspace_uuid,
    'the workspace identifier',
  )
  const userName = requireClaim(
    claims.email ?? claims.preferred_username ?? claims.name,
    'the user name',
  )

  if (!role) {
    throw new JwtTokenError(
      'Your session is missing an application role. Please sign in again.',
    )
  }

  return {
    Accept: 'application/json',
    Authorization: `Bearer ${accessToken}`,
    requestUuid: globalThis.crypto.randomUUID(),
    'X-Token-C-User-Uuid': userUuid,
    'X-Token-C-Name': userName,
    'X-Token-C-User-Role': role,
    'X-Token-C-Workspace-Uuid': workspaceUuid,
  }
}
