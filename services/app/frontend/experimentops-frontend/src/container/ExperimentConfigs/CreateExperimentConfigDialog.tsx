import { useMemo, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'

import { ExperimentTypeSelect } from '../../components/ExperimentTypeSelect'
import { JsonEditor } from '../../components/JsonViewer'
import type { ExperimentType } from '../../services/experimentType.service'
import { getExperimentConfigValidationErrors } from './configValidation'
import { buildExperimentTypeDefaultConfig } from './experimentConfigDefaults'
import { ExperimentTypeIoSummary } from './ExperimentTypeIoSummary'

export function CreateExperimentConfigDialog({
  isPending,
  onClose,
  onSubmit,
}: {
  isPending: boolean
  onClose: () => void
  onSubmit: (input: {
    config: Record<string, unknown>
    experimentType: string
    name: string
  }) => void
}) {
  const { t } = useTranslation()
  const [name, setName] = useState('')
  const [selectedType, setSelectedType] = useState<ExperimentType | null>(null)
  const [configDraft, setConfigDraft] = useState<Record<
    string,
    unknown
  > | null>(null)
  const regexValidationErrors = useMemo(
    () =>
      configDraft
        ? getExperimentConfigValidationErrors(configDraft, selectedType)
        : [],
    [configDraft, selectedType],
  )
  const hasValidationErrors = regexValidationErrors.length > 0

  const handleSelectType = (experimentType: ExperimentType | null) => {
    setSelectedType(experimentType)
    setConfigDraft(
      experimentType ? buildExperimentTypeDefaultConfig(experimentType) : null,
    )
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selectedType || !configDraft || hasValidationErrors) return

    onSubmit({
      config: configDraft,
      experimentType: selectedType.name,
      name: name.trim(),
    })
  }

  return (
    <div
      aria-labelledby="create-experiment-config-title"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-secondary/40 p-4"
      role="dialog"
    >
      <div className="max-h-[calc(100vh-2rem)] w-full max-w-lg overflow-y-auto rounded-xl bg-white p-6 shadow-2xl">
        <div className="flex items-center justify-between gap-4">
          <h2
            className="font-heading text-2xl font-semibold text-secondary"
            id="create-experiment-config-title"
          >
            {t('configs.add')}
          </h2>
          <button
            aria-label={t('configs.closeDialog')}
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
            {t('configs.name')}
            <input
              autoFocus
              className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
              disabled={isPending}
              onChange={(event) => setName(event.target.value)}
              required
              value={name}
            />
          </label>

          <ExperimentTypeSelect
            disabled={isPending}
            label={t('configs.experimentTypeLabel')}
            onChange={handleSelectType}
            required
            selectedType={selectedType}
          />

          {selectedType ? (
            <ExperimentTypeIoSummary manifests={selectedType.formatMappings} />
          ) : null}

          {configDraft ? (
            <div>
              <div className="flex items-center justify-between gap-3">
                <span className="block text-sm font-medium text-secondary">
                  {t('configs.config')}
                </span>
                <button
                  className="text-xs font-semibold text-primary hover:text-primary/80 disabled:cursor-not-allowed disabled:opacity-60"
                  disabled={isPending || !selectedType}
                  onClick={() =>
                    selectedType &&
                    setConfigDraft(
                      buildExperimentTypeDefaultConfig(selectedType),
                    )
                  }
                  type="button"
                >
                  {t('configs.resetToDefaults')}
                </button>
              </div>
              <div className="mt-1">
                <JsonEditor
                  onChange={(value) =>
                    setConfigDraft(value as Record<string, unknown>)
                  }
                  value={configDraft}
                />
              </div>
              {hasValidationErrors ? (
                <div className="mt-2 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                  <p className="font-semibold">{t('configs.fixValidation')}</p>
                  <ul className="mt-1 list-disc space-y-1 pl-5">
                    {regexValidationErrors.map((error) => (
                      <li key={error}>{error}</li>
                    ))}
                  </ul>
                </div>
              ) : null}
            </div>
          ) : null}

          <div className="flex justify-end gap-3 pt-2">
            <button
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-semibold text-secondary hover:bg-slate-50"
              disabled={isPending}
              onClick={onClose}
              type="button"
            >
              {t('common.actions.cancel')}
            </button>
            <button
              className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={
                isPending || !configDraft || !name.trim() || hasValidationErrors
              }
              type="submit"
            >
              {isPending ? t('configs.creating') : t('configs.create')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
