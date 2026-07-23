import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

import de from './locales/de.json'
import en from './locales/en.json'

export const SUPPORTED_LANGUAGES = ['en', 'de'] as const
export type SupportedLanguage = (typeof SUPPORTED_LANGUAGES)[number]

export const LANGUAGE_STORAGE_KEY = 'experimentops_language'
export const DEFAULT_LANGUAGE: SupportedLanguage = 'en'

function isSupportedLanguage(value: string | null): value is SupportedLanguage {
  return SUPPORTED_LANGUAGES.includes(value as SupportedLanguage)
}

function getInitialLanguage(): SupportedLanguage {
  const storedLanguage = localStorage.getItem(LANGUAGE_STORAGE_KEY)
  if (isSupportedLanguage(storedLanguage)) {
    return storedLanguage
  }

  const browserLanguage = navigator.language.split('-')[0]?.toLowerCase() ?? ''
  return isSupportedLanguage(browserLanguage)
    ? browserLanguage
    : DEFAULT_LANGUAGE
}

export function applyDocumentLanguage(language: SupportedLanguage) {
  document.documentElement.lang = language
  document.documentElement.dir = 'ltr'
}

const initialLanguage = getInitialLanguage()

void i18n.use(initReactI18next).init({
  fallbackLng: DEFAULT_LANGUAGE,
  initAsync: false,
  interpolation: {
    escapeValue: false,
  },
  lng: initialLanguage,
  resources: {
    de: { translation: de },
    en: { translation: en },
  },
  returnEmptyString: false,
  supportedLngs: SUPPORTED_LANGUAGES,
})

applyDocumentLanguage(initialLanguage)

i18n.on('languageChanged', (language) => {
  const supportedLanguage = isSupportedLanguage(language)
    ? language
    : DEFAULT_LANGUAGE
  localStorage.setItem(LANGUAGE_STORAGE_KEY, supportedLanguage)
  applyDocumentLanguage(supportedLanguage)
})

export default i18n
