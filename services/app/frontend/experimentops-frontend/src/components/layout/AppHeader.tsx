import { Link } from 'react-router-dom'

import { APP_NAME } from '../../constants'
import { authenticatedRoutesConstant } from '../../routes'

const workspaceName = (
  import.meta.env.VITE_AUTH_WORKSPACE_NAME ?? 'AahilAshiqResearchLab'
)
  .replace(/([a-z])([A-Z])/g, '$1 $2')
  .replace(/[-_]/g, ' ')

type AppHeaderProps = {
  onMenuToggle: () => void
  onLogout: () => void
  role: string | null
  user: {
    email: string
    name: string
  } | null
}

export function AppHeader({
  onMenuToggle,
  onLogout,
  role,
  user,
}: AppHeaderProps) {
  const initials = (user?.name || user?.email || '?')
    .split(/[\s@._-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('')
  const roleLabel = role
    ?.toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')

  return (
    <header className="fixed inset-x-0 top-0 z-50 h-20 border-b border-slate-200 bg-white/95 backdrop-blur">
      <div className="flex h-full items-center gap-3 px-4 sm:px-6">
        <button
          aria-label="Open navigation"
          className="inline-flex rounded-md p-2 text-slate-600 hover:bg-slate-100 hover:text-primary lg:hidden"
          onClick={onMenuToggle}
          type="button"
        >
          <svg aria-hidden="true" className="h-6 w-6" fill="none" viewBox="0 0 24 24">
            <path d="M4 7h16M4 12h16M4 17h16" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
          </svg>
        </button>

        <Link className="shrink-0 text-lg font-semibold tracking-tight" to={authenticatedRoutesConstant.HOME}>
          <span className="text-primary">{APP_NAME}</span>
          <span className="text-secondary">.studio</span>
        </Link>

        <div className="hidden h-7 w-px bg-slate-200 sm:block" />
        <p className="hidden max-w-56 truncate text-sm font-medium text-slate-500 sm:block">
          {workspaceName}
        </p>

        <div className="ml-auto flex items-center gap-3">
          <details className="relative">
            <summary className="flex cursor-pointer list-none items-center gap-2 rounded-md p-1.5 transition hover:bg-slate-100 [&::-webkit-details-marker]:hidden">
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-primary text-xs font-semibold text-white">
                {initials}
              </span>
              <span className="hidden text-left sm:block">
                <span className="block max-w-40 truncate text-sm font-medium text-secondary">{user?.name ?? 'Unknown user'}</span>
                <span className="block max-w-40 truncate text-xs text-slate-500">{roleLabel ?? 'Unknown role'}</span>
              </span>
              <svg aria-hidden="true" className="hidden h-4 w-4 text-slate-500 sm:block" fill="none" viewBox="0 0 24 24">
                <path d="m6 9 6 6 6-6" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
              </svg>
            </summary>
            <div className="absolute right-0 z-50 mt-2 w-64 overflow-hidden rounded-lg border border-slate-200 bg-white py-2 shadow-card">
              <div className="px-4 py-3">
                <p className="text-sm font-medium text-secondary">{user?.name ?? 'Unknown user'}</p>
                <p className="mt-1 truncate text-xs text-slate-500">{user?.email ?? 'unknown'}</p>
                <p className="mt-1 text-xs font-medium text-primary">
                  {roleLabel ?? 'Unknown role'}
                </p>
              </div>
              <div className="border-t border-slate-100 px-2 pt-2">
                <button
                  className="flex w-full items-center gap-2 rounded-md px-3 py-2 text-left text-sm font-medium text-slate-600 transition hover:bg-slate-100 hover:text-primary"
                  onClick={onLogout}
                  type="button"
                >
                  <svg aria-hidden="true" className="h-4 w-4" fill="none" viewBox="0 0 24 24">
                    <path d="M10 17l5-5-5-5M15 12H3m9-8h6a3 3 0 0 1 3 3v10a3 3 0 0 1-3 3h-6" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
                  </svg>
                  Sign out
                </button>
              </div>
            </div>
          </details>
        </div>
      </div>
    </header>
  )
}
