import type { ExperimentType } from '../../services/experimentType.service'

function cloneDefaultValue(value: unknown): unknown {
  if (Array.isArray(value)) return value.map(cloneDefaultValue)

  if (typeof value === 'object' && value !== null) {
    return Object.fromEntries(
      Object.entries(value).map(([key, child]) => [
        key,
        cloneDefaultValue(child),
      ]),
    )
  }

  return value
}

export function buildExperimentTypeDefaultConfig(
  experimentType: ExperimentType,
): Record<string, unknown> {
  return Object.fromEntries(
    experimentType.defaultConfig.map((field) => [
      field.name,
      cloneDefaultValue(field.defaultValue),
    ]),
  )
}
