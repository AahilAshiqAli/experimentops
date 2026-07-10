import type { ExperimentType } from '../../services/experimentType.service'

export function getExperimentConfigValidationErrors(
  config: unknown,
  experimentType: ExperimentType | null,
) {
  if (!experimentType) {
    return [
      'Unable to validate config because experiment type rules are not available.',
    ]
  }

  if (typeof config !== 'object' || config === null || Array.isArray(config)) {
    return ['Config must be a JSON object.']
  }

  const configObject = config as Record<string, unknown>

  return experimentType.defaultConfig.flatMap((field) => {
    if (field.datatype !== 'string' || !field.regex) return []

    const value = configObject[field.name]

    if (typeof value !== 'string') {
      return [`${field.name} must be a string.`]
    }

    try {
      const regex = new RegExp(field.regex)

      if (!regex.test(value)) {
        return [`${field.name} must match regex ${field.regex}.`]
      }
    } catch {
      return [`${field.name} has an invalid validation regex: ${field.regex}.`]
    }

    return []
  })
}
