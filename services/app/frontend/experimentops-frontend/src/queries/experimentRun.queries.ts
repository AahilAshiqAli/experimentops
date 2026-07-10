import { useMutation, useQueryClient } from '@tanstack/react-query'

import { useLogin } from '../context-api/logincontext'
import {
  createExperimentRun,
  type CreateExperimentRunInput,
  validateExperimentRun,
  type ValidateExperimentRunInput,
} from '../services/experimentRun.service'

export function useMutationCreateExperimentRun(experimentUuid: string) {
  const { accessToken } = useLogin()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: CreateExperimentRunInput) =>
      createExperimentRun(accessToken as string, experimentUuid, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['experiments', experimentUuid, 'runs'],
      })
    },
  })
}

export function useMutationValidateExperimentRun(experimentUuid: string) {
  const { accessToken } = useLogin()

  return useMutation({
    mutationFn: (input: ValidateExperimentRunInput) =>
      validateExperimentRun(accessToken as string, experimentUuid, input),
  })
}
