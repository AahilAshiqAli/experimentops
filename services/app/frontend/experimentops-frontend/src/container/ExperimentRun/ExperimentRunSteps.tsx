import type { ExperimentConfigDetail } from '../../services/experimentConfig.service'
import type {
  ExperimentRunDetailStep,
  ExperimentRunResolvedInput,
  ExperimentRunStepOutput,
} from '../../services/experimentRun.service'
import { formatLabel } from '../ProjectDetails/projectDetails.utils'

function DatasetInputIcon() {
  return (
    <svg aria-hidden="true" className="h-4 w-4" fill="none" viewBox="0 0 24 24">
      <ellipse
        cx="12"
        cy="6"
        rx="6.5"
        ry="3"
        stroke="currentColor"
        strokeWidth="1.8"
      />
      <path
        d="M5.5 6v5.8c0 1.7 2.9 3 6.5 3s6.5-1.3 6.5-3V6m-13 5.8v5.5c0 1.7 2.9 3 6.5 3s6.5-1.3 6.5-3v-5.5"
        stroke="currentColor"
        strokeWidth="1.8"
      />
    </svg>
  )
}

function FileIcon() {
  return (
    <svg aria-hidden="true" className="h-4 w-4" fill="none" viewBox="0 0 24 24">
      <path
        d="M7 3h7l4 4v14H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z"
        stroke="currentColor"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
      <path
        d="M14 3v5h4"
        stroke="currentColor"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  )
}

function FormatBadge({ format }: { format?: string | null }) {
  if (!format) return null
  return (
    <span className="rounded bg-indigo-50 px-1.5 py-0.5 text-[10px] font-semibold uppercase text-indigo-600">
      {format}
    </span>
  )
}

function InputItem({ input }: { input: ExperimentRunResolvedInput }) {
  return (
    <div
      className="flex min-w-0 items-center gap-2 rounded-md border border-slate-200 bg-white px-3 py-2 shadow-sm"
      title={`${input.portName} · ${input.fileUuid}`}
    >
      <span className="shrink-0 text-primary">
        {input.inputType === 'DATASET' ? <DatasetInputIcon /> : <FileIcon />}
      </span>
      <span className="min-w-0 flex-1 truncate text-xs font-medium text-secondary">
        {input.name}
      </span>
      <FormatBadge format={input.format} />
    </div>
  )
}

function OutputItem({ output }: { output: ExperimentRunStepOutput }) {
  return (
    <div
      className="flex min-w-0 items-center gap-2 rounded-md border border-slate-200 bg-white px-3 py-2 shadow-sm"
      title={`${output.portName} · ${output.artifactUuid}`}
    >
      <span className="shrink-0 text-primary">
        <FileIcon />
      </span>
      <span className="min-w-0 flex-1 truncate text-xs font-medium text-secondary">
        {output.name}
      </span>
      <FormatBadge format={output.format} />
    </div>
  )
}

function ConfigurationPreview({
  config,
  error,
  isLoading,
}: {
  config?: ExperimentConfigDetail
  error: unknown
  isLoading: boolean
}) {
  if (isLoading) {
    return <div className="mt-2 h-24 animate-pulse rounded-md bg-slate-100" />
  }
  if (error) {
    return (
      <p className="mt-2 text-xs text-red-600">
        Unable to load this configuration.
      </p>
    )
  }
  if (!config) {
    return (
      <p className="mt-2 text-xs text-slate-500">
        Configuration is not available.
      </p>
    )
  }

  return (
    <pre className="mt-2 max-h-40 overflow-auto whitespace-pre-wrap break-words rounded-md border border-slate-200 bg-slate-50 p-3 font-mono text-[11px] leading-relaxed text-slate-700">
      {JSON.stringify(config.config, null, 2)}
    </pre>
  )
}

function StepCard({
  config,
  configError,
  isConfigLoading,
  step,
}: {
  config?: ExperimentConfigDetail
  configError: unknown
  isConfigLoading: boolean
  step: ExperimentRunDetailStep
}) {
  return (
    <article className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <header className="flex items-center gap-3 border-b border-slate-200 px-5 py-3.5">
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary text-sm font-bold text-white shadow-sm">
          {step.stepCount}
        </span>
        <h3 className="font-heading text-lg font-semibold text-secondary">
          {formatLabel(step.experimentType)}
        </h3>
      </header>

      <div className="grid divide-y divide-slate-200 lg:grid-cols-3 lg:divide-x lg:divide-y-0">
        <section className="min-w-0 p-5">
          <p className="text-xs font-semibold text-slate-500">Experiment</p>
          <p className="mt-1 break-words text-sm font-semibold text-secondary">
            {config?.name ?? formatLabel(step.experimentType)}
          </p>
          <p className="mt-4 text-xs font-semibold text-slate-500">
            Configuration
          </p>
          <ConfigurationPreview
            config={config}
            error={configError}
            isLoading={isConfigLoading}
          />
        </section>

        <section className="min-w-0 p-5">
          <h4 className="text-sm font-semibold text-secondary">
            Resolved inputs
          </h4>
          {step.inputs.length ? (
            <div className="mt-3 space-y-2">
              {step.inputs.map((input, index) => (
                <InputItem
                  input={input}
                  key={`${input.portName}:${input.fileUuid}:${index}`}
                />
              ))}
            </div>
          ) : (
            <p className="mt-3 text-xs text-slate-500">No resolved inputs.</p>
          )}
        </section>

        <section className="min-w-0 p-5">
          <h4 className="text-sm font-semibold text-secondary">
            Produced outputs
          </h4>
          {step.outputs.length ? (
            <div className="mt-3 space-y-2">
              {step.outputs.map((output) => (
                <OutputItem output={output} key={output.artifactUuid} />
              ))}
            </div>
          ) : (
            <p className="mt-3 text-xs text-slate-500">No produced outputs.</p>
          )}
        </section>
      </div>
    </article>
  )
}

export function ExperimentRunSteps({
  configError,
  configs,
  isConfigLoading,
  steps,
}: {
  configError: unknown
  configs: ExperimentConfigDetail[]
  isConfigLoading: boolean
  steps: ExperimentRunDetailStep[]
}) {
  if (!steps.length) {
    return (
      <div className="mt-4 rounded-xl border border-dashed border-slate-300 bg-white px-6 py-12 text-center text-sm text-slate-500">
        No steps are available for this run.
      </div>
    )
  }

  const orderedSteps = [...steps].sort(
    (left, right) => left.stepCount - right.stepCount,
  )

  return (
    <div className="mt-4 space-y-4" role="tabpanel">
      {orderedSteps.map((step) => (
        <StepCard
          config={configs.find(
            (config) => config.uuid === step.experimentConfigUuid,
          )}
          configError={configError}
          isConfigLoading={isConfigLoading}
          key={`${step.stepCount}:${step.experimentConfigUuid}`}
          step={step}
        />
      ))}
    </div>
  )
}
