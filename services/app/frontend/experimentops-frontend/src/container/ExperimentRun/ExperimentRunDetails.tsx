import { Link } from 'react-router-dom'

import { useDocumentTitle } from '../../hooks'
import type { ExperimentRunStatus } from '../../services/experimentRun.service'
import { formatLabel } from '../ProjectDetails/projectDetails.utils'
import { ExperimentRunArtifacts } from './ExperimentRunArtifacts'
import { ExperimentRunLogs } from './ExperimentRunLogs'
import {
  ArtifactIcon,
  CheckIcon,
  CopyableRunId,
  CrossIcon,
  InfoRow,
  PipelineFlow,
  RunDetailTabs,
  RunStatusBadge,
  StepsIcon,
  SummaryCard,
} from './ExperimentRunDetail.components'
import { ExperimentRunSteps } from './ExperimentRunSteps'
import { useExperimentRunDetailsContainer } from './useExperimentRunDetailsContainer'

function formatDateTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'medium',
      }).format(date)
}

function statusTone(
  status: ExperimentRunStatus,
): 'danger' | 'primary' | 'success' | 'warning' {
  if (status === 'FAILED') return 'danger'
  if (status === 'SUCCEEDED') return 'success'
  if (status === 'PENDING') return 'warning'
  return 'primary'
}

function RunDetailsSkeleton() {
  return (
    <section aria-label="Loading experiment run" className="animate-pulse">
      <div className="h-4 w-80 rounded bg-slate-200" />
      <div className="mt-7 h-10 w-2/5 rounded bg-slate-200" />
      <div className="mt-3 h-4 w-1/3 rounded bg-slate-100" />
      <div className="mt-7 grid gap-4 md:grid-cols-3">
        {[0, 1, 2].map((item) => (
          <div className="h-24 rounded-xl bg-slate-200" key={item} />
        ))}
      </div>
      <div className="mt-5 grid gap-5 xl:grid-cols-[minmax(0,1fr)_20rem]">
        <div className="space-y-5">
          <div className="h-56 rounded-xl bg-slate-200" />
          <div className="h-36 rounded-xl bg-slate-100" />
        </div>
        <div className="h-[32rem] rounded-xl bg-slate-200" />
      </div>
    </section>
  )
}

function RunDetailsState({
  message,
  title,
}: {
  message: string
  title: string
}) {
  return (
    <section className="rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
      <h1 className="font-heading text-2xl font-semibold text-secondary">
        {title}
      </h1>
      <p className="mt-2 text-sm text-slate-600">{message}</p>
    </section>
  )
}

