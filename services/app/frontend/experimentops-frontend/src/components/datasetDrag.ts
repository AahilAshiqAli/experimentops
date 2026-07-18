import type { DragEvent } from 'react'

export const DATASET_DRAG_TYPE = 'application/x-experimentops-dataset-version'

export function setDatasetDragData(
  event: DragEvent<HTMLElement>,
  datasetVersionUuid: string,
) {
  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData(DATASET_DRAG_TYPE, datasetVersionUuid)
}
