import { useMemo, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'

import { JsonEditor } from '../../components/JsonViewer'
import type { ExperimentType } from '../../services/experimentType.service'
import { getExperimentConfigValidationErrors } from './configValidation'

function buildDefaultConfig(experimentType: ExperimentType) {
  return Object.fromEntries(
    experimentType.defaultConfig.map((field) => [
      field.name,
      field.defaultValue,
    ]),
  ) as Record<string, unknown>
}

export function CreateExperimentConfigDialog({
  experimentTypes,
  isLoadingExperimentTypes,
  isPending,
  onClose,
  onSubmit,
}: {
  experimentTypes: ExperimentType[]
  isLoadingExperimentTypes: boolean
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
  const [selectedTypeUuid, setSelectedTypeUuid] = useState('')
  const [configDraft, setConfigDraft] = useState<Record<
    string,
    unknown
  > | null>(null)
  const selectedType =
    experimentTypes.find((type) => type.uuid === selectedTypeUuid) ?? null
  const regexValidationErrors = useMemo(
    () =>
      configDraft
        ? getExperimentConfigValidationErrors(configDraft, selectedType)
        : [],
    [configDraft, selectedType],
  )
  const hasValidationErrors = regexValidationErrors.length > 0

  const handleSelectType = (uuid: string) => {
    setSelectedTypeUuid(uuid)
    const experimentType = experimentTypes.find((type) => type.uuid === uuid)
    setConfigDraft(experimentType ? buildDefaultConfig(experimentType) : null)
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
      <div className="w-full max-w-lg rounded-xl bg-white p-6 shadow-2xl">
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

          <label className="block text-sm font-medium text-secondary">
            {t('configs.experimentTypeLabel')}
            <select
              className="mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
              disabled={isPending || isLoadingExperimentTypes}
              onChange={(event) => handleSelectType(event.target.value)}
              required
              value={selectedTypeUuid}
            >
              <option disabled value="">
                {isLoadingExperimentTypes
                  ? t('configs.loadingTypes')
                  : t('configs.selectType')}
              </option>
              {experimentTypes.map((experimentType) => (
                <option key={experimentType.uuid} value={experimentType.uuid}>
                  {experimentType.name}
                </option>
              ))}
            </select>
          </label>

          {selectedType ? (
            <div className="rounded-lg border border-slate-200 bg-slate-50 p-3">
              <span className="block text-sm font-semibold text-secondary">
                {t('configs.supportedFormats')}
              </span>
              {selectedType.formatMappings.length ? (
                <div className="mt-2 flex flex-wrap gap-2">
                  {selectedType.formatMappings.map((mapping) => (
                    <span
                      className="inline-flex items-center gap-1 rounded-full border border-primary/20 bg-white px-2.5 py-1 text-xs font-semibold text-secondary"
                      key={`${mapping.inputFormat}-${mapping.outputFormat}`}
                    >
                      <span>{mapping.inputFormat}</span>
                      <span className="text-slate-400">→</span>
                      <span>{mapping.outputFormat}</span>
                    </span>
                  ))}
                </div>
              ) : (
                <p className="mt-1 text-sm text-slate-500">
                  {t('configs.noFormatRestrictions')}
                </p>
              )}
            </div>
          ) : null}

          {configDraft ? (
            <div>
              <span className="block text-sm font-medium text-secondary">
                {t('configs.config')}
              </span>
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