export function ExperimentRunDetails() {
  const container = useExperimentRunDetailsContainer()
  const run = container.experimentRunQuery.data
  const runsPath = `/projects/${container.projectUuid}/experiments/${container.experimentUuid}/experiment-runs`

  useDocumentTitle(run?.name ? `${run.name} run` : 'Experiment run')

  if (!container.canViewExperimentRun) {
    return (
      <RunDetailsState
        message="You do not have permission to view experiment runs."
        title="Experiment run unavailable"
      />
    )
  }
  if (container.experimentRunQuery.isLoading) return <RunDetailsSkeleton />
  if (container.experimentRunQuery.error) {
    return (
      <RunDetailsState
        message={
          container.experimentRunQuery.error instanceof Error
            ? container.experimentRunQuery.error.message
            : 'Unable to load this experiment run.'
        }
        title="Unable to load experiment run"
      />
    )
  }
  if (!run) {
    return (
      <RunDetailsState
        message="This experiment run is not available."
        title="Experiment run not found"
      />
    )
  }

  return (
    <section>
      <nav
        aria-label="Breadcrumb"
        className="flex flex-wrap items-center gap-2 text-sm text-slate-500"
      >
        <Link className="transition hover:text-primary" to={runsPath}>
          Runs
        </Link>
        <span aria-hidden="true">›</span>
        <span>{run.experimentName}</span>
        <span aria-hidden="true">›</span>
        <span className="font-semibold text-secondary">Run #{run.uuid}</span>
      </nav>

      <header className="mt-6">
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="font-heading text-3xl font-semibold text-secondary">
            {run.name} Run
          </h1>
          <RunStatusBadge status={run.status} />
        </div>
        <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-slate-500">
          <span>
            Experiment:{' '}
            <span className="font-medium text-primary">
              {run.experimentName}
            </span>
          </span>
          <span aria-hidden="true">•</span>
          <span>
            Project:{' '}
            <Link
              className="font-medium text-primary hover:underline"
              to={`/projects/${run.projectUUID}`}
            >
              {run.projectName}
            </Link>
          </span>
        </div>
        {run.message ? (
          <p
            className={`mt-4 rounded-lg border px-4 py-3 text-sm ${
              run.status === 'FAILED'
                ? 'border-red-200 bg-red-50 text-red-700'
                : 'border-slate-200 bg-white text-slate-600'
            }`}
          >
            {run.message}
          </p>
        ) : null}
      </header>

      <div className="mt-6 grid items-start gap-5 xl:grid-cols-[minmax(0,1fr)_20rem]">
        <div className="min-w-0 space-y-5">
          <div className="grid gap-4 md:grid-cols-3">
            <SummaryCard
              icon={
                run.status === 'FAILED' ? (
                  <CrossIcon className="h-6 w-6" />
                ) : (
                  <CheckIcon className="h-6 w-6" />
                )
              }
              label="Status"
              tone={statusTone(run.status)}
              value={formatLabel(run.status)}
            />
            <SummaryCard
              icon={<StepsIcon className="h-6 w-6" />}
              label="Steps completed"
              value={`${run.numSteps} / ${run.numSteps}`}
            />
            <SummaryCard
              icon={<ArtifactIcon className="h-6 w-6" />}
              label="Artifacts"
              value={run.artifactCount}
            />
          </div>
          <PipelineFlow run={run} />
          <RunDetailTabs
            artifactsContent={
              <ExperimentRunArtifacts
                artifacts={container.runArtifactsQuery.data ?? []}
                error={container.runArtifactsQuery.error}
                isFetching={container.runArtifactsQuery.isFetching}
                isLoading={container.runArtifactsQuery.isLoading}
                onRefresh={() => {
                  void container.runArtifactsQuery.refetch()
                }}
              />
            }
            logsContent={
              <ExperimentRunLogs
                isDownloading={container.isDownloadingLogs}
                isFetching={container.experimentRunLogsQuery.isFetching}
                isLoading={container.experimentRunLogsQuery.isLoading}
                logs={container.experimentRunLogsQuery.data?.data ?? []}
                onDownload={() => {
                  void container.handleDownloadLogs()
                }}
                onPageChange={container.setLogPage}
                onRefresh={() => {
                  void container.experimentRunLogsQuery.refetch()
                }}
                page={container.logPage}
                pageSize={20}
                totalElements={
                  container.experimentRunLogsQuery.data?.totalElements ?? 0
                }
              />
            }
            stepsContent={
              <ExperimentRunSteps
                configError={container.experimentConfigsQuery.error}
                configs={container.experimentConfigsQuery.data ?? []}
                isConfigLoading={container.experimentConfigsQuery.isLoading}
                steps={run.executionMode}
              />
            }
          />
        </div>

        <aside className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm xl:sticky xl:top-24">
          <h2 className="font-heading text-lg font-semibold text-secondary">
            Run information
          </h2>
          <dl className="mt-3 divide-y divide-slate-100">
            <InfoRow label="Run ID" value={<CopyableRunId uuid={run.uuid} />} />
            <InfoRow
              label="Status"
              value={<RunStatusBadge status={run.status} />}
            />
            <InfoRow label="Experiment" value={run.experimentName} />
            <InfoRow
              label="Project"
              value={
                <Link
                  className="text-primary hover:underline"
                  to={`/projects/${run.projectUUID}`}
                >
                  {run.projectName}
                </Link>
              }
            />
            <InfoRow label="Triggered by" value={run.createdBy} />
            <InfoRow
              label="Started at"
              value={formatDateTime(run.creationDate)}
            />
            <InfoRow
              label="Completed at"
              value={
                run.status === 'SUCCEEDED' || run.status === 'FAILED'
                  ? formatDateTime(run.lastUpdated)
                  : '—'
              }
            />
          </dl>
        </aside>
      </div>
    </section>
  )
}
