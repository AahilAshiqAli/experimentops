import { useTranslation } from 'react-i18next'

import type { ExperimentRunComparisonAxis } from '../../services/experimentRun.service'

const AXIS_COPY = {
  CONFIG: {
    description: 'runs.compare.axisDescriptionText.config',
    label: 'runs.compare.axis.config',
  },
  DATASET: {
    description: 'runs.compare.axisDescriptionText.dataset',
    label: 'runs.compare.axis.dataset',
  },
} as const

export function CompareExperimentRunsDialog({
  axis,
  isComparing,
  onAxisChange,
  onClose,
  onCompare,
  selectedCount,
}: {
  axis: ExperimentRunComparisonAxis
  isComparing: boolean
  onAxisChange: (axis: ExperimentRunComparisonAxis) => void
  onClose: () => void
  onCompare: () => void
  selectedCount: number
}) {
  const { t } = useTranslation()

  return (
    <div
      aria-labelledby="compare-runs-dialog-title"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-secondary/40 p-4"
      role="dialog"
    >
      <div className="w-full max-w-lg rounded-xl bg-white p-6 shadow-2xl">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2
              className="font-heading text-2xl font-semibold text-secondary"
              id="compare-runs-dialog-title"
            >
              {t('runs.compare.chooseAxis')}
            </h2>
            <p className="mt-1 text-sm leading-6 text-slate-600">
              {t('runs.compare.axisDescription', { count: selectedCount })}
            </p>
          </div>
          <button
            aria-label={t('runs.compare.closeDialog')}
            className="rounded-md p-2 text-slate-500 hover:bg-slate-100"
            disabled={isComparing}
            onClick={onClose}
            type="button"
          >
            ×
          </button>
        </div>

        <fieldset className="mt-5 space-y-3">
          <legend className="sr-only">{t('runs.compare.chooseAxis')}</legend>
          {(['CONFIG', 'DATASET'] as const).map((option) => (
            <label
              className={`block cursor-pointer rounded-lg border p-4 transition ${
                axis === option
                  ? 'border-primary ring-2 ring-primary/15'
                  : 'border-slate-200 hover:border-slate-300'
              }`}
              key={option}
            >
              <span className="flex items-center gap-2">
                <input
                  checked={axis === option}
                  className="h-4 w-4 border-slate-300 text-primary focus:ring-primary"
                  disabled={isComparing}
                  name="comparison-axis"
                  onChange={() => onAxisChange(option)}
                  type="radio"
                  value={option}
                />
                <span className="text-sm font-semibold text-secondary">
                  {t(AXIS_COPY[option].label)}
                </span>
              </span>
              <span className="mt-1 block pl-6 text-sm text-slate-600">
                {t(AXIS_COPY[option].description)}
              </span>
            </label>
          ))}
        </fieldset>

        <div className="mt-6 flex justify-end gap-2">
          <button
            className="rounded-md border border-slate-300 px-4 py-2 text-sm font-semibold text-secondary hover:border-primary hover:text-primary disabled:opacity-50"
            disabled={isComparing}
            onClick={onClose}
            type="button"
          >
            {t('common.actions.cancel')}
          </button>
          <button
            className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90 disabled:cursor-wait disabled:opacity-60"
            disabled={isComparing}
            onClick={onCompare}
            type="button"
          >
            {isComparing
              ? t('runs.compare.comparing')
              : t('runs.compare.compareRuns', { count: selectedCount })}
          </button>
        </div>
      </div>
    </div>
  )
}
