import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useLogin } from '../context-api/logincontext'
import {
  createUser,
  getUsers,
  type CreateUserInput,
  type UserListPagination,
} from '../services/user.service'
import { PERMISSIONS_KEYS } from '../utils'

export function useQueryUsers(pagination: UserListPagination) {
  const { accessToken, hasPermission } = useLogin()
  const canListUsers = hasPermission(PERMISSIONS_KEYS.USER.GET_USER)

  return useQuery({
    enabled: Boolean(accessToken && canListUsers),
    queryFn: () => getUsers(accessToken as string, pagination),
    queryKey: ['users', pagination],
  })
}

export function useMutationCreateUser() {
  const { accessToken } = useLogin()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: CreateUserInput) =>
      createUser(accessToken as string, input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users'] }),
  })
}
