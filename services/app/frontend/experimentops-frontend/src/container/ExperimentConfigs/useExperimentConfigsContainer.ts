import { useState } from 'react'
import { useParams } from 'react-router-dom'

import { useLogin } from '../../context-api/logincontext'
import {
  useMutationCreateExperimentConfig,
  useMutationUpdateExperimentConfig,
  useQueryExperimentConfigs,
  useQueryExperimentTypes,
  useQueryProject,
} from '../../queries'
import type { ExperimentConfig } from '../../services/experimentConfig.service'
import { Toaster } from '../../services/toaster.service'
import { PERMISSIONS_KEYS } from '../../utils'
import { getExperimentConfigValidationErrors } from './configValidation'

const CONFIGS_PER_PAGE = 20

export function useExperimentConfigsContainer() {
  const { experimentUuid, projectUuid } = useParams<{
    experimentUuid: string
    projectUuid: string
  }>()
  const { hasPermission } = useLogin()
  const projectQuery = useQueryProject(projectUuid)
  const [page, setPage] = useState(1)
  const configsQuery = useQueryExperimentConfigs(experimentUuid, {
    page: page - 1,
    size: CONFIGS_PER_PAGE,
  })
  const createConfigMutation = useMutationCreateExperimentConfig(
    experimentUuid ?? '',
  )
  const updateConfigMutation = useMutationUpdateExperimentConfig(
    experimentUuid ?? '',
  )
  const [editingConfig, setEditingConfig] = useState<ExperimentConfig | null>(
    null,
  )
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const experimentTypesQuery = useQueryExperimentTypes(
    isCreateOpen || Boolean(editingConfig),
  )
  const canListConfigs = hasPermission(
    PERMISSIONS_KEYS.EXPERIMENT_CONFIG.GET_EXPERIMENT_CONFIG,
  )
  const canAddConfig = hasPermission(
    PERMISSIONS_KEYS.EXPERIMENT_CONFIG.ADD_EXPERIMENT_CONFIG,
  )
  const totalPages = Math.max(
    1,
    Math.ceil((configsQuery.data?.totalElements ?? 0) / CONFIGS_PER_PAGE),
  )

  const handleCreateConfig = (input: {
    config: Record<string, unknown>
    experimentType: string
    name: string
  }) => {
    createConfigMutation.mutate(input, {
      onError: (error) => {
        Toaster.error(
          error instanceof Error
            ? error.message
            : 'Unable to create the experiment config.',
        )
      },
      onSuccess: () => {
        setIsCreateOpen(false)
        Toaster.success('Experiment config created successfully.')
      },
    })
  }

  const handleUpdateConfig = (nextConfig: unknown) => {
    if (!editingConfig) return
    const experimentType =
      experimentTypesQuery.data?.data.find(
        (type) => type.name === editingConfig.experimentType,
      ) ?? null
    const validationErrors = getExperimentConfigValidationErrors(
      nextConfig,
      experimentType,
    )

    if (validationErrors.length) {
      Toaster.error(validationErrors[0])
      return
    }

    updateConfigMutation.mutate(
      {
        input: {
          config: nextConfig as Record<string, unknown>,
          experimentType: editingConfig.experimentType,
          name: editingConfig.name,
        },
        uuid: editingConfig.uuid,
      },
      {
        onError: (error) => {
          Toaster.error(
            error instanceof Error
              ? error.message
              : 'Unable to update the experiment config.',
          )
        },
        onSuccess: () => {
          setEditingConfig(null)
          Toaster.success('Experiment config updated successfully.')
        },
      },
    )
  }

  const getEditingConfigValidationErrors = (nextConfig: unknown) => {
    if (!editingConfig) return []

    const experimentType =
      experimentTypesQuery.data?.data.find(
        (type) => type.name === editingConfig.experimentType,
      ) ?? null

    return getExperimentConfigValidationErrors(nextConfig, experimentType)
  }

  return {
    canAddConfig,
    canListConfigs,
    configsQuery,
    createConfigMutation,
    editingConfig,
    experimentTypesQuery,
    getEditingConfigValidationErrors,
    handleCreateConfig,
    handleUpdateConfig,
    isCreateOpen,
    page,
    projectQuery,
    setEditingConfig,
    setIsCreateOpen,
    setPage,
    totalPages,
    updateConfigMutation,
  }
}
