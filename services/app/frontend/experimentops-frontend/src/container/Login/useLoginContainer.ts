import { useLogin } from '../../context-api/logincontext'
import { useMutationLogin } from '../../queries'
import type { LoginCredentials } from '../../services/auth.service'
import { Toaster } from '../../services/toaster.service'
import { ApiServiceError } from '../../utils/api.service'

export function useLoginContainer() {
  const { login } = useLogin()
  const { isPending: isSubmitting, mutateAsync: loginRequest } = useMutationLogin()

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
      Toaster.error(
        error instanceof ApiServiceError
          ? error.message
          : 'Unable to sign in. Please try again.',
      )
    }
  }

  return {
    handleSubmit,
    isSubmitting,
  }
}
