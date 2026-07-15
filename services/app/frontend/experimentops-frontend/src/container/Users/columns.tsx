import type { DataTableColumn } from '../../components/DataTable'
import type { User } from '../../services/user.service'

function formatRole(role: string) {
  return role
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

export const userColumns: DataTableColumn<User>[] = [
  {
    className: 'whitespace-nowrap text-sm font-medium text-secondary',
    header: 'Name',
    key: 'name',
    sort: true,
    value: (user) => `${user.firstName} ${user.lastName}`.trim(),
  },
  {
    className: 'whitespace-nowrap text-sm text-slate-600',
    header: 'Email',
    key: 'email',
    sort: true,
    value: (user) => user.email,
  },
  {
    className: 'whitespace-nowrap text-sm text-slate-600',
    filter: true,
    header: 'Role',
    key: 'userRole',
    sort: true,
    value: (user) => formatRole(user.userRole),
  },
]
