import { useState } from 'react'

import { DataTable } from '../../components/DataTable'
import { useLogin } from '../../context-api/logincontext'
import { useDocumentTitle } from '../../hooks'
import { useMutationCreateUser, useQueryUsers } from '../../queries'
import type { CreateUserInput } from '../../services/user.service'
import { Toaster } from '../../services/toaster.service'
import { PERMISSIONS_KEYS } from '../../utils'
import { SectionState } from '../ProjectDetails/projectDetails.shared'
import { getErrorMessage } from '../ProjectDetails/projectDetails.utils'
import { userColumns } from './columns'
import { CreateUserDialog } from './CreateUserDialog'

const USERS_PER_PAGE = 20

export function Users() {
  useDocumentTitle('Users')

  const { hasPermission } = useLogin()
  const [page, setPage] = useState(1)
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const canListUsers = hasPermission(PERMISSIONS_KEYS.USER.GET_USER)
  const canAddUser = hasPermission(PERMISSIONS_KEYS.USER.ADD_USER)
  const usersQuery = useQueryUsers({ page: page - 1, size: USERS_PER_PAGE })
  const createUserMutation = useMutationCreateUser()
  const totalUsers = usersQuery.data?.totalElements ?? 0
  const totalPages = Math.max(1, Math.ceil(totalUsers / USERS_PER_PAGE))
  const activePage = Math.min(page, totalPages)

  const handleCreateUser = (input: CreateUserInput) => {
    createUserMutation.mutate(input, {
      onSuccess: () => {
        setIsCreateOpen(false)
        Toaster.success('User created successfully.')
      },
      onError: (error) => {
        Toaster.error(
          getErrorMessage(error, 'Unable to create user. Please try again.'),
        )
      },
    })
  }

  return (
    <section>
      <div className="mb-3 border-b border-slate-200 pb-3">
        <h1 className="font-heading text-2xl font-semibold text-secondary">
          Users
        </h1>
      </div>

      {!canListUsers ? (
        <SectionState message="You do not have permission to view users." />
      ) : usersQuery.error ? (
        <div className="rounded-lg border border-red-200 bg-red-50 px-5 py-4 text-sm text-red-700">
          {getErrorMessage(
            usersQuery.error,
            'Unable to load users. Please try again.',
          )}
        </div>
      ) : (
        <DataTable
          columns={userColumns}
          data={usersQuery.data?.data ?? []}
          emptyMessage="No users have been added to this workspace yet."
          getRowKey={(user) => user.uuid}
          isLoading={usersQuery.isLoading}
          pagination={{
            onPageChange: setPage,
            page: activePage,
            totalItems: totalUsers,
            totalPages,
          }}
          searchPlaceholder="Search users..."
          toolbarEnd={
            canAddUser ? (
              <button
                className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90"
                onClick={() => setIsCreateOpen(true)}
                type="button"
              >
                + Add User
              </button>
            ) : null
          }
        />
      )}

      {isCreateOpen ? (
        <CreateUserDialog
          isPending={createUserMutation.isPending}
          onClose={() => setIsCreateOpen(false)}
          onSubmit={handleCreateUser}
        />
      ) : null}
    </section>
  )
}
