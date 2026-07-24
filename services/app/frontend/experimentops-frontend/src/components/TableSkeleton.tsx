import { useTranslation } from 'react-i18next'

export function TableSkeleton() {
  const { t } = useTranslation()

  return (
    <div className="space-y-2" role="status">
      <div className="h-12 animate-pulse rounded bg-slate-100" />
      <div className="h-10 animate-pulse rounded bg-slate-100" />
      <div className="h-10 animate-pulse rounded bg-slate-100" />
      <span className="sr-only">{t('common.table.loadingRows')}</span>
    </div>
  )
}
