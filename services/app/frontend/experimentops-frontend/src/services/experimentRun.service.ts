import i18n from '../i18n'
import ApiService from '../utils/api.service'
import { getAuthenticatedRequestHeaders } from './jwt.service'
import { ServicesUrlEndpoints } from './servicesEndpointConstant'

export const EXPERIMENT_RUN_STATUSES = [
  'PENDING',
  'RUNNING',
  'SUCCEEDED',
  'FAILED',
] as const

export type ExperimentRunStatus = (typeof EXPERIMENT_RUN_STATUSES)[number]

export type ExperimentRunStep = {
  experimentConfigUuid: string
  inputs: ExperimentRunStepInput[]
  stepCount: number
}

export type ExperimentRunStepInput = {
  file: string
  inputType: 'ARTIFACT' | 'DATASET'
  portName: string
  sourceStepCount?: number
}

type ExperimentRunInput = {
  executionMode: ExperimentRunStep[]
}

export type CreateExperimentRunInput = ExperimentRunInput & {
  name: string
}

export type ValidateExperimentRunInput = ExperimentRunInput

export type ExperimentRun = {
  datasetCount: number
  duration: string
  name: string
  progress: number
  status: ExperimentRunStatus
  uuid: string
}

export type ExperimentRunDataset = {
  datasetVersionUuid: string
  name: string
}

export type ExperimentRunArtifact = {
  portName: string
  uuid: string
}

export type ExperimentRunResolvedInput = {
  fileUuid: string
  format?: string | null
  inputType: 'ARTIFACT' | 'DATASET'
  name: string
  portName: string
}

export type ExperimentRunStepOutput = {
  artifactUuid: string
  downstreamPolicy?: string | null
  format?: string | null
  name: string
  portName: string
  size?: number | null
  status?: string | null
}

export type ExperimentRunDetailStep = {
  experimentConfigUuid: string
  experimentType: string
  inputs: ExperimentRunResolvedInput[]
  outputs: ExperimentRunStepOutput[]
  stepCount: number
}

export type ExperimentRunDetail = {
  artifactCount: number
  completedSteps: number
  createdBy: string
  creationDate: string
  datasets: ExperimentRunDataset[]
  executionMode: ExperimentRunDetailStep[]
  experimentName: string
  experimentUUID: string
  lastUpdated: string
  message: string | null
  name: string
  numSteps: number
  projectName: string
  projectUUID: string
  runArtifacts: ExperimentRunArtifact[]
  status: ExperimentRunStatus
  uuid: string
}

export type ExperimentRunListParams = {
  name?: string
  page?: number
  size?: number
  status?: ExperimentRunStatus[]
}

export type PaginatedExperimentRuns = {
  data: ExperimentRun[]
  totalElements: number
}

export type ExperimentRunStatusResponse = {
  experimentRunUuid: string
  progress: number
  status: ExperimentRunStatus
  duration: string
}

export type ExperimentRunCreationResponse = {
  experimentRunUuid: string
  runDatasetUuid?: string | null
  runDatasetUuids?: string[]
  status: ExperimentRunStatus
}

export type ExperimentRunComparisonAxis = 'CONFIG' | 'DATASET'

export type ExperimentRunComparisonInput = {
  comparisonAxis: ExperimentRunComparisonAxis
  experimentRunUuids: string[]
}

export type ExperimentRunComparisonReport = {
  evaluationReport: Record<string, unknown>
  experimentRunUuid: string
}

export type ExperimentRunComparison = {
  comparisonAxis: ExperimentRunComparisonAxis
  pipelineSignature: string[]
  runs: ExperimentRunComparisonReport[]
}

function isExperimentRunStatus(value: unknown): value is ExperimentRunStatus {
  return (
    typeof value === 'string' &&
    EXPERIMENT_RUN_STATUSES.includes(value as ExperimentRunStatus)
  )
}

function isExperimentRun(value: unknown): value is ExperimentRun {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ExperimentRun).uuid === 'string' &&
    typeof (value as ExperimentRun).name === 'string' &&
    typeof (value as ExperimentRun).progress === 'number' &&
    typeof (value as ExperimentRun).datasetCount === 'number' &&
    typeof (value as ExperimentRun).duration === 'string' &&
    isExperimentRunStatus((value as ExperimentRun).status)
  )
}

