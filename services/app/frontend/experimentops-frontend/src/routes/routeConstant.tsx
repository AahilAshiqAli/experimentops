import type { RouteObject } from 'react-router-dom'

import { AdminLayout } from '../container/AdminLayout/AdminLayout'
import { Dashboard } from '../container/Dashboard/Dashboard'
import { Login } from '../container/Login/Login'
import { Projects } from '../container/Projects/Projects'

export const unAuthenticatedRoutesConstant = {
  LOGIN: '/login',
} as const

export const authenticatedRoutesConstant = {
  HOME: '/',
  DASHBOARD: '/dashboard',
  PROJECTS: '/projects',
} as const

export const UNAUTHENTICATED_ROUTES: RouteObject[] = [
  {
    path: unAuthenticatedRoutesConstant.LOGIN,
    element: <Login />,
  },
]

export const AUTHENTICATED_ROUTES: RouteObject[] = [
  {
    path: authenticatedRoutesConstant.HOME,
    element: <AdminLayout />,
    children: [
      {
        index: true,
        element: <Dashboard />,
      },
      {
        path: authenticatedRoutesConstant.DASHBOARD,
        element: <Dashboard />,
      },
      {
        path: authenticatedRoutesConstant.PROJECTS,
        element: <Projects />,
      },
    ],
  },
]
