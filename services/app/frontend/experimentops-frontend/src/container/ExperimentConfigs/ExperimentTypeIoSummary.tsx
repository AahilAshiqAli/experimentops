import { useTranslation } from 'react-i18next'

import type {
  ExperimentTypeInputRelationship,
  ExperimentTypeManifest,
  ExperimentTypeOutputManifest,
} from '../../services/experimentType.service'

function humanizeEnum(value: string) {
  return value
    .trim()
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((word) => `${word.charAt(0).toUpperCase()}${word.slice(1)}`)
    .join(' ')
}

function OutputFormat({ output }: { output: ExperimentTypeOutputManifest }) {
  const { t } = useTranslation()
  const strategy = output.type.type.trim().toUpperCase()

  if (strategy === 'FIXED' && output.type.format) {
    return (
      <span className="rounded-full bg-slate-100 px-2 py-0.5 font-mono text-[11px] font-semibold text-slate-700">
        {output.type.format}
      </span>
    )
  }

  if (output.type.sourceInputPort) {
    return (
      <span className="text-xs text-slate-500">
        {t('configs.io.matchesInput', {
          port: output.type.sourceInputPort,
        })}
      </span>
    )
  }

  return (
    <span className="text-xs text-slate-500">
      {humanizeEnum(output.type.type)}
    </span>
  )
}

function Relationship({
  relationship,
}: {
  relationship: ExperimentTypeInputRelationship
}) {
  const { t } = useTranslation()

  return (
    <p className="rounded-md border border-sky-100 bg-sky-50 px-2.5 py-2 text-xs text-sky-800">
      {relationship.type.trim().toUpperCase() === 'SAME_FORMAT'
        ? t('configs.io.sameFormat', {
            ports: relationship.ports.join(', '),
          })
        : `${humanizeEnum(relationship.type)}: ${relationship.ports.join(', ')}`}
    </p>
  )
}

function Manifest({
  manifest,
  optionNumber,
  showOptionTitle,
}: {
  manifest: ExperimentTypeManifest
  optionNumber: number
  showOptionTitle: boolean
}) {
  const { t } = useTranslation()

  return (
    <section>
      {showOptionTitle ? (
        <h4 className="mb-3 text-xs font-bold uppercase tracking-wide text-slate-500">
          {t('configs.io.option', { number: optionNumber })}
        </h4>
      ) : null}

      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <h4 className="text-xs font-bold uppercase tracking-wide text-slate-500">
            {t('configs.io.inputs')}
          </h4>
          <div className="mt-2 space-y-2">
            {manifest.inputs.map((input) => (
              <div
                className="rounded-md border border-slate-200 bg-white p-2.5"
                key={input.portName}
              >
                <div>
                  <span className="block break-words font-mono text-xs font-semibold text-secondary">
                    {input.portName}
                  </span>
                  <span
                    className={`mt-1 inline-flex rounded-full px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide ${
                      input.required
                        ? 'bg-primary/10 text-primary'
                        : 'bg-slate-100 text-slate-500'
                    }`}
                  >
                    {input.required
                      ? t('configs.io.required')
                      : t('configs.io.optional')}
                  </span>
                </div>
                <div className="mt-1.5 flex flex-wrap items-center gap-1.5 text-xs text-slate-500">
                  <span>{humanizeEnum(input.contract.dataKind)}</span>
                  <span aria-hidden="true">·</span>
                  <span>
                    {input.cardinality.trim().toUpperCase() === 'ONE'
                      ? t('configs.io.single')
                      : humanizeEnum(input.cardinality)}
                  </span>
                </div>
                <div className="mt-2 flex flex-wrap gap-1">
                  {input.contract.acceptedFormats.length ? (
                    input.contract.acceptedFormats.map((format, index) => (
                      <span
                        className="rounded-full bg-slate-100 px-2 py-0.5 font-mono text-[11px] font-semibold text-slate-700"
                        key={`${format}-${index}`}
                      >
                        {format}
                      </span>
                    ))
                  ) : (
                    <span className="text-xs text-slate-500">
                      {t('configs.io.anyFormat')}
                    </span>
                  )}
                </div>
              </div>
            ))}
            {manifest.inputRelationships.map((relationship, index) => (
              <Relationship
                key={`${relationship.type}-${relationship.ports.join('-')}-${index}`}
                relationship={relationship}
              />
            ))}
          </div>
        </div>

        <div>
          <h4 className="text-xs font-bold uppercase tracking-wide text-slate-500">
            {t('configs.io.outputs')}
          </h4>
          <div className="mt-2 space-y-2">
            {manifest.outputs.map((output) => {
              const policy = output.downStreamPolicy.trim().toUpperCase()

              return (
                <div
                  className="rounded-md border border-slate-200 bg-white p-2.5"
                  key={output.name}
                >
                  <div>
                    <span className="block break-words font-mono text-xs font-semibold text-secondary">
                      {output.name}
                    </span>
                    <span
                      className={`mt-1 inline-flex rounded-full px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide ${
                        output.required
                          ? 'bg-primary/10 text-primary'
                          : 'bg-slate-100 text-slate-500'
                      }`}
                    >
                      {output.required
                        ? t('configs.io.required')
                        : t('configs.io.optional')}
                    </span>
                  </div>
                  <div className="mt-1.5 flex flex-wrap items-center gap-1.5 text-xs text-slate-500">
                    <span>{humanizeEnum(output.dataKind)}</span>
                    <span aria-hidden="true">·</span>
                    <OutputFormat output={output} />
                  </div>
                  <span
                    className={`mt-2 inline-flex rounded-full px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide ${
                      policy === 'TERMINAL'
                        ? 'bg-amber-100 text-amber-800'
                        : policy === 'CONNECTABLE'
                          ? 'bg-emerald-100 text-emerald-800'
                          : 'bg-slate-100 text-slate-600'
                    }`}
                  >
                    {policy === 'TERMINAL'
                      ? t('configs.io.terminal')
                      : policy === 'CONNECTABLE'
                        ? t('configs.io.connectable')
                        : humanizeEnum(output.downStreamPolicy)}
                  </span>
                </div>
              )
            })}
          </div>
        </div>
      </div>
    </section>
  )
}

export function ExperimentTypeIoSummary({
  manifests,
}: {
  manifests: ExperimentTypeManifest[]
}) {
  const { t } = useTranslation()
  const firstManifest = manifests[0]
  const summary = firstManifest
    ? manifests.length === 1
      ? `${t('configs.io.inputCount', { count: firstManifest.inputs.length })} · ${t('configs.io.outputCount', { count: firstManifest.outputs.length })}`
      : t('configs.io.optionCount', { count: manifests.length })
    : t('configs.io.empty')

  return (
    <details
      className="group rounded-lg border border-slate-200 bg-slate-50"
      open
    >
      <summary className="flex cursor-pointer list-none items-center justify-between gap-3 p-3 marker:content-none">
        <span>
          <span className="block text-sm font-semibold text-secondary">
            {t('configs.io.title')}
          </span>
          <span className="mt-0.5 block text-xs text-slate-500">{summary}</span>
        </span>
        <span
          aria-hidden="true"
          className="text-slate-400 transition-transform group-open:rotate-180"
        >
          ⌄
        </span>
      </summary>

      {manifests.length ? (
        <div className="max-h-96 space-y-5 overflow-y-auto border-t border-slate-200 p-3">
          {manifests.map((manifest, index) => (
            <Manifest
              key={index}
              manifest={manifest}
              optionNumber={index + 1}
              showOptionTitle={manifests.length > 1}
            />
          ))}
        </div>
      ) : null}
    </details>
  )
}
