import { useTranslation } from 'react-i18next'

import { SUPPORTED_LANGUAGES, type SupportedLanguage } from '../i18n'

type LanguageSelectorProps = {
  className?: string
}

export function LanguageSelector({ className = '' }: LanguageSelectorProps) {
  const { i18n, t } = useTranslation()
  const activeLanguage = i18n.resolvedLanguage?.split('-')[0] ?? 'en'

  return (
    <label className={`inline-flex items-center gap-2 text-sm ${className}`}>
      <span className="sr-only">{t('common.language.label')}</span>
      <select
        aria-label={t('common.language.label')}
        className="rounded-md border border-slate-300 bg-white px-2.5 py-2 text-sm font-medium text-slate-600 outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
        onChange={(event) =>
          void i18n.changeLanguage(event.target.value as SupportedLanguage)
        }
        value={activeLanguage}
      >
        {SUPPORTED_LANGUAGES.map((language) => (
          <option key={language} value={language}>
            {t(`common.language.${language}`)}
          </option>
        ))}
      </select>
    </label>
  )
}
