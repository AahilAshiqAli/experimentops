import { useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import { useLogin } from '../../context-api/logincontext'
import {
  useMutationCreateExperimentConfig,
  useMutationUpdateExperimentConfig,
  useQueryExperimentConfigs,
  useQueryExperimentTypesByNames,
  useQueryProject,
} from '../../queries'
import type { ExperimentConfig } from '../../services/experimentConfig.service'
import { Toaster } from '../../services/toaster.service'
import { PERMISSIONS_KEYS } from '../../utils'
import { getExperimentConfigValidationErrors } from './configValidation'
import { buildExperimentTypeDefaultConfig } from './experimentConfigDefaults'

const CONFIGS_PER_PAGE = 20

export function useExperimentConfigsContainer() {
  const { t } = useTranslation()
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
  const canListConfigs = hasPermission(
    PERMISSIONS_KEYS.EXPERIMENT_CONFIG.GET_EXPERIMENT_CONFIG,
  )
  const [editingConfig, setEditingConfig] = useState<ExperimentConfig | null>(
    null,
  )
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const experimentTypeNames = useMemo(
    () => [
      ...new Set(
        (configsQuery.data?.data ?? []).map((config) => config.experimentType),
      ),
    ],
    [configsQuery.data?.data],
  )
  const experimentTypeQueries = useQueryExperimentTypesByNames(
    experimentTypeNames,
    canListConfigs,
  )
  const experimentTypes = experimentTypeQueries.flatMap(
    (query) => query.data?.data ?? [],
  )
  const isExperimentTypesLoading = experimentTypeQueries.some(
    (query) => query.isLoading,
  )
  const configs = useMemo(
    () =>
      (configsQuery.data?.data ?? []).map((config) => {
        const experimentType = experimentTypes.find(
          (type) => type.name === config.experimentType,
        )

        return experimentType
          ? { ...config, formatMappings: experimentType.formatMappings }
          : config
      }),
    [configsQuery.data?.data, experimentTypes],
  )
  const editingExperimentType = useMemo(
    () =>
      editingConfig
        ? (experimentTypes.find(
            (type) => type.name === editingConfig.experimentType,
          ) ?? null)
        : null,
    [editingConfig, experimentTypes],
  )
  const editingConfigDefault = useMemo(
    () =>
      editingExperimentType
        ? buildExperimentTypeDefaultConfig(editingExperimentType)
        : undefined,
    [editingExperimentType],
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
          error instanceof Error ? error.message : t('configs.errors.create'),
        )
      },
      onSuccess: () => {
        setIsCreateOpen(false)
        Toaster.success(t('configs.created'))
      },
    })
  }

  const handleUpdateConfig = (nextConfig: unknown) => {
    if (!editingConfig) return
    const validationErrors = getExperimentConfigValidationErrors(
      nextConfig,
      editingExperimentType,
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
            error instanceof Error ? error.message : t('configs.errors.update'),
          )
        },
        onSuccess: () => {
          setEditingConfig(null)
          Toaster.success(t('configs.updated'))
        },
      },
    )
  }

  const getEditingConfigValidationErrors = (nextConfig: unknown) => {
    if (!editingConfig) return []

    return getExperimentConfigValidationErrors(
      nextConfig,
      editingExperimentType,
    )
  }

  return {
    canAddConfig,
    canListConfigs,
    configs,
    configsQuery,
    createConfigMutation,
    editingConfig,
    editingConfigDefault,
    getEditingConfigValidationErrors,
    handleCreateConfig,
    handleUpdateConfig,
    isCreateOpen,
    isExperimentTypesLoading,
    page,
    projectQuery,
    setEditingConfig,
    setIsCreateOpen,
    setPage,
    totalPages,
    updateConfigMutation,
  }
}
