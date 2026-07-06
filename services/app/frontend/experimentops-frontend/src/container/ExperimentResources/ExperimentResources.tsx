import { useState, type FormEvent } from 'react'
import { useParams } from 'react-router-dom'

import { JsonEditor, JsonEditorDialog } from '../../components/JsonViewer'
import { DataTable } from '../../components/DataTable'
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
import type { ExperimentType } from '../../services/experimentType.service'
import { Toaster } from '../../services/toaster.service'
import { PERMISSIONS_KEYS } from '../../utils'
import {
  PageSkeleton,
  PageState,
  ProjectFrame,
  SectionState,
} from '../ProjectDetails/projectDetails.shared'
import { getErrorMessage } from '../ProjectDetails/projectDetails.utils'
import { getExperimentConfigColumns } from './columns'

const CONFIGS_PER_PAGE = 20

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
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <h2 className="font-heading text-2xl font-semibold text-secondary">
          Experiment configs
        </h2>
        {canAddConfig ? (
          <button
            className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90"
            onClick={() => setIsCreateOpen(true)}
            type="button"
          >
            + Add Config
          </button>
        ) : null}
      </div>

      <div className="mt-6">
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
            columns={getExperimentConfigColumns(setEditingConfig)}
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
          />
        )}
      </div>

      {isCreateOpen && (
        <CreateExperimentConfigDialog
          experimentTypes={experimentTypesQuery.data?.data ?? []}
          experimentTypesError={
            experimentTypesQuery.error
              ? getErrorMessage(
                  experimentTypesQuery.error,
                  'Unable to load experiment types.',
                )
              : null
          }
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
  const { projectUuid } = useParams<{ projectUuid: string }>()
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
      <ExperimentRunsPanel />
    </ProjectFrame>
  )
}

function buildDefaultConfig(experimentType: ExperimentType) {
  return Object.fromEntries(
    experimentType.defaultConfig.map((field) => [
      field.name,
      field.defaultValue,
    ]),
  ) as Record<string, unknown>
}

function CreateExperimentConfigDialog({
  experimentTypes,
  experimentTypesError,
  isLoadingExperimentTypes,
  isPending,
  onClose,
  onSubmit,
}: {
  experimentTypes: ExperimentType[]
  experimentTypesError: string | null
  isLoadingExperimentTypes: boolean
  isPending: boolean
  onClose: () => void
  onSubmit: (input: { config: Record<string, unknown>; name: string }) => void
}) {
  const [selectedTypeUuid, setSelectedTypeUuid] = useState('')
  const [configDraft, setConfigDraft] = useState<Record<
    string,
    unknown
  > | null>(null)
  const selectedType =
    experimentTypes.find((type) => type.uuid === selectedTypeUuid) ?? null

  const handleSelectType = (uuid: string) => {
    setSelectedTypeUuid(uuid)
    const experimentType = experimentTypes.find((type) => type.uuid === uuid)
    setConfigDraft(experimentType ? buildDefaultConfig(experimentType) : null)
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selectedType || !configDraft) return

    onSubmit({ config: configDraft, name: selectedType.name })
  }

  return (
    <div
      aria-labelledby="create-experiment-config-title"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-secondary/40 p-4"
      role="dialog"
    >
      <div className="w-full max-w-lg rounded-xl bg-white p-6 shadow-2xl">
        <div className="flex items-center justify-between gap-4">
          <h2
            className="font-heading text-2xl font-semibold text-secondary"
            id="create-experiment-config-title"
          >
            Add Config
          </h2>
          <button
            aria-label="Close add config dialog"
            className="rounded-md p-2 text-slate-500 hover:bg-slate-100"
            disabled={isPending}
            onClick={onClose}
            type="button"
          >
            ×
          </button>
        </div>

        <form className="mt-5 space-y-4" onSubmit={handleSubmit}>
          <label className="block text-sm font-medium text-secondary">
            Experiment type
            <select
              autoFocus
              className="mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
              disabled={isPending || isLoadingExperimentTypes}
              onChange={(event) => handleSelectType(event.target.value)}
              required
              value={selectedTypeUuid}
            >
              <option disabled value="">
                {isLoadingExperimentTypes
                  ? 'Loading experiment types…'
                  : 'Select an experiment type'}
              </option>
              {experimentTypes.map((experimentType) => (
                <option key={experimentType.uuid} value={experimentType.uuid}>
                  {experimentType.name}
                </option>
              ))}
            </select>
          </label>

          {experimentTypesError ? (
            <p className="text-xs text-red-600">{experimentTypesError}</p>
          ) : null}

          {configDraft ? (
            <div>
              <span className="block text-sm font-medium text-secondary">
                Config
              </span>
              <div className="mt-1">
                <JsonEditor
                  onChange={(value) =>
                    setConfigDraft(value as Record<string, unknown>)
                  }
                  value={configDraft}
                />
              </div>
            </div>
          ) : null}

          <div className="flex justify-end gap-3 pt-2">
            <button
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-semibold text-secondary hover:bg-slate-50"
              disabled={isPending}
              onClick={onClose}
              type="button"
            >
              Cancel
            </button>
            <button
              className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={isPending || !configDraft}
              type="submit"
            >
              {isPending ? 'Creating…' : 'Create config'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

function TableSkeleton() {
  return (
    <div className="space-y-2" role="status">
      <div className="h-12 animate-pulse rounded bg-slate-100" />
      <div className="h-10 animate-pulse rounded bg-slate-100" />
      <div className="h-10 animate-pulse rounded bg-slate-100" />
      <span className="sr-only">Loading experiment configs</span>
    </div>
  )
}

function ExperimentRunsPanel() {
  return (
    <div>
      <h2 className="font-heading text-2xl font-semibold text-secondary">
        Experiment runs
      </h2>
      <div className="mt-6 rounded-lg border border-dashed border-slate-300 px-6 py-12 text-center text-sm text-slate-600">
        The runs API and content will be connected here next.
      </div>
    </div>
  )
}
