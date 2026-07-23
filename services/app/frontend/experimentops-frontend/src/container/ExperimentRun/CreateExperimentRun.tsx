import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import { setDatasetDragData } from '../../components/datasetDrag'
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
  const { t } = useTranslation()
  const container = useExperimentRunContainer()

  useDocumentTitle(t('runs.create'))

  if (container.projectQuery.isLoading) return <PageSkeleton />
  if (container.projectQuery.error || !container.projectQuery.data) {
    return (
      <PageState
        message={
          container.projectQuery.error
            ? getErrorMessage(
                container.projectQuery.error,
                t('datasets.errors.loadProject'),
              )
            : t('project.notAvailable')
        }
        title={
          container.projectQuery.error
            ? t('project.errors.loadTitle')
            : t('project.errors.notFoundTitle')
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
            ← {t('runs.back')}
          </Link>
          <h2 className="mt-4 font-heading text-2xl font-semibold text-secondary">
            {t('runs.add')}
          </h2>

          <section className="mt-6">
            <input
              className="w-full rounded-md border border-slate-300 px-3 py-2 outline-none placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/20"
              id="experiment-run-name"
              onChange={(event) => container.setName(event.target.value)}
              placeholder={t('runs.namePlaceholder')}
              type="text"
              value={container.name}
            />
            <button
              className="mt-4 w-full rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90"
              onClick={() => container.setIsDatasetPickerOpen(true)}
              type="button"
            >
              {t('runs.findDatasets')}
            </button>

            <div className="mt-3 rounded-lg border border-dashed border-slate-300 p-3">
              {container.selectedDatasets.length ? (
                <div className="space-y-2">
                  <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                    {t('runs.availableDatasets')}
                  </p>
                  {container.selectedDatasets.map(({ dataset, version }) => {
                    const bindingCount = container.datasetBindings.filter(
                      (binding) =>
                        binding.datasetVersionUuid ===
                        version.datasetVersionUuid,
                    ).length
                    return (
                      <div
                        className="group flex cursor-grab items-start gap-2 rounded-md border border-slate-200 bg-white p-2 active:cursor-grabbing"
                        draggable
                        key={version.datasetVersionUuid}
                        onDragStart={(event) =>
                          setDatasetDragData(event, version.datasetVersionUuid)
                        }
                        title={t('runs.dragDataset')}
                      >
                        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded bg-primary/10 text-xs font-black text-primary">
                          D
                        </div>
                        <div className="min-w-0 flex-1">
                          <p
                            className="truncate text-xs font-semibold text-secondary"
                            title={version.originalFileName}
                          >
                            {version.originalFileName}
                          </p>
                          <p className="truncate text-[10px] text-slate-500">
                            {dataset.name} · {version.format}
                            {bindingCount
                              ? ` · ${t('runs.usedCount', { count: bindingCount })}`
                              : ''}
                          </p>
                        </div>
                        <button
                          aria-label={t('runs.removeDataset', {
                            file: version.originalFileName,
                          })}
                          className="px-1 text-xs font-bold text-red-500 opacity-70 hover:opacity-100"
                          onClick={() =>
                            container.handleRemoveSelectedDataset(
                              version.datasetVersionUuid,
                            )
                          }
                          type="button"
                        >
                          ×
                        </button>
                      </div>
                    )
                  })}
                  <p className="text-[10px] leading-4 text-slate-500">
                    {t('runs.dragHint')}
                  </p>
                </div>
              ) : (
                <p className="text-sm text-slate-500">
                  {t('runs.noDatasetsSelected')}
                </p>
              )}
            </div>
          </section>

          <section className="mt-6 border-t border-slate-200 pt-5">
            <h3 className="text-sm font-semibold text-secondary">
              {t('runs.searchConfigsTitle')}
            </h3>
            <label className="mt-3 block text-sm font-medium text-secondary">
              {t('configs.experimentType')}
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
                    ? t('configs.loadingTypes')
                    : t('configs.selectType')}
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
              {t('runs.searchConfigs')}
            </button>
          </section>

          <section className="mt-6 border-t border-slate-200 pt-5">
            <h3 className="text-sm font-semibold text-secondary">
              {t('runs.availableConfigs')}
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
                        {t('runs.moveToBoard')}
                      </button>
                      <button
                        className="rounded-md px-2.5 py-1.5 text-xs font-semibold text-red-600 hover:bg-red-50"
                        onClick={() =>
                          container.handleRemoveConfig(config.uuid)
                        }
                        type="button"
                      >
                        {t('runs.remove')}
                      </button>
                    </div>
                  </li>
                ))}
              </ol>
            ) : container.selectedConfigs.length === 0 ? (
              <p className="mt-3 text-sm text-slate-500">
                {t('runs.searchConfigsDescription')}
              </p>
            ) : (
              <p className="mt-3 text-sm text-slate-500">
                {t('runs.noAvailableConfigs')}
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
              !container.isPipelineReady
            }
            type="submit"
          >
            {container.createRunMutation.isPending
              ? t('runs.creating')
              : t('runs.create')}
          </button>
        </aside>

        <main
          aria-label={t('runs.boardLabel')}
          className="flex min-h-[calc(100vh-12rem)] flex-col rounded-xl border border-slate-200 bg-white p-5 shadow-sm"
        >
          <PipelineBoard
            configs={container.boardConfigs}
            connections={container.connections}
            datasetBindings={container.datasetBindings}
            getConnectionError={container.getConnectionError}
            getDatasetBindingError={container.getDatasetBindingError}
            onBindDataset={container.handleBindDataset}
            onConnect={container.handleConnect}
            onRemoveConnection={container.handleRemoveConnection}
            onRemoveDatasetBinding={container.handleRemoveDatasetBinding}
            onRemoveFromBoard={container.handleRemoveFromBoard}
            pipelineOrder={container.pipelineOrder}
            selectedDatasets={container.selectedDatasets}
          />

          {!container.isPipelineReady && container.boardConfigs.length > 0 ? (
            <div className="mt-4 rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-800">
              <p className="font-semibold">{t('runs.pipeline.attention')}</p>
              <ul className="mt-1 list-disc space-y-1 pl-5">
                {container.pipelineValidation.errors.map((error) => (
                  <li key={error}>{error}</li>
                ))}
              </ul>
            </div>
          ) : null}
          {container.isPipelineReady ? (
            container.validateRunMutation.isPending ? (
              <p className="mt-4 rounded-lg bg-sky-50 px-4 py-3 text-sm text-sky-800">
                {t('runs.pipeline.validating')}
              </p>
            ) : container.validateRunMutation.error ? (
              <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
                {getErrorMessage(
                  container.validateRunMutation.error,
                  t('runs.errors.pipelineInvalid'),
                )}
              </p>
            ) : container.validateRunMutation.isSuccess ? (
              <p className="mt-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                {t('runs.pipeline.passed')}
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
          onToggle={container.handleToggleDatasetVersion}
          projectUuid={container.projectUuid ?? ''}
          selectedVersionUuids={container.selectedDatasets.map(
            (item) => item.version.datasetVersionUuid,
          )}
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
          isSelecting={container.configDetailsMutation.isPending}
          onClose={() => container.setIsConfigPickerOpen(false)}
          onSelect={container.handleSelectConfig}
          selectedConfigUuids={container.selectedConfigs.map(
            (config) => config.uuid,
          )}
        />
      ) : null}
    </ProjectFrame>
  )
}
