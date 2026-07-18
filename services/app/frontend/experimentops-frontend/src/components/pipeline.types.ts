import type { Dataset, DatasetVersion } from '../services/dataset.service'

export type PipelineConnection = {
  sourceConfigUuid: string
  sourceOutputName: string
  targetConfigUuid: string
  targetInputPortName: string
}

export type DatasetBinding = {
  datasetUuid: string
  datasetVersionUuid: string
  targetConfigUuid: string
  targetInputPortName: string
}

export type SelectedDatasetVersion = {
  dataset: Dataset
  version: DatasetVersion
}

export type ConnectionDraft = Omit<PipelineConnection, never>

export type DatasetBindingTarget = Pick<
  DatasetBinding,
  'targetConfigUuid' | 'targetInputPortName'
>
