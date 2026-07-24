import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import { AppHeader } from '../../components/layout/AppHeader'
import { AppSidebar } from '../../components/layout/AppSidebar'
import { useLogin } from '../../context-api/logincontext'
import { useExperimentRunStatusMonitor } from '../ExperimentRun/hooks/useExperimentRunStatusMonitor'

// AdminLayout is the shared frame for authenticated pages. It contains the parts that should remain visible while a user moves between screens—header, sidebar, user controls—and its <Outlet /> is the changing page area.

export function AdminLayout() {
  const { t } = useTranslation()
  const { user, role, logout } = useLogin()
  useExperimentRunStatusMonitor()
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(true)
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false)
  return (
    <div className="min-h-screen bg-background text-secondary">
      <AppHeader
        onLogout={logout}
        onMenuToggle={() => setIsMobileSidebarOpen((open) => !open)}
        role={role}
        user={user}
      />
      {isMobileSidebarOpen && (
        <button
          aria-label={t('common.navigation.close')}
          className="fixed inset-0 top-20 z-30 bg-secondary/20 lg:hidden"
          onClick={() => setIsMobileSidebarOpen(false)}
          type="button"
        />
      )}
      <div className="flex min-h-screen pt-20">
        <AppSidebar
          collapsed={isSidebarCollapsed}
          isMobileOpen={isMobileSidebarOpen}
          onNavigate={() => setIsMobileSidebarOpen(false)}
          onToggle={() => setIsSidebarCollapsed((collapsed) => !collapsed)}
        />
        <main className="min-w-0 flex-1 p-4 sm:p-5 lg:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
