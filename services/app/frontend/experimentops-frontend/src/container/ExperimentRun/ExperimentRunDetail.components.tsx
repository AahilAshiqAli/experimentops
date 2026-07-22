import { useState, type ReactNode } from 'react'

import type {
  ExperimentRunDetail,
  ExperimentRunStatus,
} from '../../services/experimentRun.service'
import { formatLabel } from '../ProjectDetails/projectDetails.utils'

type IconProps = { className?: string }

export function CheckIcon({ className = 'h-5 w-5' }: IconProps) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="none"
      viewBox="0 0 24 24"
    >
      <path
        d="m5 12.5 4.2 4.2L19 7"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      />
    </svg>
  )
}

export function CrossIcon({ className = 'h-5 w-5' }: IconProps) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="none"
      viewBox="0 0 24 24"
    >
      <path
        d="m7 7 10 10M17 7 7 17"
        stroke="currentColor"
        strokeLinecap="round"
        strokeWidth="2"
      />
    </svg>
  )
}

export function StepsIcon({ className = 'h-5 w-5' }: IconProps) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="none"
      viewBox="0 0 24 24"
    >
      <circle cx="12" cy="12" r="8" stroke="currentColor" strokeWidth="2" />
      <path
        d="M12 7v5l3 2"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      />
    </svg>
  )
}

export function ArtifactIcon({ className = 'h-5 w-5' }: IconProps) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="none"
      viewBox="0 0 24 24"
    >
      <path
        d="m12 3 8 4.5v9L12 21l-8-4.5v-9L12 3Z"
        stroke="currentColor"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
      <path
        d="m4.5 7.8 7.5 4.3 7.5-4.3M12 12v8.5"
        stroke="currentColor"
        strokeWidth="1.8"
      />
    </svg>
  )
}

function DatasetIcon({ className = 'h-5 w-5' }: IconProps) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="none"
      viewBox="0 0 24 24"
    >
      <ellipse
        cx="12"
        cy="5.5"
        rx="7"
        ry="3"
        stroke="currentColor"
        strokeWidth="1.8"
      />
      <path
        d="M5 5.5v6c0 1.7 3.1 3 7 3s7-1.3 7-3v-6M5 11.5v6c0 1.7 3.1 3 7 3s7-1.3 7-3v-6"
        stroke="currentColor"
        strokeWidth="1.8"
      />
    </svg>
  )
}

function ArrowIcon() {
  return (
    <svg
      aria-hidden="true"
      className="h-6 w-9 shrink-0 text-slate-400"
      fill="none"
      viewBox="0 0 36 24"
    >
      <path
        d="M2 12h30m-7-7 7 7-7 7"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  )
}

const statusStyles: Record<ExperimentRunStatus, string> = {
  FAILED: 'border-red-200 bg-red-50 text-red-700',
  PENDING: 'border-amber-200 bg-amber-50 text-amber-700',
  RUNNING: 'border-blue-200 bg-blue-50 text-blue-700',
  SUCCEEDED: 'border-emerald-200 bg-emerald-50 text-emerald-700',
}

const statusDotStyles: Record<ExperimentRunStatus, string> = {
  FAILED: 'bg-red-500',
  PENDING: 'bg-amber-500',
  RUNNING: 'bg-blue-500',
  SUCCEEDED: 'bg-emerald-500',
}

export function RunStatusBadge({ status }: { status: ExperimentRunStatus }) {
  return (
    <span
      className={`inline-flex items-center gap-2 rounded-md border px-3 py-1.5 text-xs font-semibold ${statusStyles[status]}`}
    >
      <span className={`h-2 w-2 rounded-full ${statusDotStyles[status]}`} />
      {formatLabel(status)}
    </span>
  )
}