function isPaginatedExperimentRuns(
  value: unknown,
): value is PaginatedExperimentRuns {
  return (
    typeof value === 'object' &&
    value !== null &&
    Array.isArray((value as PaginatedExperimentRuns).data) &&
    (value as PaginatedExperimentRuns).data.every(isExperimentRun) &&
    typeof (value as PaginatedExperimentRuns).totalElements === 'number'
  )
}

function isExperimentRunStatusResponse(
  value: unknown,
): value is ExperimentRunStatusResponse {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ExperimentRunStatusResponse).experimentRunUuid ===
      'string' &&
    typeof (value as ExperimentRunStatusResponse).progress === 'number' &&
    isExperimentRunStatus((value as ExperimentRunStatusResponse).status) &&
    typeof (value as ExperimentRunStatusResponse).duration === 'string'
  )
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isExperimentRunComparisonAxis(
  value: unknown,
): value is ExperimentRunComparisonAxis {
  return value === 'CONFIG' || value === 'DATASET'
}

function isExperimentRunComparisonReport(
  value: unknown,
): value is ExperimentRunComparisonReport {
  return (
    isRecord(value) &&
    typeof value.experimentRunUuid === 'string' &&
    isRecord(value.evaluationReport)
  )
}

function isExperimentRunComparison(
  value: unknown,
): value is ExperimentRunComparison {
  return (
    isRecord(value) &&
    isExperimentRunComparisonAxis(value.comparisonAxis) &&
    Array.isArray(value.pipelineSignature) &&
    value.pipelineSignature.every((step) => typeof step === 'string') &&
    Array.isArray(value.runs) &&
    value.runs.every(isExperimentRunComparisonReport)
  )
}

function isExperimentRunDataset(value: unknown): value is ExperimentRunDataset {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ExperimentRunDataset).datasetVersionUuid === 'string' &&
    typeof (value as ExperimentRunDataset).name === 'string'
  )
}

function isExperimentRunArtifact(
  value: unknown,
): value is ExperimentRunArtifact {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ExperimentRunArtifact).uuid === 'string' &&
    typeof (value as ExperimentRunArtifact).portName === 'string'
  )
}

function isExperimentRunResolvedInput(
  value: unknown,
): value is ExperimentRunResolvedInput {
  if (typeof value !== 'object' || value === null) return false
  const input = value as ExperimentRunResolvedInput
  return (
    typeof input.portName === 'string' &&
    (input.inputType === 'ARTIFACT' || input.inputType === 'DATASET') &&
    typeof input.name === 'string' &&
    typeof input.fileUuid === 'string' &&
    (input.format === undefined ||
      input.format === null ||
      typeof input.format === 'string')
  )
}

function isExperimentRunStepOutput(
  value: unknown,
): value is ExperimentRunStepOutput {
  if (typeof value !== 'object' || value === null) return false
  const output = value as ExperimentRunStepOutput
  return (
    typeof output.artifactUuid === 'string' &&
    typeof output.portName === 'string' &&
    typeof output.name === 'string' &&
    (output.format === undefined ||
      output.format === null ||
      typeof output.format === 'string') &&
    (output.size === undefined ||
      output.size === null ||
      typeof output.size === 'number') &&
    (output.status === undefined ||
      output.status === null ||
      typeof output.status === 'string') &&
    (output.downstreamPolicy === undefined ||
      output.downstreamPolicy === null ||
      typeof output.downstreamPolicy === 'string')
  )
}

function isExperimentRunDetailStep(
  value: unknown,
): value is ExperimentRunDetailStep {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ExperimentRunDetailStep).experimentConfigUuid ===
      'string' &&
    typeof (value as ExperimentRunDetailStep).stepCount === 'number' &&
    typeof (value as ExperimentRunDetailStep).experimentType === 'string' &&
    Array.isArray((value as ExperimentRunDetailStep).inputs) &&
    (value as ExperimentRunDetailStep).inputs.every(
      isExperimentRunResolvedInput,
    ) &&
    Array.isArray((value as ExperimentRunDetailStep).outputs) &&
    (value as ExperimentRunDetailStep).outputs.every(isExperimentRunStepOutput)
  )
}

