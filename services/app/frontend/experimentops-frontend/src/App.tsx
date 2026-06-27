import { Navigate, RouterProvider, createBrowserRouter } from 'react-router-dom'

import { useLogin } from './context-api/logincontext'
import {
  AUTHENTICATED_ROUTES,
  UNAUTHENTICATED_ROUTES,
  authenticatedRoutesConstant,
  unAuthenticatedRoutesConstant,
} from './routes'

function App() {
  const { isAuthenticated, isAuthenticating } = useLogin()

  if (isAuthenticating) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background text-secondary">
        <div className="text-center">
          <div className="mx-auto h-10 w-10 animate-spin rounded-full border-4 border-slate-200 border-t-primary" />
          <p className="mt-4 text-sm font-medium text-slate-600">
            Loading your workspace…
          </p>
        </div>
      </div>
    )
  }

  const router = createBrowserRouter([
    ...(isAuthenticated ? AUTHENTICATED_ROUTES : UNAUTHENTICATED_ROUTES),
    {
      path: '*',
      element: (
        <Navigate
          to={
            isAuthenticated
              ? authenticatedRoutesConstant.HOME
              : unAuthenticatedRoutesConstant.LOGIN
          }
          replace
        />
      ),
    },
  ])

  return <RouterProvider router={router} />
}

export default App
