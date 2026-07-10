import ApiService from '../utils/api.service'
import { getAuthenticatedRequestHeaders } from './jwt.service'
import { ServicesUrlEndpoints } from './servicesEndpointConstant'

export type ExperimentRunStep = {
  experimentConfigUuid: string
  stepCount: number
}

export type CreateExperimentRunInput = {
  datasetVersionUuid: string
  executionMode: ExperimentRunStep[]
}

export type ValidateExperimentRunInput = CreateExperimentRunInput

export function createExperimentRun(
  accessToken: string,
  experimentUuid: string,
  input: CreateExperimentRunInput,
) {
  return ApiService.post<unknown, CreateExperimentRunInput>(
    ServicesUrlEndpoints.CREATE_EXPERIMENT_RUN.replace(
      ':experimentUuid',
      encodeURIComponent(experimentUuid),
    ),
    input,
    {
      headers: {
        ...getAuthenticatedRequestHeaders(accessToken),
        'Content-Type': 'application/json',
      },
    },
  )
}

export function validateExperimentRun(
  accessToken: string,
  experimentUuid: string,
  input: ValidateExperimentRunInput,
) {
  return ApiService.post<unknown, ValidateExperimentRunInput>(
    ServicesUrlEndpoints.VALIDATE_EXPERIMENT_RUN.replace(
      ':experimentUuid',
      encodeURIComponent(experimentUuid),
    ),
    input,
    {
      headers: {
        ...getAuthenticatedRequestHeaders(accessToken),
        'Content-Type': 'application/json',
      },
    },
  )
}
