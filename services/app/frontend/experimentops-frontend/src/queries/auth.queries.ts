import { useMutation } from '@tanstack/react-query'

import {
  generateForgotPassword,
  login,
  resetPassword,
  type LoginCredentials,
  type ResetPasswordRequest,
} from '../services/auth.service'

export function useMutationLogin() {
  return useMutation({
    mutationFn: (credentials: LoginCredentials) => login(credentials),
  })
}

export function useMutationForgotPassword() {
  return useMutation({
    mutationFn: (email: string) => generateForgotPassword(email),
  })
}

export function useMutationResetPassword() {
  return useMutation({
    mutationFn: (request: ResetPasswordRequest) => resetPassword(request),
  })
}
