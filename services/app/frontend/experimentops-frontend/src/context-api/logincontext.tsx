/* eslint-disable react-refresh/only-export-components */

import { useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import {
  useCallback,
  createContext,
  useEffect,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'

import i18n from '../i18n'
import type { AuthLoginResponse } from '../services/auth.service'
import {
  decodeJwt,
  getApplicationRole,
  isJwtExpired,
  type JwtTokenClaims,
} from '../services/jwt.service'
import { getRoles, type Permission } from '../services/role.service'
import { Toaster } from '../services/toaster.service'
import { ApiServiceError } from '../utils/api.service'
import { clearPermissionStore, setPermissionStore } from '../utils/permission'

export type LoginUser = {
  email: string
  name: string
}

export type LoginSession = AuthLoginResponse & {
  user: LoginUser
}

type LoginContextValue = {
  user: LoginUser | null
  accessToken: string | null
  refreshToken: string | null
  tokenParsed: JwtTokenClaims | null
  role: string | null
  permissions: Permission[]
  hasPermission: (code: string) => boolean
  isAuthenticated: boolean
  isAuthenticating: boolean
  login: (session: LoginSession) => void
  logout: () => void
}

const LoginContext = createContext<LoginContextValue | undefined>(undefined)
const AUTH_SESSION_STORAGE_KEY = 'experimentops_auth_session'
const PERMISSIONS_STORAGE_KEY = 'experimentops_permissions'

function getStoredSession(): LoginSession | null {
  try {
    const storedSession = localStorage.getItem(AUTH_SESSION_STORAGE_KEY)

    if (!storedSession) {
      return null
    }

    const session = JSON.parse(storedSession) as LoginSession

    return session.access_token && session.user ? session : null
  } catch {
    localStorage.removeItem(AUTH_SESSION_STORAGE_KEY)
    return null
  }
}

function getStoredPermissions(): Permission[] {
  try {
    const storedPermissions = localStorage.getItem(PERMISSIONS_STORAGE_KEY)
    const permissions: unknown = storedPermissions
      ? JSON.parse(storedPermissions)
      : []

    return Array.isArray(permissions)
      ? permissions.filter(
          (permission): permission is Permission =>
            typeof permission === 'object' &&
            permission !== null &&
            typeof (permission as Permission).code === 'string' &&
            typeof (permission as Permission).permissionName === 'string',
        )
      : []
  } catch {
    localStorage.removeItem(PERMISSIONS_STORAGE_KEY)
    return []
  }
}

async function initializeRequests(
  accessToken: string,
  tokenParsed: JwtTokenClaims,
  role: string,
  signal: AbortSignal,
) {
  const roles = await getRoles(accessToken, tokenParsed, role, signal)
  const permissionByRole = roles.find((item) => item.roleName === role)

  if (!permissionByRole) {
    throw new ApiServiceError(i18n.t('session.noPermissions', { role }), 403)
  }

  return permissionByRole.permissionList
}

type LoginProviderProps = {
  children: ReactNode
}

export function LoginProvider({ children }: LoginProviderProps) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [session, setSession] = useState<LoginSession | null>(getStoredSession)
  const [tokenParsed, setTokenParsed] = useState<JwtTokenClaims | null>(null)
  const [role, setRole] = useState<string | null>(null)
  const [permissions, setPermissions] = useState<Permission[]>(() => {
    const storedPermissions = getStoredPermissions()
    setPermissionStore(storedPermissions)
    return storedPermissions
  })
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [isAuthenticating, setIsAuthenticating] = useState(Boolean(session))

  const logout = useCallback(() => {
    localStorage.removeItem(AUTH_SESSION_STORAGE_KEY)
    localStorage.removeItem(PERMISSIONS_STORAGE_KEY)
    clearPermissionStore()
    setSession(null)
    setTokenParsed(null)
    setRole(null)
    setPermissions([])
    setIsAuthenticated(false)
    setIsAuthenticating(false)
    queryClient.clear()
  }, [queryClient])

  const login = useCallback((nextSession: LoginSession) => {
    localStorage.setItem(AUTH_SESSION_STORAGE_KEY, JSON.stringify(nextSession))
    setIsAuthenticated(false)
    setIsAuthenticating(true)
    setSession(nextSession)
  }, [])

  useEffect(() => {
    const accessToken = session?.access_token

    if (!accessToken) {
      return
    }

    const abortController = new AbortController()

    const initializeSession = async () => {
      await Promise.resolve()

      if (abortController.signal.aborted) {
        return
      }

      try {
        const parsedToken = decodeJwt(accessToken)
        const parsedRole = getApplicationRole(parsedToken)

        if (isJwtExpired(parsedToken)) {
          throw new Error(t('session.expired'))
        }

        if (!parsedRole) {
          throw new Error(t('session.noRole'))
        }

        const permissionList = await initializeRequests(
          accessToken,
          parsedToken,
          parsedRole,
          abortController.signal,
        )

        if (abortController.signal.aborted) {
          return
        }

        localStorage.setItem(
          PERMISSIONS_STORAGE_KEY,
          JSON.stringify(permissionList),
        )
        setPermissionStore(permissionList)
        setTokenParsed(parsedToken)
        setRole(parsedRole)
        setPermissions(permissionList)
        setIsAuthenticated(true)
        setIsAuthenticating(false)
      } catch (error) {
        if (abortController.signal.aborted) {
          return
        }

        Toaster.error(
          error instanceof Error ? error.message : t('session.initialize'),
        )
        logout()
      }
    }

    void initializeSession()

    return () => abortController.abort()
  }, [logout, session, t])

  const user = useMemo<LoginUser | null>(() => {
    if (!session) {
      return null
    }

    return {
      email:
        tokenParsed?.email ??
        tokenParsed?.preferred_username ??
        session.user.email,
      name:
        tokenParsed?.name ??
        ([tokenParsed?.given_name, tokenParsed?.family_name]
          .filter(Boolean)
          .join(' ') ||
          session.user.name),
    }
  }, [session, tokenParsed])

  const hasPermission = useCallback(
    (code: string) =>
      permissions.some((permission) => permission.code === code),
    [permissions],
  )

  const value = useMemo<LoginContextValue>(
    () => ({
      user,
      accessToken: session?.access_token ?? null,
      refreshToken: session?.refresh_token ?? null,
      tokenParsed,
      role,
      permissions,
      hasPermission,
      isAuthenticated,
      isAuthenticating,
      login,
      logout,
    }),
    [
      hasPermission,
      isAuthenticated,
      isAuthenticating,
      login,
      logout,
      permissions,
      role,
      session,
      tokenParsed,
      user,
    ],
  )

  return <LoginContext.Provider value={value}>{children}</LoginContext.Provider>
}

export function useLogin() {
  const context = useContext(LoginContext)

  if (context === undefined) {
    throw new Error('useLogin must be used inside a LoginProvider')
  }

  return context
}
