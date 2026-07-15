import { NavLink } from 'react-router-dom'

import { authenticatedRoutesConstant } from '../../routes'
import { useLogin } from '../../context-api/logincontext'
import { PERMISSIONS_KEYS } from '../../utils'

type AppSidebarProps = {
  collapsed: boolean
  isMobileOpen: boolean
  onNavigate: () => void
  onToggle: () => void
}

export function AppSidebar({
  collapsed,
  isMobileOpen,
  onNavigate,
  onToggle,
}: AppSidebarProps) {
  const { hasPermission } = useLogin()
  const canListUsers = hasPermission(PERMISSIONS_KEYS.USER.GET_USER)

  return (
    <aside
      aria-label="Primary navigation"
      className={`fixed bottom-0 left-0 top-20 z-40 flex w-64 flex-col border-r border-slate-200 bg-white transition-all duration-200 lg:sticky lg:top-20 lg:h-[calc(100vh-5rem)] lg:translate-x-0 ${
        collapsed ? 'lg:w-20' : 'lg:w-64'
      } ${isMobileOpen ? 'translate-x-0' : '-translate-x-full'}`}
    >
      <div className="flex items-center justify-between border-b border-slate-100 px-4 py-4 lg:justify-end">
        <span
          className={`text-xs font-semibold uppercase tracking-[0.18em] text-slate-400 ${collapsed ? 'lg:hidden' : ''}`}
        >
          Navigation
        </span>
        <button
          aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          className="hidden rounded-md p-2 text-slate-500 transition hover:bg-slate-100 hover:text-primary lg:inline-flex"
          onClick={onToggle}
          type="button"
        >
          <svg
            aria-hidden="true"
            className={`h-5 w-5 transition-transform ${collapsed ? 'rotate-180' : ''}`}
            fill="none"
            viewBox="0 0 24 24"
          >
            <path
              d="m15 18-6-6 6-6"
              stroke="currentColor"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="2"
            />
          </svg>
        </button>
      </div>

      <nav className="flex-1 p-3">
        <NavLink
          className={({ isActive }) =>
            `flex items-center gap-3 rounded-md px-3 py-3 text-sm font-medium transition ${
              isActive
                ? 'bg-primary text-white shadow-glow'
                : 'text-slate-600 hover:bg-slate-100 hover:text-secondary'
            } ${collapsed ? 'lg:justify-center lg:px-2' : ''}`
          }
          onClick={onNavigate}
          to={authenticatedRoutesConstant.DASHBOARD}
        >
          <svg
            aria-hidden="true"
            className="h-5 w-5 shrink-0"
            fill="none"
            viewBox="0 0 24 24"
          >
            <path
              d="M4 13h6V4H4v9Zm0 7h6v-4H4v4Zm10 0h6v-9h-6v9Zm0-16v4h6V4h-6Z"
              fill="currentColor"
            />
          </svg>
          <span className={collapsed ? 'lg:hidden' : ''}>Dashboard</span>
        </NavLink>
        <NavLink
          className={({ isActive }) =>
            `mt-1 flex items-center gap-3 rounded-md px-3 py-3 text-sm font-medium transition ${
              isActive
                ? 'bg-primary text-white shadow-glow'
                : 'text-slate-600 hover:bg-slate-100 hover:text-secondary'
            } ${collapsed ? 'lg:justify-center lg:px-2' : ''}`
          }
          onClick={onNavigate}
          to={authenticatedRoutesConstant.PROJECTS}
        >
          <svg
            aria-hidden="true"
            className="h-5 w-5 shrink-0"
            fill="none"
            viewBox="0 0 24 24"
          >
            <path
              d="M4 6.5A2.5 2.5 0 0 1 6.5 4h3l2 2h6A2.5 2.5 0 0 1 20 8.5v8a2.5 2.5 0 0 1-2.5 2.5h-11A2.5 2.5 0 0 1 4 16.5v-10Z"
              stroke="currentColor"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="1.8"
            />
          </svg>
          <span className={collapsed ? 'lg:hidden' : ''}>Projects</span>
        </NavLink>
        {canListUsers ? (
          <NavLink
            className={({ isActive }) =>
              `mt-1 flex items-center gap-3 rounded-md px-3 py-3 text-sm font-medium transition ${
                isActive
                  ? 'bg-primary text-white shadow-glow'
                  : 'text-slate-600 hover:bg-slate-100 hover:text-secondary'
              } ${collapsed ? 'lg:justify-center lg:px-2' : ''}`
            }
            onClick={onNavigate}
            to={authenticatedRoutesConstant.USERS}
          >
            <svg
              aria-hidden="true"
              className="h-5 w-5 shrink-0"
              fill="none"
              viewBox="0 0 24 24"
            >
              <path
                d="M16 20v-1a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v1m17-9a4 4 0 1 0 0-8m-7 8a4 4 0 1 0 0-8"
                stroke="currentColor"
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="1.8"
              />
            </svg>
            <span className={collapsed ? 'lg:hidden' : ''}>Users</span>
          </NavLink>
        ) : null}
      </nav>
    </aside>
  )
}
