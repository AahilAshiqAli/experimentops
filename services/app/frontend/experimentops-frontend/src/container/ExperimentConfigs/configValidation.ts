import type { ExperimentType } from '../../services/experimentType.service'
import i18n from '../../i18n'

export function getExperimentConfigValidationErrors(
  config: unknown,
  experimentType: ExperimentType | null,
) {
  if (!experimentType) {
    return [i18n.t('configs.errors.rulesUnavailable')]
  }

  if (typeof config !== 'object' || config === null || Array.isArray(config)) {
    return [i18n.t('configs.errors.mustBeObject')]
  }

  const configObject = config as Record<string, unknown>

  return experimentType.defaultConfig.flatMap((field) => {
    if (field.datatype !== 'string' || !field.regex) return []

    const value = configObject[field.name]

    if (typeof value !== 'string') {
      return [i18n.t('configs.errors.mustBeString', { field: field.name })]
    }

    try {
      const regex = new RegExp(field.regex)

      if (!regex.test(value)) {
        return [
          i18n.t('configs.errors.mustMatchRegex', {
            field: field.name,
            regex: field.regex,
          }),
        ]
      }
    } catch {
      return [
        i18n.t('configs.errors.invalidRegex', {
          field: field.name,
          regex: field.regex,
        }),
      ]
    }

    return []
  })
}
