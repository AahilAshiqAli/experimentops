import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'

import { DataTable } from '../../components/DataTable'
import { JsonEditorDialog } from '../../components/JsonViewer'
import { useLogin } from '../../context-api/logincontext'
import { useDocumentTitle } from '../../hooks'
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
import {
  PageSkeleton,
  PageState,
  ProjectFrame,
  SectionState,
} from '../ProjectDetails/projectDetails.shared'
import { getErrorMessage } from '../ProjectDetails/projectDetails.utils'
import { CreateExperimentConfigDialog } from './CreateExperimentConfigDialog'
import { CreateExperimentRun } from './CreateExperimentRun'
import { ExperimentRunsPanel } from './ExperimentRunsPanel'
import { TableSkeleton } from './TableSkeleton'
import { getExperimentConfigColumns } from './columns'

const CONFIGS_PER_PAGE = 20

export { CreateExperimentRun }

export function ExperimentConfigs() {
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
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const experimentTypesQuery = useQueryExperimentTypes(isCreateOpen)
  const [editingConfig, setEditingConfig] = useState<ExperimentConfig | null>(
    null,
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

  useDocumentTitle('Experiment configs')

  if (projectQuery.isLoading) return <PageSkeleton />
  if (projectQuery.error) {
    return (
      <PageState
        message={getErrorMessage(
          projectQuery.error,
          'Unable to load this project.',
        )}
        title="Unable to load project"
        tone="error"
      />
    )
  }
  if (!projectQuery.data) {
    return (
      <PageState
        message="This project is not available."
        title="Project not found"
      />
    )
  }

  return (
    <ProjectFrame activeTab="experiments" project={projectQuery.data}>
      <div className="mb-2">
        <h2 className="font-heading text-xl font-semibold text-secondary">
          Experiment configs
        </h2>
      </div>
      <div>
        {!canListConfigs ? (
          <SectionState message="You do not have permission to view experiment configs." />
        ) : configsQuery.isLoading ? (
          <TableSkeleton />
        ) : configsQuery.error ? (
          <SectionState
            message={getErrorMessage(
              configsQuery.error,
              'Unable to load experiment configs. Please try again.',
            )}
            tone="error"
          />
        ) : (
          <DataTable
            columns={getExperimentConfigColumns({ onEdit: setEditingConfig })}
            data={configsQuery.data?.data ?? []}
            emptyMessage="No configs have been added to this experiment yet."
            getRowKey={(config) => config.uuid}
            pagination={{
              onPageChange: setPage,
              page,
              totalItems: configsQuery.data?.totalElements ?? 0,
              totalPages,
            }}
            searchPlaceholder="Search configs..."
            toolbarEnd={
              <>
                {canAddConfig ? (
                  <button
                    className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90"
                    onClick={() => setIsCreateOpen(true)}
                    type="button"
                  >
                    + Add Config
                  </button>
                ) : null}
              </>
            }
          />
        )}
      </div>

      {isCreateOpen && (
        <CreateExperimentConfigDialog
          experimentTypes={experimentTypesQuery.data?.data ?? []}
          isLoadingExperimentTypes={experimentTypesQuery.isLoading}
          isPending={createConfigMutation.isPending}
          onClose={() => setIsCreateOpen(false)}
          onSubmit={(input) => {
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
          }}
        />
      )}

      {editingConfig && (
        <JsonEditorDialog
          isSubmitting={updateConfigMutation.isPending}
          onClose={() => setEditingConfig(null)}
          onSubmit={(nextConfig) => {
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
          }}
          title={editingConfig.name}
          value={editingConfig.config}
        />
      )}
    </ProjectFrame>
  )
}

export function ExperimentRuns() {
  const { experimentUuid, projectUuid } = useParams<{
    experimentUuid: string
    projectUuid: string
  }>()
  const navigate = useNavigate()
  const projectQuery = useQueryProject(projectUuid)

  useDocumentTitle('Experiment runs')

  if (projectQuery.isLoading) return <PageSkeleton />
  if (projectQuery.error) {
    return (
      <PageState
        message={getErrorMessage(
          projectQuery.error,
          'Unable to load this project.',
        )}
        title="Unable to load project"
        tone="error"
      />
    )
  }
  if (!projectQuery.data) {
    return (
      <PageState
        message="This project is not available."
        title="Project not found"
      />
    )
  }

  return (
    <ProjectFrame activeTab="experiments" project={projectQuery.data}>
      <ExperimentRunsPanel
        onAdd={() =>
          navigate(
            `/projects/${projectUuid}/experiments/${experimentUuid}/experiment-runs/new`,
          )
        }
      />
    </ProjectFrame>
  )
}
