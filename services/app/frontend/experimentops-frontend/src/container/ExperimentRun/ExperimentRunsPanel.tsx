import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

export function ExperimentRunsPanel({
  children,
  onAdd,
}: {
  children: ReactNode
  onAdd: () => void
}) {
  const { t } = useTranslation()
  return (
    <div>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="font-heading text-2xl font-semibold text-secondary">
            {t('runs.title')}
          </h2>
        </div>
        <button
          className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90"
          onClick={onAdd}
          type="button"
        >
          + {t('runs.add')}
        </button>
      </div>
      <div className="mt-6">{children}</div>
    </div>
  )
}
