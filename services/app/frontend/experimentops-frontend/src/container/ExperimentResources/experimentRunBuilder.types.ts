import type { Dataset, DatasetVersion } from '../../services/dataset.service'

export type PipelineConnection = {
  fromUuid: string
  toUuid: string
}

export type BoardPoint = {
  x: number
  y: number
}

export type DrawingConnection = {
  fromUuid: string
  pointer: BoardPoint
}

export type DraggingCard = {
  offset: BoardPoint
  uuid: string
}

export type SelectedDatasetVersion = {
  dataset: Dataset
  version: DatasetVersion
}
