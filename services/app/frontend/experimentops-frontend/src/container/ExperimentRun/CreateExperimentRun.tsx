import { Link } from 'react-router-dom'

import { PipelineBoard } from '../../components/PipelineBoard'
import { useDocumentTitle } from '../../hooks'
import {
  PageSkeleton,
  PageState,
  ProjectFrame,
} from '../ProjectDetails/projectDetails.shared'
import { getErrorMessage } from '../ProjectDetails/projectDetails.utils'
import { DatasetVersionPickerDialog } from './DatasetVersionPickerDialog'
import { ExperimentConfigPickerDialog } from './ExperimentConfigPickerDialog'
import { useExperimentRunContainer } from './useExperimentRunContainer'

export function CreateExperimentRun() {
  const container = useExperimentRunContainer()

  useDocumentTitle('Create experiment run')

  if (container.projectQuery.isLoading) return <PageSkeleton />
  if (container.projectQuery.error || !container.projectQuery.data) {
    return (
      <PageState
        message={
          container.projectQuery.error
            ? getErrorMessage(
                container.projectQuery.error,
                'Unable to load this project.',
              )
            : 'This project is not available.'
        }
        title={
          container.projectQuery.error
            ? 'Unable to load project'
            : 'Project not found'
        }
        tone={container.projectQuery.error ? 'error' : 'neutral'}
      />
    )
  }

  return (
    <ProjectFrame activeTab="experiments" project={container.projectQuery.data}>
      <form
        className="grid min-h-[calc(100vh-12rem)] gap-6 lg:grid-cols-[20rem_minmax(0,1fr)]"
        onSubmit={container.handleSubmit}
      >
        <aside className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
          <Link
            className="text-sm font-semibold text-primary hover:underline"
            to={`/projects/${container.projectUuid}/experiments/${container.experimentUuid}/experiment-runs`}
          >
            ← Back to runs
          </Link>
          <h2 className="mt-4 font-heading text-2xl font-semibold text-secondary">
            Add Experiment Run
          </h2>

          <section className="mt-6">
            <input
              className="w-full rounded-md border border-slate-300 px-3 py-2 outline-none placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/20"
              id="experiment-run-name"
              onChange={(event) => container.setName(event.target.value)}
              placeholder="Enter experiment run name"
              type="text"
              value={container.name}
            />
            <button
              className="mt-4 w-full rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90"
              onClick={() => container.setIsDatasetPickerOpen(true)}
              type="button"
            >
              Find Dataset
            </button>

            <div className="mt-3 rounded-lg border border-dashed border-slate-300 p-4">
              {container.selectedDataset && container.selectedDatasetVersion ? (
                <div>
                  <div className="flex items-start gap-3">
                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
                      ↻
                    </div>
                    <div className="min-w-0">
                      <p
                        className="truncate text-sm font-semibold text-secondary"
                        title={container.selectedDataset.name}
                      >
                        {container.selectedDataset.name}
                      </p>
                      <p
                        className="mt-1 truncate text-xs text-slate-500"
                        title={
                          container.selectedDatasetVersion.originalFileName
                        }
                      >
                        {container.selectedDatasetVersion.originalFileName}
                      </p>
                    </div>
                  </div>
                  <button
                    className="mt-3 text-xs font-semibold text-primary hover:underline"
                    onClick={() => container.setIsDatasetPickerOpen(true)}
                    type="button"
                  >
                    Change dataset
                  </button>
                </div>
              ) : (
                <p className="text-sm text-slate-500">
                  No dataset version selected yet.
                </p>
              )}
            </div>
          </section>

          <section className="mt-6 border-t border-slate-200 pt-5">
            <h3 className="text-sm font-semibold text-secondary">
              Search for experiment configs
            </h3>
            <label className="mt-3 block text-sm font-medium text-secondary">
              Experiment Type
              <select
                className="mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
                disabled={container.experimentTypesQuery.isLoading}
                onChange={(event) =>
                  container.setSelectedExperimentType(event.target.value)
                }
                value={container.selectedExperimentType}
              >
                <option value="">
                  {container.experimentTypesQuery.isLoading
                    ? 'Loading experiment types…'
                    : 'Select an experiment type'}
                </option>
                {(container.experimentTypesQuery.data?.data ?? []).map(
                  (type) => (
                    <option key={type.uuid} value={type.name}>
                      {type.name}
                    </option>
                  ),
                )}
              </select>
            </label>
            <button
              className="mt-3 w-full rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary transition hover:bg-primary/5 disabled:cursor-not-allowed disabled:opacity-50"
              disabled={!container.selectedExperimentType}
              onClick={() => container.setIsConfigPickerOpen(true)}
              type="button"
            >
              Search configs
            </button>
          </section>

          <section className="mt-6 border-t border-slate-200 pt-5">
            <h3 className="text-sm font-semibold text-secondary">
              Available configs
            </h3>
            {container.availableConfigs.length ? (
              <ol className="mt-3 space-y-2">
                {container.availableConfigs.map((config) => (
                  <li
                    className="rounded-lg border border-primary/20 bg-primary/5 p-3"
                    key={config.uuid}
                  >
                    <div className="min-w-0">
                      <p
                        className="truncate text-sm font-semibold text-secondary"
                        title={config.name}
                      >
                        {config.name}
                      </p>
                      <p className="text-xs text-slate-500">
                        {config.experimentType}
                      </p>
                    </div>
                    <div className="mt-3 flex flex-wrap gap-2">
                      <button
                        className="rounded-md border border-primary bg-white px-2.5 py-1.5 text-xs font-semibold text-primary hover:bg-primary/5"
                        onClick={() =>
                          container.handleMoveConfigToBoard(config)
                        }
                        type="button"
                      >
                        Move to board
                      </button>
                      <button
                        className="rounded-md px-2.5 py-1.5 text-xs font-semibold text-red-600 hover:bg-red-50"
                        onClick={() =>
                          container.handleRemoveConfig(config.uuid)
                        }
                        type="button"
                      >
                        Remove
                      </button>
                    </div>
                  </li>
                ))}
              </ol>
            ) : container.selectedConfigs.length === 0 ? (
              <p className="mt-3 text-sm text-slate-500">
                Search configs and select rows from the popup.
              </p>
            ) : (
              <p className="mt-3 text-sm text-slate-500">
                All selected configs are on the board.
              </p>
            )}
          </section>

          <button
            className="mt-6 w-full rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={
              container.createRunMutation.isPending ||
              container.validateRunMutation.isPending ||
              Boolean(container.validateRunMutation.error) ||
              !container.name.trim() ||
              !container.selectedDatasetVersion ||
              !container.isPipelineReady
            }
            type="submit"
          >
            {container.createRunMutation.isPending
              ? 'Creating run…'
              : 'Create Experiment Run'}
          </button>
        </aside>

        <main
          aria-label="Pipeline builder board"
          className="flex min-h-[calc(100vh-12rem)] flex-col rounded-xl border border-slate-200 bg-white p-5 shadow-sm"
        >
          <PipelineBoard
            configs={container.boardConfigs}
            connections={container.connections}
            onConnect={container.handleConnect}
            onRemoveConnection={container.handleRemoveConnection}
            onRemoveFromBoard={container.handleRemoveFromBoard}
            pipelineOrder={container.pipelineOrder}
          />

          {!container.isPipelineReady && container.boardConfigs.length > 0 ? (
            <p className="mt-4 rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-800">
              Connect every board config into one chain before creating the run.
            </p>
          ) : null}
          {container.isPipelineReady && container.selectedDatasetVersion ? (
            container.validateRunMutation.isPending ? (
              <p className="mt-4 rounded-lg bg-sky-50 px-4 py-3 text-sm text-sky-800">
                Validating this pipeline with the selected dataset…
              </p>
            ) : container.validateRunMutation.error ? (
              <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
                {getErrorMessage(
                  container.validateRunMutation.error,
                  'This pipeline is not valid for the selected dataset.',
                )}
              </p>
            ) : container.validateRunMutation.isSuccess ? (
              <p className="mt-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                Pipeline validation passed.
              </p>
            ) : null
          ) : null}
        </main>
      </form>

      {container.isDatasetPickerOpen ? (
        <DatasetVersionPickerDialog
          datasetPickerPage={container.datasetPickerPage}
          datasetPickerSearch={container.datasetPickerSearch}
          datasetPickerTotalPages={container.datasetPickerTotalPages}
          datasetVersions={container.datasetVersionsQuery.data?.versions ?? []}
          datasetVersionsError={container.datasetVersionsQuery.error}
          datasetsError={container.datasetsQuery.error}
          filteredDatasets={container.filteredDatasets}
          isDatasetVersionsLoading={container.datasetVersionsQuery.isLoading}
          isDatasetsLoading={container.datasetsQuery.isLoading}
          openedDataset={container.openedDataset}
          onClose={() => container.setIsDatasetPickerOpen(false)}
          onDatasetPickerPageChange={container.setDatasetPickerPage}
          onDatasetPickerSearchChange={
            container.handleDatasetPickerSearchChange
          }
          onOpenDataset={container.setOpenedDataset}
          onSelect={container.handleSelectDatasetVersion}
          projectUuid={container.projectUuid ?? ''}
          selectedVersionUuid={
            container.selectedDatasetVersion?.datasetVersionUuid
          }
          totalDatasetVersions={
            container.datasetVersionsQuery.data?.totalElements ?? 0
          }
        />
      ) : null}
      {container.isConfigPickerOpen ? (
        <ExperimentConfigPickerDialog
          configs={container.configPickerQuery.data?.data ?? []}
          error={container.configPickerQuery.error}
          experimentType={container.selectedExperimentType}
          isLoading={container.configPickerQuery.isLoading}
          onClose={() => container.setIsConfigPickerOpen(false)}
          onSelect={(config) => {
            container.handleSelectConfig(config)
            container.setIsConfigPickerOpen(false)
          }}
          selectedConfigUuids={container.selectedConfigs.map(
            (config) => config.uuid,
          )}
        />
      ) : null}
    </ProjectFrame>
  )
}