function isExperimentRunDetail(value: unknown): value is ExperimentRunDetail {
  if (typeof value !== 'object' || value === null) return false

  const run = value as ExperimentRunDetail
  return (
    typeof run.uuid === 'string' &&
    typeof run.name === 'string' &&
    isExperimentRunStatus(run.status) &&
    (run.message === null || typeof run.message === 'string') &&
    typeof run.experimentUUID === 'string' &&
    typeof run.experimentName === 'string' &&
    typeof run.projectUUID === 'string' &&
    typeof run.projectName === 'string' &&
    typeof run.creationDate === 'string' &&
    typeof run.lastUpdated === 'string' &&
    typeof run.createdBy === 'string' &&
    typeof run.artifactCount === 'number' &&
    typeof run.completedSteps === 'number' &&
    typeof run.numSteps === 'number' &&
    Array.isArray(run.datasets) &&
    run.datasets.every(isExperimentRunDataset) &&
    Array.isArray(run.runArtifacts) &&
    run.runArtifacts.every(isExperimentRunArtifact) &&
    Array.isArray(run.executionMode) &&
    run.executionMode.every(isExperimentRunDetailStep)
  )
}

export function createExperimentRun(
  accessToken: string,
  experimentUuid: string,
  input: CreateExperimentRunInput,
): Promise<ExperimentRunCreationResponse> {
  return ApiService.post<
    ExperimentRunCreationResponse,
    CreateExperimentRunInput
  >(
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

export async function getExperimentRuns(
  accessToken: string,
  experimentUuid: string,
  params: ExperimentRunListParams = {},
): Promise<PaginatedExperimentRuns> {
  const payload = await ApiService.get<unknown>(
    ServicesUrlEndpoints.GET_EXPERIMENT_RUNS.replace(
      ':experimentUuid',
      encodeURIComponent(experimentUuid),
    ),
    {
      headers: getAuthenticatedRequestHeaders(accessToken),
      params: {
        ...params,
        status: params.status?.length ? params.status.join(',') : undefined,
      },
    },
  )

  if (!isPaginatedExperimentRuns(payload)) {
    throw new Error(i18n.t('serviceErrors.runInvalid'))
  }

  return payload
}

export async function getExperimentRun(
  accessToken: string,
  uuid: string,
): Promise<ExperimentRunDetail> {
  const payload = await ApiService.get<unknown>(
    ServicesUrlEndpoints.GET_EXPERIMENT_RUN.replace(
      ':uuid',
      encodeURIComponent(uuid),
    ),
    { headers: getAuthenticatedRequestHeaders(accessToken) },
  )

  if (!isExperimentRunDetail(payload)) {
    throw new Error(i18n.t('serviceErrors.runDetailInvalid'))
  }

  return payload
}

export async function getExperimentRunStatuses(
  accessToken: string,
  experimentRunUuids: string[],
): Promise<ExperimentRunStatusResponse[]> {
  const payload = await ApiService.post<
    unknown,
    { experimentRunUuids: string[] }
  >(
    ServicesUrlEndpoints.GET_EXPERIMENT_RUN_STATUSES,
    { experimentRunUuids },
    {
      headers: {
        ...getAuthenticatedRequestHeaders(accessToken),
        'Content-Type': 'application/json',
      },
    },
  )

  if (
    !Array.isArray(payload) ||
    !payload.every(isExperimentRunStatusResponse)
  ) {
    throw new Error(i18n.t('serviceErrors.runStatusInvalid'))
  }

  return payload
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

export async function compareExperimentRuns(
  accessToken: string,
  input: ExperimentRunComparisonInput,
): Promise<ExperimentRunComparison> {
  const payload = await ApiService.post<unknown, ExperimentRunComparisonInput>(
    ServicesUrlEndpoints.COMPARE_EXPERIMENT_RUNS,
    input,
    {
      headers: {
        ...getAuthenticatedRequestHeaders(accessToken),
        'Content-Type': 'application/json',
      },
    },
  )

  if (!isExperimentRunComparison(payload)) {
    throw new Error(i18n.t('serviceErrors.runComparisonInvalid'))
  }

  return payload
}
