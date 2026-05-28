/* eslint-disable react-refresh/only-export-components */

import { useNavigate, createBrowserRouter } from 'react-router-dom'

import { APP_NAME, ROUTES } from '../constants'
import { AppShell, ProtectedRoute } from '../components'
import { useLogin } from '../context-api/logincontext'
import { useDocumentTitle } from '../hooks'

function HomePage() {
  useDocumentTitle('Welcome')

  return (
    <section className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
      <div className="rounded-3xl border border-white/10 bg-white/5 p-8 shadow-2xl shadow-black/20">
        <p className="text-sm font-semibold uppercase tracking-[0.3em] text-secondary">
          Starter shell
        </p>
        <h1 className="mt-4 text-4xl font-semibold tracking-tight text-white sm:text-6xl">
          {APP_NAME} is ready for routing, queries, and auth context.
        </h1>
        <p className="mt-4 max-w-2xl text-base leading-7 text-slate-300 sm:text-lg">
          Tailwind is wired, React Router is active, TanStack Query has a shared
          client, and the app structure now has dedicated folders for constants,
          context, routes, hooks, utils, components, and queries.
        </p>
      </div>

      <div className="rounded-3xl border border-primary/30 bg-surface p-8 shadow-glow">
        <h2 className="text-2xl font-semibold text-white">Project status</h2>
        <ul className="mt-4 space-y-3 text-sm text-slate-300">
          <li>- ESLint was already present in the starter template.</li>
          <li>- Prettier was not configured until now.</li>
          <li>- TanStack Query and React Router are now added.</li>
          <li>- Tailwind uses custom primary and secondary colors.</li>
        </ul>
      </div>
    </section>
  )
}

function DashboardPage() {
  useDocumentTitle('Dashboard')

  return (
    <section className="rounded-3xl border border-white/10 bg-white/5 p-8">
      <h1 className="text-3xl font-semibold text-white">Dashboard</h1>
      <p className="mt-3 max-w-2xl text-slate-300">
        This protected route is a placeholder for authenticated screens.
      </p>
    </section>
  )
}

function LoginPage() {
  const navigate = useNavigate()
  const { login, isAuthenticated } = useLogin()

  useDocumentTitle('Login')

  const handleLogin = () => {
    login({
      name: 'Experiment Ops User',
      email: 'user@experimentops.local',
    })

    navigate(ROUTES.dashboard)
  }

  return (
    <section className="grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
      <div className="rounded-3xl border border-white/10 bg-white/5 p-8">
        <h1 className="text-3xl font-semibold text-white">Login</h1>
        <p className="mt-3 text-slate-300">
          The context is intentionally lightweight so we can plug in real auth
          later.
        </p>
        <button
          type="button"
          onClick={handleLogin}
          disabled={isAuthenticated}
          className="mt-6 inline-flex rounded-full bg-primary px-5 py-3 font-medium text-white transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isAuthenticated ? 'Already signed in' : 'Sign in'}
        </button>
      </div>

      <div className="rounded-3xl border border-secondary/30 bg-surface p-8">
        <h2 className="text-2xl font-semibold text-white">What is ready</h2>
        <p className="mt-3 text-slate-300">
          The app now has a clean place for queries, hooks, utilities, and
          shared constants.
        </p>
      </div>
    </section>
  )
}

function NotFoundPage() {
  useDocumentTitle('Not found')

  return (
    <section className="rounded-3xl border border-white/10 bg-white/5 p-8 text-center">
      <h1 className="text-3xl font-semibold text-white">Page not found</h1>
      <p className="mt-3 text-slate-300">
        The route you requested does not exist.
      </p>
    </section>
  )
}

export const router = createBrowserRouter([
  {
    element: <AppShell />,
    children: [
      {
        path: ROUTES.home,
        element: <HomePage />,
      },
      {
        path: ROUTES.login,
        element: <LoginPage />,
      },
      {
        path: ROUTES.dashboard,
        element: (
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '*',
        element: <NotFoundPage />,
      },
    ],
  },
])
