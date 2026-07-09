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