export function SummaryCard({
  icon,
  label,
  tone = 'primary',
  value,
}: {
  icon: ReactNode
  label: string
  tone?: 'danger' | 'primary' | 'success' | 'warning'
  value: ReactNode
}) {
  const tones = {
    danger: 'bg-red-50 text-red-600',
    primary: 'bg-sky-50 text-primary',
    success: 'bg-emerald-50 text-emerald-600',
    warning: 'bg-amber-50 text-amber-600',
  }

  return (
    <article className="flex min-h-24 items-center gap-4 rounded-xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
      <span
        className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-full ${tones[tone]}`}
      >
        {icon}
      </span>
      <div className="min-w-0">
        <p className="text-xs font-medium text-slate-500">{label}</p>
        <div className="mt-1 truncate text-lg font-semibold text-secondary">
          {value}
        </div>
      </div>
    </article>
  )
}

export function InfoRow({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="grid grid-cols-[minmax(6.25rem,0.8fr)_minmax(0,1.2fr)] gap-4 py-3.5 text-sm">
      <dt className="text-slate-500">{label}</dt>
      <dd className="min-w-0 break-words font-medium text-secondary">
        {value}
      </dd>
    </div>
  )
}

export function CopyableRunId({ uuid }: { uuid: string }) {
  const [copied, setCopied] = useState(false)

  const handleCopy = async () => {
    await navigator.clipboard.writeText(uuid)
    setCopied(true)
    window.setTimeout(() => setCopied(false), 1500)
  }

  return (
    <span className="flex items-start gap-2">
      <span className="break-all font-mono text-xs leading-5">{uuid}</span>
      <button
        aria-label="Copy run ID"
        className="shrink-0 rounded p-1 text-slate-400 transition hover:bg-slate-100 hover:text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
        onClick={handleCopy}
        title={copied ? 'Copied' : 'Copy run ID'}
        type="button"
      >
        {copied ? (
          <CheckIcon className="h-4 w-4 text-emerald-600" />
        ) : (
          <svg
            aria-hidden="true"
            className="h-4 w-4"
            fill="none"
            viewBox="0 0 24 24"
          >
            <rect
              height="13"
              rx="2"
              stroke="currentColor"
              strokeWidth="1.8"
              width="11"
              x="9"
              y="8"
            />
            <path
              d="M7 16H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"
              stroke="currentColor"
              strokeWidth="1.8"
            />
          </svg>
        )}
      </button>
    </span>
  )
}

function PipelineNode({
  children,
  eyebrow,
  icon,
}: {
  children: ReactNode
  eyebrow: string
  icon: ReactNode
}) {
  return (
    <article className="min-w-52 max-w-64 flex-1 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-start gap-3">
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-sky-50 text-primary">
          {icon}
        </span>
        <div className="min-w-0">
          <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">
            {eyebrow}
          </p>
          {children}
        </div>
      </div>
    </article>
  )
}

export function PipelineFlow({ run }: { run: ExperimentRunDetail }) {
  const orderedSteps = [...run.executionMode].sort(
    (left, right) => left.stepCount - right.stepCount,
  )

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <h2 className="font-heading text-lg font-semibold text-secondary">
        Pipeline flow
      </h2>
      <div className="mt-4 overflow-x-auto pb-2">
        <div className="flex min-w-max items-center gap-3">
          <PipelineNode eyebrow="Dataset inputs" icon={<DatasetIcon />}>
            {run.datasets.length ? (
              <div className="mt-1.5 space-y-1.5">
                {run.datasets.map((dataset) => (
                  <p
                    className="max-w-44 truncate text-sm font-semibold text-secondary"
                    key={dataset.datasetVersionUuid}
                    title={dataset.name}
                  >
                    {dataset.name}
                  </p>
                ))}
              </div>
            ) : (
              <p className="mt-1.5 text-sm text-slate-500">No datasets</p>
            )}
          </PipelineNode>

          {orderedSteps.map((step) => (
            <div className="flex items-center gap-3" key={step.stepCount}>
              <ArrowIcon />
              <PipelineNode
                eyebrow={`Step ${step.stepCount}`}
                icon={
                  <span className="text-sm font-bold">{step.stepCount}</span>
                }
              >
                <p className="mt-1.5 max-w-44 break-words text-sm font-semibold text-secondary">
                  {formatLabel(step.experimentType)}
                </p>
                <p className="mt-1 text-xs text-slate-500">
                  {step.inputs.length} input
                  {step.inputs.length === 1 ? '' : 's'}
                </p>
              </PipelineNode>
            </div>
          ))}

          <ArrowIcon />
          <PipelineNode eyebrow="Outputs" icon={<ArtifactIcon />}>
            {run.runArtifacts.length ? (
              <div className="mt-1.5 space-y-1.5">
                {run.runArtifacts.slice(0, 3).map((artifact) => (
                  <p
                    className="max-w-44 truncate text-sm font-semibold text-secondary"
                    key={artifact.uuid}
                    title={artifact.portName}
                  >
                    {artifact.portName}
                  </p>
                ))}
                {run.runArtifacts.length > 3 ? (
                  <p className="text-xs text-slate-500">
                    +{run.runArtifacts.length - 3} more
                  </p>
                ) : null}
              </div>
            ) : (
              <p className="mt-1.5 text-sm text-slate-500">No artifacts</p>
            )}
          </PipelineNode>
        </div>
      </div>
    </section>
  )
}

type RunDetailTab = 'Artifacts' | 'Logs' | 'Steps'

const detailTabs: Array<{ label: RunDetailTab }> = [
  { label: 'Steps' },
  { label: 'Artifacts' },
  { label: 'Logs' },
]

export function RunDetailTabs({
  artifactsContent,
  logsContent,
  stepsContent,
}: {
  artifactsContent: ReactNode
  logsContent: ReactNode
  stepsContent: ReactNode
}) {
  const [activeTab, setActiveTab] = useState<RunDetailTab>('Steps')
  return (
    <section>
      <div className="flex gap-7 border-b border-slate-200" role="tablist">
        {detailTabs.map((tab) => (
          <button
            aria-selected={activeTab === tab.label}
            className={`border-b-2 px-1 pb-3 pt-1 text-sm font-semibold transition ${
              activeTab === tab.label
                ? 'border-primary text-primary'
                : 'border-transparent text-slate-500 hover:text-secondary'
            }`}
            key={tab.label}
            onClick={() => setActiveTab(tab.label)}
            role="tab"
            type="button"
          >
            {tab.label}
          </button>
        ))}
      </div>
      {activeTab === 'Steps'
        ? stepsContent
        : activeTab === 'Artifacts'
          ? artifactsContent
          : logsContent}
    </section>
  )
}
