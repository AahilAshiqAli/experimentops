import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import type {
  ConnectionDraft,
  DatasetBinding,
  DatasetBindingTarget,
  PipelineConnection,
  SelectedDatasetVersion,
} from '../../components/pipeline.types'
import {
  useMutationCreateExperimentRun,
  useMutationGetExperimentConfigsByUuids,
  useMutationValidateExperimentRun,
  useQueryDataset,
  useQueryExperimentConfigs,
  useQueryProject,
  useQueryProjectDatasets,
} from '../../queries'
import type { Dataset, DatasetVersion } from '../../services/dataset.service'
import type { ExperimentConfig } from '../../services/experimentConfig.service'
import type { ExperimentType } from '../../services/experimentType.service'
import { Toaster } from '../../services/toaster.service'
import {
  buildExecutionMode,
  getConnectionError,
  getDatasetBindingError,
  validatePipeline,
} from './pipeline'

export function useExperimentRunContainer() {
  const { t } = useTranslation()
  const { experimentUuid, projectUuid } = useParams<{
    experimentUuid: string
    projectUuid: string
  }>()
  const navigate = useNavigate()
  const projectQuery = useQueryProject(projectUuid)
  const createRunMutation = useMutationCreateExperimentRun(experimentUuid ?? '')
  const configDetailsMutation = useMutationGetExperimentConfigsByUuids(
    experimentUuid ?? '',
  )
  const validateRunMutation = useMutationValidateExperimentRun(
    experimentUuid ?? '',
  )
  const [isDatasetPickerOpen, setIsDatasetPickerOpen] = useState(false)
  const [name, setName] = useState('')
  const [selectedDatasets, setSelectedDatasets] = useState<
    SelectedDatasetVersion[]
  >([])
  const [datasetBindings, setDatasetBindings] = useState<DatasetBinding[]>([])
  const [isConfigPickerOpen, setIsConfigPickerOpen] = useState(false)
  const [selectedExperimentType, setSelectedExperimentType] =
    useState<ExperimentType | null>(null)
  const [selectedConfigs, setSelectedConfigs] = useState<ExperimentConfig[]>([])
  const [boardConfigUuids, setBoardConfigUuids] = useState<string[]>([])
  const [connections, setConnections] = useState<PipelineConnection[]>([])
  const [datasetPickerPage, setDatasetPickerPage] = useState(1)
  const [datasetPickerSearch, setDatasetPickerSearch] = useState('')
  const [openedDataset, setOpenedDataset] = useState<Dataset | null>(null)

  const datasetsQuery = useQueryProjectDatasets(projectUuid, {
    page: datasetPickerPage - 1,
    size: 9,
  })
  const datasetVersionsQuery = useQueryDataset(
    projectUuid,
    openedDataset?.datasetUuid,
    {
      page: 0,
      scanStatus: 'COMPLETED',
      size: 100,
    },
  )
  const configPickerQuery = useQueryExperimentConfigs(
    experimentUuid,
    {
      experimentType: selectedExperimentType?.name,
      page: 0,
      size: 100,
    },
    { enabled: Boolean(isConfigPickerOpen && selectedExperimentType) },
  )

  const boardConfigs = useMemo(
    () =>
      boardConfigUuids
        .map((uuid) => selectedConfigs.find((config) => config.uuid === uuid))
        .filter((config): config is ExperimentConfig => Boolean(config)),
    [boardConfigUuids, selectedConfigs],
  )
  const availableConfigs = useMemo(
    () =>
      selectedConfigs.filter(
        (config) => !boardConfigUuids.includes(config.uuid),
      ),
    [boardConfigUuids, selectedConfigs],
  )
  const pipelineValidation = useMemo(
    () =>
      validatePipeline(
        boardConfigs,
        connections,
        datasetBindings,
        selectedDatasets,
      ),
    [boardConfigs, connections, datasetBindings, selectedDatasets],
  )
  const pipelineOrder = pipelineValidation.order
  const isPipelineReady = pipelineValidation.errors.length === 0
  const executionMode = useMemo(
    () =>
      isPipelineReady
        ? buildExecutionMode(pipelineOrder, connections, datasetBindings)
        : [],
    [connections, datasetBindings, isPipelineReady, pipelineOrder],
  )

  const datasets = datasetsQuery.data?.data ?? []
  const filteredDatasets = datasetPickerSearch.trim()
    ? datasets.filter((dataset) =>
        dataset.name
          .toLowerCase()
          .includes(datasetPickerSearch.trim().toLowerCase()),
      )
    : datasets
  const totalDatasets = datasetPickerSearch
    ? filteredDatasets.length
    : (datasetsQuery.data?.totalElements ?? 0)
  const datasetPickerTotalPages = Math.max(1, Math.ceil(totalDatasets / 9))
  const validateRunInput = useMemo(
    () => (isPipelineReady ? { executionMode } : null),
    [executionMode, isPipelineReady],
  )
  const validateRun = validateRunMutation.mutate
  const resetRunValidation = validateRunMutation.reset

  useEffect(() => {
    if (!validateRunInput) {
      resetRunValidation()
      return
    }
    const timeoutId = window.setTimeout(() => {
      validateRun(validateRunInput)
    }, 350)
    return () => window.clearTimeout(timeoutId)
  }, [resetRunValidation, validateRun, validateRunInput])

  const handleSelectConfig = async (config: ExperimentConfig) => {
    if (configDetailsMutation.isPending) return

    try {
      const details = await configDetailsMutation.mutateAsync([config.uuid])
      const matchingDetails = details.find(
        (detail) => detail.uuid === config.uuid,
      )
      if (!matchingDetails) {
        throw new Error(t('runs.errors.configMissing'))
      }
      const hydratedConfig: ExperimentConfig = {
        ...config,
        ...matchingDetails,
      }
      setSelectedConfigs((current) =>
        current.some((selected) => selected.uuid === hydratedConfig.uuid)
          ? current.map((selected) =>
              selected.uuid === hydratedConfig.uuid ? hydratedConfig : selected,
            )
          : [...current, hydratedConfig],
      )
      setIsConfigPickerOpen(false)
    } catch (error) {
      Toaster.error(
        error instanceof Error ? error.message : t('runs.errors.configDetails'),
      )
    }
  }

  const handleMoveConfigToBoard = (config: ExperimentConfig) => {
    setBoardConfigUuids((current) =>
      current.includes(config.uuid) ? current : [...current, config.uuid],
    )
  }

  const removeConfigBindings = (configUuid: string) => {
    setConnections((current) =>
      current.filter(
        (connection) =>
          connection.sourceConfigUuid !== configUuid &&
          connection.targetConfigUuid !== configUuid,
      ),
    )
    setDatasetBindings((current) =>
      current.filter((binding) => binding.targetConfigUuid !== configUuid),
    )
  }

  const handleRemoveConfig = (configUuid: string) => {
    setSelectedConfigs((current) =>
      current.filter((config) => config.uuid !== configUuid),
    )
    setBoardConfigUuids((current) =>
      current.filter((uuid) => uuid !== configUuid),
    )
    removeConfigBindings(configUuid)
  }

  const handleRemoveFromBoard = (configUuid: string) => {
    setBoardConfigUuids((current) =>
      current.filter((uuid) => uuid !== configUuid),
    )
    removeConfigBindings(configUuid)
  }

  const handleConnect = (draft: ConnectionDraft) => {
    const error = getConnectionError(
      draft,
      boardConfigs,
      connections,
      datasetBindings,
      selectedDatasets,
    )
    if (error) {
      Toaster.error(error)
      return
    }
    setConnections((current) => [...current, draft])
  }

  const handleRemoveConnection = (connection: PipelineConnection) => {
    setConnections((current) =>
      current.filter(
        (item) =>
          !(
            item.sourceConfigUuid === connection.sourceConfigUuid &&
            item.sourceOutputName === connection.sourceOutputName &&
            item.targetConfigUuid === connection.targetConfigUuid &&
            item.targetInputPortName === connection.targetInputPortName
          ),
      ),
    )
  }

  const handleToggleDatasetVersion = (
    dataset: Dataset,
    version: DatasetVersion,
  ) => {
    const isSelected = selectedDatasets.some(
      (item) => item.version.datasetVersionUuid === version.datasetVersionUuid,
    )
    if (isSelected) {
      setSelectedDatasets((current) =>
        current.filter(
          (item) =>
            item.version.datasetVersionUuid !== version.datasetVersionUuid,
        ),
      )
      setDatasetBindings((current) =>
        current.filter(
          (binding) =>
            binding.datasetVersionUuid !== version.datasetVersionUuid,
        ),
      )
      return
    }
    setSelectedDatasets((current) => [...current, { dataset, version }])
  }

  const handleRemoveSelectedDataset = (datasetVersionUuid: string) => {
    setSelectedDatasets((current) =>
      current.filter(
        (item) => item.version.datasetVersionUuid !== datasetVersionUuid,
      ),
    )
    setDatasetBindings((current) =>
      current.filter(
        (binding) => binding.datasetVersionUuid !== datasetVersionUuid,
      ),
    )
  }

  const handleBindDataset = (
    target: DatasetBindingTarget,
    datasetVersionUuid: string,
  ) => {
    const selected = selectedDatasets.find(
      (item) => item.version.datasetVersionUuid === datasetVersionUuid,
    )
    if (!selected) {
      Toaster.error(t('runs.pipeline.errors.selectDatasetFirst'))
      return
    }
    const error = getDatasetBindingError(
      target,
      selected,
      boardConfigs,
      connections,
      datasetBindings,
    )
    if (error) {
      Toaster.error(error)
      return
    }
    setDatasetBindings((current) => [
      ...current,
      {
        datasetUuid: selected.dataset.datasetUuid,
        datasetVersionUuid,
        ...target,
      },
    ])
  }

  const handleRemoveDatasetBinding = (binding: DatasetBinding) => {
    setDatasetBindings((current) =>
      current.filter(
        (item) =>
          !(
            item.targetConfigUuid === binding.targetConfigUuid &&
            item.targetInputPortName === binding.targetInputPortName
          ),
      ),
    )
  }

  const handleDatasetPickerSearchChange = (search: string) => {
    setDatasetPickerSearch(search)
    setDatasetPickerPage(1)
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (
      !isPipelineReady ||
      validateRunMutation.isPending ||
      validateRunMutation.error
    ) {
      return
    }

    createRunMutation.mutate(
      { executionMode, name: name.trim() },
      {
        onError: (error) => {
          Toaster.error(
            error instanceof Error ? error.message : t('runs.errors.create'),
          )
        },
        onSuccess: () => {
          Toaster.success(t('runs.created'))
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
    configPickerQuery,
    configDetailsMutation,
    connections,
    createRunMutation,
    datasetBindings,
    datasetPickerPage,
    datasetPickerSearch,
    datasetPickerTotalPages,
    datasetVersionsQuery,
    datasetsQuery,
    experimentUuid,
    filteredDatasets,
    getConnectionError: (draft: ConnectionDraft) =>
      getConnectionError(
        draft,
        boardConfigs,
        connections,
        datasetBindings,
        selectedDatasets,
      ),
    getDatasetBindingError: (
      target: DatasetBindingTarget,
      dataset: SelectedDatasetVersion,
    ) =>
      getDatasetBindingError(
        target,
        dataset,
        boardConfigs,
        connections,
        datasetBindings,
      ),
    handleBindDataset,
    handleConnect,
    handleDatasetPickerSearchChange,
    handleMoveConfigToBoard,
    handleRemoveConfig,
    handleRemoveConnection,
    handleRemoveDatasetBinding,
    handleRemoveFromBoard,
    handleRemoveSelectedDataset,
    handleSelectConfig,
    handleSubmit,
    handleToggleDatasetVersion,
    isConfigPickerOpen,
    isDatasetPickerOpen,
    isPipelineReady,
    name,
    openedDataset,
    pipelineOrder,
    pipelineValidation,
    projectQuery,
    projectUuid,
    selectedConfigs,
    selectedDatasets,
    selectedExperimentType,
    setDatasetPickerPage,
    setIsConfigPickerOpen,
    setIsDatasetPickerOpen,
    setName,
    setOpenedDataset,
    setSelectedExperimentType,
    validateRunMutation,
  }
}
