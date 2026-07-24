import { useTranslation } from 'react-i18next'

import { useDocumentTitle } from '../../hooks'

export function Dashboard() {
  const { t } = useTranslation()
  useDocumentTitle(t('dashboard.title'))

  return (
    <section>
      <h1 className="font-heading text-2xl font-semibold text-secondary">
        {t('dashboard.title')}
      </h1>
      <p className="mt-2 max-w-2xl text-sm text-slate-600">
        {t('dashboard.description')}
      </p>
    </section>
  )
}
