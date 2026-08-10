import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import { useDocumentTitle } from '../../hooks'
import type { ExperimentRunStatus } from '../../services/experimentRun.service'
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
import { RUN_STATUS_TRANSLATION_KEYS } from './columns'
import { useExperimentRunDetailsContainer } from './useExperimentRunDetailsContainer'

function formatDateTime(value: string, locale?: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat(locale, {
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
  const { t } = useTranslation()
  return (
    <section aria-label={t('runs.details.loading')} className="animate-pulse">
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
  const { i18n, t } = useTranslation()
  const language = i18n.resolvedLanguage ?? i18n.language
  const container = useExperimentRunDetailsContainer()
  const run = container.experimentRunQuery.data
  const runsPath = `/projects/${container.projectUuid}/experiments/${container.experimentUuid}/experiment-runs`

  useDocumentTitle(
    run?.name
      ? t('runs.details.titleWithName', { name: run.name })
      : t('runs.details.title'),
  )

  if (!container.canViewExperimentRun) {
    return (
      <RunDetailsState
        message={t('runs.permissionDenied')}
        title={t('runs.details.unavailableTitle')}
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
            : t('runs.errors.load')
        }
        title={t('runs.errors.loadTitle')}
      />
    )
  }
  if (!run) {
    return (
      <RunDetailsState
        message={t('runs.details.notAvailable')}
        title={t('runs.details.notFoundTitle')}
      />
    )
  }

  return (
    <section>
      <nav
        aria-label={t('common.navigation.breadcrumb')}
        className="flex flex-wrap items-center gap-2 text-sm text-slate-500"
      >
        <Link className="transition hover:text-primary" to={runsPath}>
          {t('project.runs')}
        </Link>
        <span aria-hidden="true">›</span>
        <span>{run.experimentName}</span>
        <span aria-hidden="true">›</span>
        <span className="font-semibold text-secondary">
          {t('runs.details.runLabel', { id: run.uuid })}
        </span>
      </nav>

      <header className="mt-6">
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="font-heading text-3xl font-semibold text-secondary">
            {t('runs.details.runHeading', { name: run.name })}
          </h1>
          <RunStatusBadge status={run.status} />
        </div>
        <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-slate-500">
          <span>
            {t('runs.details.experiment')}:{' '}
            <span className="font-medium text-primary">
              {run.experimentName}
            </span>
          </span>
          <span aria-hidden="true">•</span>
          <span>
            {t('runs.details.project')}:{' '}
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
              label={t('runs.details.status')}
              tone={statusTone(run.status)}
              value={t(RUN_STATUS_TRANSLATION_KEYS[run.status])}
            />
            <SummaryCard
              icon={<StepsIcon className="h-6 w-6" />}
              label={t('runs.details.stepsCompleted')}
              value={`${run.completedSteps} / ${run.numSteps}`}
            />
            <SummaryCard
              icon={<ArtifactIcon className="h-6 w-6" />}
              label={t('runs.details.artifacts')}
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
            {t('runs.details.runInformation')}
          </h2>
          <dl className="mt-3 divide-y divide-slate-100">
            <InfoRow
              label={t('runs.details.runId')}
              value={<CopyableRunId uuid={run.uuid} />}
            />
            <InfoRow
              label={t('runs.details.status')}
              value={<RunStatusBadge status={run.status} />}
            />
            <InfoRow
              label={t('runs.details.experiment')}
              value={run.experimentName}
            />
            <InfoRow
              label={t('runs.details.project')}
              value={
                <Link
                  className="text-primary hover:underline"
                  to={`/projects/${run.projectUUID}`}
                >
                  {run.projectName}
                </Link>
              }
            />
            <InfoRow
              label={t('runs.details.triggeredBy')}
              value={run.createdBy}
            />
            <InfoRow
              label={t('runs.details.startedAt')}
              value={formatDateTime(run.creationDate, language)}
            />
            <InfoRow
              label={t('runs.details.completedAt')}
              value={
                run.status === 'SUCCEEDED' || run.status === 'FAILED'
                  ? formatDateTime(run.lastUpdated, language)
                  : '—'
              }
            />
          </dl>
        </aside>
      </div>
    </section>
  )
}
