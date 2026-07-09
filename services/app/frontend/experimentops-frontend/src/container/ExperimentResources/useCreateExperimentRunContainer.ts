import { useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'

import {
  useMutationCreateExperimentRun,
  useQueryExperimentTypes,
  useQueryProject,
} from '../../queries'
import type { Dataset, DatasetVersion } from '../../services/dataset.service'
import type { ExperimentConfig } from '../../services/experimentConfig.service'
import { Toaster } from '../../services/toaster.service'
import type { PipelineConnection } from './experimentRunBuilder.types'

function getPipelineOrder(
  configs: ExperimentConfig[],
  connections: PipelineConnection[],
) {
  if (!configs.length) return []
  if (configs.length === 1) return configs

  const configByUuid = new Map(configs.map((config) => [config.uuid, config]))
  const boardUuids = new Set(configs.map((config) => config.uuid))
  const incoming = new Set(connections.map((connection) => connection.toUuid))
  const outgoing = new Map(
    connections.map((connection) => [connection.fromUuid, connection.toUuid]),
  )
  const startUuid = configs.find((config) => !incoming.has(config.uuid))?.uuid

  if (!startUuid) return []

  const order: ExperimentConfig[] = []
  const visited = new Set<string>()
  let currentUuid: string | undefined = startUuid

  while (
    currentUuid &&
    boardUuids.has(currentUuid) &&
    !visited.has(currentUuid)
  ) {
    const config = configByUuid.get(currentUuid)
    if (!config) break

    order.push(config)
    visited.add(currentUuid)
    currentUuid = outgoing.get(currentUuid)
  }

  return order.length === configs.length ? order : []
}

export function useCreateExperimentRunContainer() {
  const { experimentUuid, projectUuid } = useParams<{
    experimentUuid: string
    projectUuid: string
  }>()
  const navigate = useNavigate()
  const projectQuery = useQueryProject(projectUuid)
  const experimentTypesQuery = useQueryExperimentTypes()
  const createRunMutation = useMutationCreateExperimentRun(experimentUuid ?? '')
  const [isDatasetPickerOpen, setIsDatasetPickerOpen] = useState(false)
  const [selectedDataset, setSelectedDataset] = useState<Dataset | null>(null)
  const [selectedDatasetVersion, setSelectedDatasetVersion] =
    useState<DatasetVersion | null>(null)
  const [isConfigPickerOpen, setIsConfigPickerOpen] = useState(false)
  const [selectedExperimentType, setSelectedExperimentType] = useState('')
  const [selectedConfigs, setSelectedConfigs] = useState<ExperimentConfig[]>([])
  const [boardConfigUuids, setBoardConfigUuids] = useState<string[]>([])
  const [connections, setConnections] = useState<PipelineConnection[]>([])

  const boardConfigs = boardConfigUuids
    .map((uuid) => selectedConfigs.find((config) => config.uuid === uuid))
    .filter((config): config is ExperimentConfig => Boolean(config))
  const availableConfigs = selectedConfigs.filter(
    (config) => !boardConfigUuids.includes(config.uuid),
  )
  const pipelineOrder = getPipelineOrder(boardConfigs, connections)
  const isPipelineReady =
    boardConfigs.length > 0 &&
    pipelineOrder.length === boardConfigs.length &&
    (boardConfigs.length === 1 ||
      connections.length === boardConfigs.length - 1)

  const handleSelectConfig = (config: ExperimentConfig) => {
    setSelectedConfigs((current) =>
      current.some((selected) => selected.uuid === config.uuid)
        ? current
        : [...current, config],
    )
  }

  const handleMoveConfigToBoard = (config: ExperimentConfig) => {
    setBoardConfigUuids((current) =>
      current.includes(config.uuid) ? current : [...current, config.uuid],
    )
  }

  const handleRemoveConfig = (configUuid: string) => {
    setSelectedConfigs((current) =>
      current.filter((config) => config.uuid !== configUuid),
    )
    setBoardConfigUuids((current) =>
      current.filter((uuid) => uuid !== configUuid),
    )
    setConnections((current) =>
      current.filter(
        (connection) =>
          connection.fromUuid !== configUuid &&
          connection.toUuid !== configUuid,
      ),
    )
  }

  const handleRemoveFromBoard = (configUuid: string) => {
    setBoardConfigUuids((current) =>
      current.filter((uuid) => uuid !== configUuid),
    )
    setConnections((current) =>
      current.filter(
        (connection) =>
          connection.fromUuid !== configUuid &&
          connection.toUuid !== configUuid,
      ),
    )
  }

  const handleConnect = (fromUuid: string, targetUuid: string) => {
    if (fromUuid === targetUuid) return

    const nextConnections = connections.filter(
      (connection) =>
        connection.fromUuid !== fromUuid && connection.toUuid !== targetUuid,
    )

    setConnections([...nextConnections, { fromUuid, toUuid: targetUuid }])
  }

  const handleRemoveConnection = (connection: PipelineConnection) => {
    setConnections((current) =>
      current.filter(
        (item) =>
          !(
            item.fromUuid === connection.fromUuid &&
            item.toUuid === connection.toUuid
          ),
      ),
    )
  }

  const handleSelectDatasetVersion = (
    dataset: Dataset,
    version: DatasetVersion,
  ) => {
    setSelectedDataset(dataset)
    setSelectedDatasetVersion(version)
    setIsDatasetPickerOpen(false)
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selectedDatasetVersion || !isPipelineReady) return

    createRunMutation.mutate(
      {
        datasetVersionUuid: selectedDatasetVersion.datasetVersionUuid,
        executionMode: pipelineOrder.map((config, index) => ({
          experimentConfigUuid: config.uuid,
          stepCount: index + 1,
        })),
      },
      {
        onError: (error) => {
          Toaster.error(
            error instanceof Error
              ? error.message
              : 'Unable to create the experiment run.',
          )
        },
        onSuccess: () => {
          Toaster.success('Experiment run created successfully.')
          navigate(
            `/projects/${projectUuid}/experiments/${experimentUuid}/experiment-runs`,
          )
        },
      },
    )
  }

  return {
    availableConfigs,
    boardConfigs,
    connections,
    createRunMutation,
    experimentTypesQuery,
    experimentUuid,
    handleConnect,
    handleMoveConfigToBoard,
    handleRemoveConfig,
    handleRemoveConnection,
    handleRemoveFromBoard,
    handleSelectConfig,
    handleSelectDatasetVersion,
    handleSubmit,
    isConfigPickerOpen,
    isDatasetPickerOpen,
    isPipelineReady,
    pipelineOrder,
    projectQuery,
    projectUuid,
    selectedConfigs,
    selectedDataset,
    selectedDatasetVersion,
    selectedExperimentType,
    setIsConfigPickerOpen,
    setIsDatasetPickerOpen,
    setSelectedExperimentType,
  }
}
