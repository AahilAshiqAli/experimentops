import { Link, NavLink, Outlet } from 'react-router-dom'

import { APP_NAME, ROUTES } from '../constants'
import { useLogin } from '../context-api/logincontext'

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  [
    'rounded-full px-4 py-2 text-sm font-medium transition',
    isActive
      ? 'bg-primary text-white shadow-glow'
      : 'text-slate-300 hover:bg-white/5 hover:text-white',
  ].join(' ')

export function AppShell() {
  const { isAuthenticated, user, logout } = useLogin()

  return (
    <div className="min-h-screen bg-background text-slate-100">
      <div className="mx-auto flex min-h-screen w-full max-w-6xl flex-col px-4 py-6 sm:px-6 lg:px-8">
        <header className="rounded-3xl border border-white/10 bg-slate-950/80 px-4 py-4 shadow-2xl shadow-black/30 backdrop-blur">
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <Link
              to={ROUTES.home}
              className="text-lg font-semibold tracking-tight"
            >
              <span className="text-primary">{APP_NAME}</span>
              <span className="text-slate-400">.studio</span>
            </Link>

            <nav className="flex flex-wrap items-center gap-2">
              <NavLink to={ROUTES.home} className={navLinkClass}>
                Home
              </NavLink>
              <NavLink to={ROUTES.dashboard} className={navLinkClass}>
                Dashboard
              </NavLink>
              <NavLink to={ROUTES.login} className={navLinkClass}>
                Login
              </NavLink>
            </nav>
          </div>

          <div className="mt-4 flex flex-col gap-3 border-t border-white/10 pt-4 text-sm text-slate-300 sm:flex-row sm:items-center sm:justify-between">
            <p>
              {isAuthenticated && user
                ? `Signed in as ${user.name} (${user.email})`
                : 'Not signed in yet'}
            </p>

            {isAuthenticated ? (
              <button
                type="button"
                onClick={logout}
                className="inline-flex items-center justify-center rounded-full border border-white/10 bg-white/5 px-4 py-2 font-medium text-white transition hover:bg-white/10"
              >
                Sign out
              </button>
            ) : null}
          </div>
        </header>

        <main className="flex-1 py-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
