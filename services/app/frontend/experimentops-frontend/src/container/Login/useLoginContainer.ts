import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

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
  const { t } = useTranslation()
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
          : t('auth.messages.signInFailed'),
      )
    }
  }

  const handleForgotPassword = async (email: string) => {
    if (!email) {
      Toaster.error(t('auth.messages.enterResetEmail'))
      return
    }

    try {
      await requestPassword(email)
      Toaster.success(t('auth.messages.checkEmail'))
    } catch (error) {
      Toaster.error(
        error instanceof ApiServiceError
          ? error.message
          : t('auth.messages.requestFailed'),
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
