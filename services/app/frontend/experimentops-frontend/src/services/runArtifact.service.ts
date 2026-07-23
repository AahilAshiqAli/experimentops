import i18n from '../i18n'
import ApiService from '../utils/api.service'
import { getAuthenticatedRequestHeaders } from './jwt.service'
import { ServicesUrlEndpoints } from './servicesEndpointConstant'

export type RunArtifact = {
  artifactType: string
  downstreamPolicy: string
  experimentType: string
  format: string
  portName: string
  size: number
  status: string
  stepCount: number
  uuid: string
}

export type RunArtifactDownloadUrl = {
  expiresAt: string
  url: string
}

function isRunArtifact(value: unknown): value is RunArtifact {
  if (typeof value !== 'object' || value === null) return false

  const artifact = value as RunArtifact
  return (
    typeof artifact.uuid === 'string' &&
    typeof artifact.artifactType === 'string' &&
    typeof artifact.experimentType === 'string' &&
    typeof artifact.format === 'string' &&
    typeof artifact.size === 'number' &&
    typeof artifact.stepCount === 'number' &&
    typeof artifact.portName === 'string' &&
    typeof artifact.status === 'string' &&
    typeof artifact.downstreamPolicy === 'string'
  )
}

function normalizeRunArtifacts(payload: unknown): RunArtifact[] | null {
  if (payload === null || payload === undefined) return []

  const artifacts = Array.isArray(payload)
    ? payload
    : typeof payload === 'object' &&
        payload !== null &&
        Array.isArray((payload as { data?: unknown }).data)
      ? (payload as { data: unknown[] }).data
      : [payload]

  return artifacts.every(isRunArtifact) ? artifacts : null
}

export async function getRunArtifacts(
  accessToken: string,
  experimentRunUuid: string,
): Promise<RunArtifact[]> {
  const payload = await ApiService.get<unknown>(
    ServicesUrlEndpoints.GET_RUN_ARTIFACTS.replace(
      ':experimentRunUuid',
      encodeURIComponent(experimentRunUuid),
    ),
    { headers: getAuthenticatedRequestHeaders(accessToken) },
  )
  const artifacts = normalizeRunArtifacts(payload)

  if (!artifacts) {
    throw new Error(i18n.t('serviceErrors.runArtifactInvalid'))
  }

  return artifacts
}

export async function getRunArtifactDownloadUrl(
  accessToken: string,
  artifactUuid: string,
): Promise<RunArtifactDownloadUrl> {
  const payload = await ApiService.get<unknown>(
    ServicesUrlEndpoints.GET_RUN_ARTIFACT_DOWNLOAD_URL.replace(
      ':artifactUuid',
      encodeURIComponent(artifactUuid),
    ),
    { headers: getAuthenticatedRequestHeaders(accessToken) },
  )

  if (typeof payload !== 'object' || payload === null) {
    throw new Error(i18n.t('serviceErrors.artifactDownloadInvalid'))
  }

  const response = payload as {
    downloadUrl?: unknown
    expiresAt?: unknown
    url?: unknown
  }
  const url = response.url ?? response.downloadUrl
  if (typeof url !== 'string' || typeof response.expiresAt !== 'string') {
    throw new Error(i18n.t('serviceErrors.artifactDownloadInvalid'))
  }

  return { expiresAt: response.expiresAt, url }
}
