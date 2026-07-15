import { useNavigate } from 'react-router-dom'

import { useLogin } from '../../context-api/logincontext'
import { useMutationForgotPassword, useMutationLogin } from '../../queries'
import {
  getPasswordResetToken,
  type LoginCredentials,
} from '../../services/auth.service'
import { Toaster } from '../../services/toaster.service'
import { ApiServiceError } from '../../utils/api.service'
import { unAuthenticatedRoutesConstant } from '../../routes'

export function useLoginContainer() {
  const navigate = useNavigate()
  const { login } = useLogin()
  const { isPending: isSubmitting, mutateAsync: loginRequest } =
    useMutationLogin()
  const { isPending: isRequestingPassword, mutateAsync: requestPassword } =
    useMutationForgotPassword()

  const handleSubmit = async (credentials: LoginCredentials) => {
    try {
      const authResponse = await loginRequest(credentials)

      login({
        ...authResponse,
        user: {
          email: credentials.username,
          name: credentials.username,
        },
      })
    } catch (error) {
      const resetToken = getPasswordResetToken(error)
      if (resetToken) {
        navigate(
          `${unAuthenticatedRoutesConstant.RESET_PASSWORD}?${new URLSearchParams({ token: resetToken }).toString()}`,
        )
        return
      }

      Toaster.error(
        error instanceof ApiServiceError
          ? error.message
          : 'Unable to sign in. Please try again.',
      )
    }
  }

  const handleForgotPassword = async (email: string) => {
    if (!email) {
      Toaster.error('Enter your email address to reset your password.')
      return
    }

    try {
      await requestPassword(email)
      Toaster.success('Please check your email.')
    } catch (error) {
      Toaster.error(
        error instanceof ApiServiceError
          ? error.message
          : 'Unable to request a password reset. Please try again.',
      )
    }
  }

  return {
    handleForgotPassword,
    handleSubmit,
    isRequestingPassword,
    isSubmitting,
  }
}
