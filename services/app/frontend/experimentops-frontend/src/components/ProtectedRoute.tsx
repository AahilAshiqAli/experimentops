import { Navigate, Outlet } from 'react-router-dom'
import type { ReactNode } from 'react'

import { ROUTES } from '../constants'
import { useLogin } from '../context-api/logincontext'

type ProtectedRouteProps = {
  children?: ReactNode
}

export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated } = useLogin()

  if (!isAuthenticated) {
    return <Navigate to={ROUTES.login} replace />
  }

  return children ?? <Outlet />
}
