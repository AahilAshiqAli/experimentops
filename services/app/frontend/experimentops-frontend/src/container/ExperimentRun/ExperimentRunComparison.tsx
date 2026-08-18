import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import {
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from 'react-router-dom'

import { useLogin } from '../../context-api/logincontext'
import { useDocumentTitle } from '../../hooks'
import { useQueryExperimentRunComparison, useQueryProject } from '../../queries'
import type {
  ExperimentRunComparison,
  ExperimentRunComparisonAxis,
  ExperimentRunComparisonReport,
} from '../../services/experimentRun.service'
import { PERMISSIONS_KEYS } from '../../utils'
import {
  PageSkeleton,
  PageState,
  ProjectFrame,
} from '../ProjectDetails/projectDetails.shared'
import { getErrorMessage } from '../ProjectDetails/projectDetails.utils'
import {
  COMPARISON_EVALUATION_CONTEXT_KEYS,
  COMPARISON_METRIC_KEYS,
  CONFUSION_MATRIX_KEYS,
} from './ExperimentRunComparison.constants'

type ComparisonLocationState = {
  runNames?: Record<string, string>
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function shortUuid(uuid: string) {
  return uuid.length > 16 ? `${uuid.slice(0, 8)}…${uuid.slice(-4)}` : uuid
}

function displayExperimentType(value: string) {
  return value
    .toLocaleLowerCase()
    .split('_')
    .map((part) => `${part.charAt(0).toLocaleUpperCase()}${part.slice(1)}`)
    .join(' ')
}

function displayGenericValue(value: unknown) {
  if (value === undefined || value === null || value === '') return '—'
  if (typeof value === 'number')
    return value.toLocaleString(undefined, {
      maximumFractionDigits: 6,
    })
  if (typeof value === 'string' || typeof value === 'boolean') {
    return String(value)
  }
  return JSON.stringify(value)
}

function displayMetricValue(value: unknown) {
  return typeof value === 'number' && Number.isFinite(value)
    ? value.toFixed(4)
    : displayGenericValue(value)
}

function displayFieldName(value: string) {
  const words = value
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
  return `${words.charAt(0).toLocaleUpperCase()}${words.slice(1)}`
}

function hasNumericFields(
  values: Record<string, unknown>,
  keys: readonly string[],
) {
  return keys.every(
    (key) => typeof values[key] === 'number' && Number.isFinite(values[key]),
  )
}

function RunHeader({
  report,
  runNames,
}: {
  report: ExperimentRunComparisonReport
  runNames: Record<string, string>
}) {
  return (
    <>
      <span className="block font-semibold text-secondary">
        {runNames[report.experimentRunUuid] ??
          shortUuid(report.experimentRunUuid)}
      </span>
      {runNames[report.experimentRunUuid] ? (
        <span className="mt-0.5 block font-mono text-[11px] font-normal normal-case tracking-normal text-slate-400">
          {shortUuid(report.experimentRunUuid)}
        </span>
      ) : null}
    </>
  )
}

function ComparisonTable({
  comparison,
  runNames,
}: {
  comparison: ExperimentRunComparison
  runNames: Record<string, string>
}) {
  const { t } = useTranslation()
  const valuesByRun = comparison.runs.map((report) => ({
    report,
    values: isRecord(report.evaluationReport.metrics)
      ? report.evaluationReport.metrics
      : report.evaluationReport,
  }))
  const keys = [
    ...new Set(valuesByRun.flatMap(({ values }) => Object.keys(values))),
  ]
  const metricKeySet = new Set<string>(COMPARISON_METRIC_KEYS)
  const contextKeySet = new Set<string>(COMPARISON_EVALUATION_CONTEXT_KEYS)
  const confusionMatrixKeySet = new Set<string>(CONFUSION_MATRIX_KEYS)
  const metricKeys = keys.filter((key) => metricKeySet.has(key))
  const contextKeys = keys.filter((key) => contextKeySet.has(key))
  const otherKeys = keys.filter(
    (key) =>
      !metricKeySet.has(key) &&
      !contextKeySet.has(key) &&
      !confusionMatrixKeySet.has(key),
  )
  const runsWithConfusionMatrix = valuesByRun.filter(({ values }) =>
    hasNumericFields(values, CONFUSION_MATRIX_KEYS),
  )

  const getFieldLabel = (key: string, section: 'context' | 'metrics') =>
    t(`runs.compare.${section}.${key}`, {
      defaultValue: displayFieldName(key),
    })

  const renderTable = (
    title: string,
    fieldHeading: string,
    sectionKeys: string[],
    displayValue: (value: unknown) => string,
    labelSection?: 'context' | 'metrics',
  ) =>
    sectionKeys.length > 0 ? (
      <>
        <h3 className="mt-6 text-sm font-semibold text-secondary">{title}</h3>
        <div className="mt-3 overflow-x-auto rounded-lg border border-slate-200">
          <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
              <tr>
                <th className="min-w-44 px-5 py-3 font-semibold" scope="col">
                  {fieldHeading}
                </th>
                {comparison.runs.map((report) => (
                  <th
                    className="min-w-48 px-5 py-3 font-semibold"
                    key={report.experimentRunUuid}
                    scope="col"
                  >
                    <RunHeader report={report} runNames={runNames} />
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {sectionKeys.map((key) => (
                <tr key={key}>
                  <th
                    className="px-5 py-3 font-medium text-slate-600"
                    scope="row"
                  >
                    {labelSection
                      ? getFieldLabel(key, labelSection)
                      : displayFieldName(key)}
                  </th>
                  {valuesByRun.map(({ report, values }) => (
                    <td
                      className="px-5 py-3 font-mono text-secondary"
                      key={report.experimentRunUuid}
                    >
                      {displayValue(values[key])}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </>
    ) : null

  return (
    <>
      {renderTable(
        t('runs.compare.evaluationMetrics'),
        t('runs.compare.metric'),
        metricKeys,
        displayMetricValue,
        'metrics',
      )}
      {renderTable(
        t('runs.compare.evaluationContext'),
        t('runs.compare.field'),
        contextKeys,
        displayGenericValue,
        'context',
      )}
      {renderTable(
        t('runs.compare.otherDetails'),
        t('runs.compare.field'),
        otherKeys,
        displayGenericValue,
      )}

      {runsWithConfusionMatrix.length > 0 ? (
        <>
          <h3 className="mt-6 text-sm font-semibold text-secondary">
            {t('runs.compare.confusionMatrices')}
          </h3>
          <div className="mt-3 grid gap-4 xl:grid-cols-2">
            {runsWithConfusionMatrix.map(({ report, values }) => (
              <div
                className="rounded-lg border border-slate-200 bg-white p-5"
                key={report.experimentRunUuid}
              >
                <RunHeader report={report} runNames={runNames} />
                <div className="mt-4 grid grid-cols-[6rem_repeat(2,minmax(5rem,1fr))] gap-2 text-center text-xs">
                  <span />
                  <span className="py-1 text-slate-500">
                    {t('runs.compare.predicted', { value: 0 })}
                  </span>
                  <span className="py-1 text-slate-500">
                    {t('runs.compare.predicted', { value: 1 })}
                  </span>
                  <span className="grid place-items-center text-slate-500">
                    {t('runs.compare.actual', { value: 0 })}
                  </span>
                  <span className="grid min-h-14 place-items-center rounded-md bg-sky-50 font-mono text-base font-semibold text-sky-700">
                    {displayGenericValue(values.trueNegative)}
                  </span>
                  <span className="grid min-h-14 place-items-center rounded-md bg-slate-100 font-mono text-base font-semibold text-slate-600">
                    {displayGenericValue(values.falsePositive)}
                  </span>
                  <span className="grid place-items-center text-slate-500">
                    {t('runs.compare.actual', { value: 1 })}
                  </span>
                  <span className="grid min-h-14 place-items-center rounded-md bg-slate-100 font-mono text-base font-semibold text-slate-600">
                    {displayGenericValue(values.falseNegative)}
                  </span>
                  <span className="grid min-h-14 place-items-center rounded-md bg-sky-50 font-mono text-base font-semibold text-sky-700">
                    {displayGenericValue(values.truePositive)}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </>
      ) : null}
    </>
  )
}

export function ExperimentRunComparison() {
  const { t } = useTranslation()
  const { experimentUuid, projectUuid } = useParams<{
    experimentUuid: string
    projectUuid: string
  }>()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const { hasPermission } = useLogin()
  const projectQuery = useQueryProject(projectUuid)
  const axisParam = searchParams.get('axis')
  const comparisonAxis: ExperimentRunComparisonAxis =
    axisParam === 'DATASET' ? 'DATASET' : 'CONFIG'
  const hasValidAxis = axisParam === 'CONFIG' || axisParam === 'DATASET'
  const experimentRunUuids = useMemo(
    () => [...new Set(searchParams.getAll('run').filter(Boolean))],
    [searchParams],
  )
  const comparisonInput = useMemo(
    () => ({ comparisonAxis, experimentRunUuids }),
    [comparisonAxis, experimentRunUuids],
  )
  const comparisonQuery = useQueryExperimentRunComparison(
    comparisonInput,
    hasValidAxis,
  )
  const canCompareExperimentRuns = hasPermission(
    PERMISSIONS_KEYS.EXPERIMENT_RUN.COMPARE_EXPERIMENT_RUNS,
  )
  const state = location.state as ComparisonLocationState | null
  const runNames = state?.runNames ?? {}
  const runsPath = `/projects/${projectUuid}/experiments/${experimentUuid}/experiment-runs`

  useDocumentTitle(t('runs.compare.title'))

  if (projectQuery.isLoading) return <PageSkeleton />
  if (projectQuery.error) {
    return (
      <PageState
        message={getErrorMessage(
          projectQuery.error,
          t('datasets.errors.loadProject'),
        )}
        title={t('project.errors.loadTitle')}
        tone="error"
      />
    )
  }
  if (!projectQuery.data) {
    return (
      <PageState
        message={t('project.notAvailable')}
        title={t('project.errors.notFoundTitle')}
      />
    )
  }

  return (
    <ProjectFrame activeTab="experiments" project={projectQuery.data}>
      <button
        className="text-sm font-semibold text-primary hover:underline"
        onClick={() => navigate(runsPath)}
        type="button"
      >
        ← {t('runs.back')}
      </button>

      <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 className="font-heading text-2xl font-semibold text-secondary">
            {t('runs.compare.title')}
          </h2>
          <p className="mt-1 text-sm text-slate-600">
            {t('runs.compare.neutralDescription', {
              count: experimentRunUuids.length,
            })}
          </p>
        </div>
        <button
          className="w-fit rounded-md border border-slate-300 px-4 py-2 text-sm font-semibold text-secondary transition hover:border-primary hover:text-primary"
          onClick={() => navigate(runsPath)}
          type="button"
        >
          {t('runs.compare.changeSelection')}
        </button>
      </div>

      {!canCompareExperimentRuns ? (
        <div className="mt-6">
          <PageState
            message={t('runs.compare.permissionDenied')}
            title={t('runs.unavailableTitle')}
          />
        </div>
      ) : !hasValidAxis || experimentRunUuids.length < 2 ? (
        <div className="mt-6">
          <PageState
            message={t('runs.compare.invalidSelection')}
            title={t('runs.compare.invalidSelectionTitle')}
          />
        </div>
      ) : comparisonQuery.isLoading ? (
        <div className="mt-6">
          <PageSkeleton />
        </div>
      ) : comparisonQuery.error ? (
        <div className="mt-6">
          <PageState
            message={getErrorMessage(
              comparisonQuery.error,
              t('runs.compare.error'),
            )}
            title={t('runs.compare.errorTitle')}
            tone="error"
          />
        </div>
      ) : comparisonQuery.data ? (
        <>
          <div className="mt-6 rounded-lg border border-slate-200 bg-white p-5">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                {t('runs.compare.sharedPipeline')}
              </p>
              <div className="mt-3 flex flex-wrap items-center gap-2">
                {comparisonQuery.data.pipelineSignature.map((step, index) => (
                  <span className="contents" key={`${step}-${index}`}>
                    {index > 0 ? (
                      <span className="text-slate-400">→</span>
                    ) : null}
                    <span className="rounded-md bg-sky-50 px-2.5 py-1.5 text-xs font-semibold text-sky-700">
                      {displayExperimentType(step)}
                    </span>
                  </span>
                ))}
              </div>
            </div>
          </div>
          <ComparisonTable
            comparison={comparisonQuery.data}
            runNames={runNames}
          />
        </>
      ) : null}
    </ProjectFrame>
  )
}
