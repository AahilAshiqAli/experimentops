import { useMutation } from '@tanstack/react-query'

import { login, type LoginCredentials } from '../services/auth.service'

export function useMutationLogin() {
  return useMutation({
    mutationFn: (credentials: LoginCredentials) => login(credentials),
  })
}
