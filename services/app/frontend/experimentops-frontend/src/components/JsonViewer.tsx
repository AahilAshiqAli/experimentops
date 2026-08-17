import { useMemo, useState, type FormEvent, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

const INLINE_CONTROL_CLASS =
  'inline-block rounded-sm border-none p-0 font-mono outline-none focus:ring-1 focus:ring-primary'

function renderIndent(depth: number) {
  return '  '.repeat(depth)
}

function JsonPrimitiveInput({
  onChange,
  value,
}: {
  onChange: (value: unknown) => void
  value: unknown
}) {
  const { t } = useTranslation()

  if (typeof value === 'boolean') {
    return (
      <select
        className={`${INLINE_CONTROL_CLASS} bg-purple-50 text-purple-700`}
        onChange={(event) => onChange(event.target.value === 'true')}
        value={String(value)}
      >
        <option value="true">{t('common.boolean.true')}</option>
        <option value="false">{t('common.boolean.false')}</option>
      </select>
    )
  }

  if (typeof value === 'number') {
    return (
      <input
        className={`${INLINE_CONTROL_CLASS} w-16 bg-amber-50 px-0.5 text-amber-600`}
        onChange={(event) => {
          const nextValue = event.target.valueAsNumber
          onChange(Number.isNaN(nextValue) ? 0 : nextValue)
        }}
        type="number"
        value={value}
      />
    )
  }

  if (value === null || value === undefined) {
    return (
      <input
        className={`${INLINE_CONTROL_CLASS} w-16 bg-slate-50 px-0.5 italic text-slate-400`}
        onChange={(event) =>
          onChange(event.target.value === '' ? null : event.target.value)
        }
        placeholder="null"
        value=""
      />
    )
  }

  const text = String(value)
  return (
    <span className="text-emerald-700">
      "
      <input
        className={`${INLINE_CONTROL_CLASS} bg-emerald-50 px-0.5 text-emerald-700`}
        onChange={(event) => onChange(event.target.value)}
        size={Math.max(text.length, 1)}
        type="text"
        value={text}
      />
      "
    </span>
  )
}

function JsonNode({
  depth,
  onChange,
  value,
}: {
  depth: number
  onChange: (value: unknown) => void
  value: unknown
}) {
  if (Array.isArray(value)) {
    if (value.length === 0) {
      return <span className="text-slate-500">[]</span>
    }

    return (
      <span>
        <span className="text-slate-500">[</span>
        {'\n'}
        {value.map((item, index) => (
          <span key={index}>
            {renderIndent(depth + 1)}
            <JsonNode
              depth={depth + 1}
              onChange={(next) => {
                const nextArray = [...value]
                nextArray[index] = next
                onChange(nextArray)
              }}
              value={item}
            />
            {index < value.length - 1 ? (
              <span className="text-slate-500">,</span>
            ) : null}
            {'\n'}
          </span>
        ))}
        {renderIndent(depth)}
        <span className="text-slate-500">]</span>
      </span>
    )
  }

  if (typeof value === 'object' && value !== null) {
    const entries = Object.entries(value as Record<string, unknown>)

    if (entries.length === 0) {
      return <span className="text-slate-500">{'{}'}</span>
    }

    return (
      <span>
        <span className="text-slate-500">{'{'}</span>
        {'\n'}
        {entries.map(([key, item], index) => (
          <span key={key}>
            {renderIndent(depth + 1)}
            <span className="text-sky-700">"{key}"</span>
            <span className="text-slate-500">: </span>
            <JsonNode
              depth={depth + 1}
              onChange={(next) =>
                onChange({
                  ...(value as Record<string, unknown>),
                  [key]: next,
                })
              }
              value={item}
            />
            {index < entries.length - 1 ? (
              <span className="text-slate-500">,</span>
            ) : null}
            {'\n'}
          </span>
        ))}
        {renderIndent(depth)}
        <span className="text-slate-500">{'}'}</span>
      </span>
    )
  }

  return <JsonPrimitiveInput onChange={onChange} value={value} />
}

export function JsonEditor({
  onChange,
  value,
}: {
  onChange: (value: unknown) => void
  value: unknown
}) {
  return (
    <pre className="max-h-[60vh] overflow-auto whitespace-pre rounded-lg border border-slate-200 bg-white p-4 font-mono text-xs leading-relaxed text-slate-700">
      <JsonNode depth={0} onChange={onChange} value={value} />
    </pre>
  )
}

export function JsonEditorDialog({
  getValidationErrors,
  headerContent,
  isSubmitting = false,
  isValidationLoading = false,
  onClose,
  onSubmit,
  resetValue,
  title,
  value,
}: {
  getValidationErrors?: (value: unknown) => string[]
  headerContent?: ReactNode
  isSubmitting?: boolean
  isValidationLoading?: boolean
  onClose: () => void
  onSubmit: (value: unknown) => void
  resetValue?: unknown
  title: string
  value: unknown
}) {
  const { t } = useTranslation()
  const [draft, setDraft] = useState(value)
  const validationErrors = useMemo(
    () => getValidationErrors?.(draft) ?? [],
    [draft, getValidationErrors],
  )
  const hasValidationErrors = validationErrors.length > 0

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (isValidationLoading || hasValidationErrors) return

    onSubmit(draft)
  }

  const handleReset = () => {
    if (resetValue === undefined) return
    setDraft(resetValue)
  }

  return (
    <div
      aria-labelledby="json-editor-title"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-secondary/40 p-4"
      role="dialog"
    >
      <div className="max-h-[calc(100vh-2rem)] w-full max-w-2xl overflow-y-auto rounded-xl bg-white p-6 shadow-2xl">
        <div className="flex items-center justify-between gap-4">
          <h2
            className="font-heading text-xl font-semibold text-secondary"
            id="json-editor-title"
          >
            {title}
          </h2>
          <button
            aria-label={t('common.jsonEditor.close')}
            className="rounded-md p-2 text-slate-500 hover:bg-slate-100"
            disabled={isSubmitting}
            onClick={onClose}
            type="button"
          >
            ×
          </button>
        </div>

        {headerContent ? <div className="mt-4">{headerContent}</div> : null}

        <form className="mt-4" onSubmit={handleSubmit}>
          <JsonEditor onChange={setDraft} value={draft} />
          {isValidationLoading ? (
            <p className="mt-3 rounded-md bg-sky-50 px-3 py-2 text-sm text-sky-800">
              {t('common.jsonEditor.loadingValidation')}
            </p>
          ) : null}
          {hasValidationErrors ? (
            <div className="mt-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              <p className="font-semibold">
                {t('common.jsonEditor.fixValidation')}
              </p>
              <ul className="mt-1 list-disc space-y-1 pl-5">
                {validationErrors.map((error) => (
                  <li key={error}>{error}</li>
                ))}
              </ul>
            </div>
          ) : null}
          <div className="mt-5 flex items-center justify-between gap-3">
            <div>
              {resetValue !== undefined ? (
                <button
                  className="rounded-md border border-primary/30 px-4 py-2 text-sm font-semibold text-primary transition hover:bg-primary/5 disabled:cursor-not-allowed disabled:opacity-60"
                  disabled={isSubmitting || isValidationLoading}
                  onClick={handleReset}
                  type="button"
                >
                  {t('configs.resetToDefaults')}
                </button>
              ) : null}
            </div>
            <div className="flex gap-3">
              <button
                className="rounded-md border border-slate-300 px-4 py-2 text-sm font-semibold text-secondary hover:bg-slate-50"
                disabled={isSubmitting}
                onClick={onClose}
                type="button"
              >
                {t('common.actions.cancel')}
              </button>
              <button
                className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={
                  isSubmitting || isValidationLoading || hasValidationErrors
                }
                type="submit"
              >
                {isSubmitting
                  ? t('common.jsonEditor.saving')
                  : t('common.actions.submit')}
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  )
}
