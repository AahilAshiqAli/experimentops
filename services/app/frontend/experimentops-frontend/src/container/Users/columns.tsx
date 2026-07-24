import type { TFunction } from 'i18next'

import type { DataTableColumn } from '../../components/DataTable'
import type { User } from '../../services/user.service'

function formatRole(role: string) {
  return role
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

export function getUserColumns(t: TFunction): DataTableColumn<User>[] {
  return [
    {
      className: 'whitespace-nowrap text-sm font-medium text-secondary',
      header: t('users.name'),
      key: 'name',
      sort: true,
      value: (user) => `${user.firstName} ${user.lastName}`.trim(),
    },
    {
      className: 'whitespace-nowrap text-sm text-slate-600',
      header: t('users.email'),
      key: 'email',
      sort: true,
      value: (user) => user.email,
    },
    {
      className: 'whitespace-nowrap text-sm text-slate-600',
      filter: true,
      header: t('users.role'),
      key: 'userRole',
      sort: true,
      value: (user) =>
        user.userRole === 'RESEARCHER'
          ? t('users.roles.researcher')
          : user.userRole === 'WORKSPACE_ADMIN'
            ? t('users.roles.workspaceAdmin')
            : formatRole(user.userRole),
    },
  ]
}
