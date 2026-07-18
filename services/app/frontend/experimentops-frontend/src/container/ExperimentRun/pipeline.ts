import type {
  ConnectionDraft,
  DatasetBinding,
  DatasetBindingTarget,
  PipelineConnection,
  SelectedDatasetVersion,
} from '../../components/pipeline.types'
import type { ExperimentConfig } from '../../services/experimentConfig.service'
import type {
  ExperimentRunStep,
  ExperimentRunStepInput,
} from '../../services/experimentRun.service'
import type {
  ExperimentTypeInputManifest,
  ExperimentTypeManifest,
  ExperimentTypeOutputManifest,
} from '../../services/experimentType.service'

type ResolvedInput = {
  dataKind: string
  formats: string[]
}

export type PipelineValidation = {
  errors: string[]
  matchingManifests: Map<string, ExperimentTypeManifest>
  order: ExperimentConfig[]
}

const normalize = (value: string) => value.trim().toUpperCase()

function uniqueByName<T>(items: T[], getName: (item: T) => string): T[] {
  const byName = new Map<string, T>()
  items.forEach((item) => {
    const name = getName(item)
    if (!byName.has(name)) byName.set(name, item)
  })
  return [...byName.values()]
}

export function getConfigInputs(
  config: ExperimentConfig,
): ExperimentTypeInputManifest[] {
  const inputs = uniqueByName(
    config.formatMappings.flatMap((manifest) => manifest.inputs),
    (input) => input.portName,
  )

  return inputs.map((input) => {
    const variants = config.formatMappings
      .flatMap((manifest) => manifest.inputs)
      .filter((candidate) => candidate.portName === input.portName)

    return {
      ...input,
      contract: {
        ...input.contract,
        acceptedFormats: [
          ...new Set(
            variants.flatMap((variant) =>
              variant.contract.acceptedFormats.map(normalize),
            ),
          ),
        ],
      },
      required: variants.every((variant) => variant.required),
    }
  })
}

export function getConfigOutputs(
  config: ExperimentConfig,
): ExperimentTypeOutputManifest[] {
  return uniqueByName(
    config.formatMappings.flatMap((manifest) => manifest.outputs),
    (output) => output.name,
  )
}

export function getPipelineOrder(
  configs: ExperimentConfig[],
  connections: PipelineConnection[],
): ExperimentConfig[] {
  if (!configs.length) return []

  const configByUuid = new Map(configs.map((config) => [config.uuid, config]))
  const originalIndex = new Map(
    configs.map((config, index) => [config.uuid, index]),
  )
  const incomingCount = new Map(configs.map((config) => [config.uuid, 0]))
  const outgoing = new Map<string, Set<string>>()

  connections.forEach((connection) => {
    if (
      !configByUuid.has(connection.sourceConfigUuid) ||
      !configByUuid.has(connection.targetConfigUuid)
    ) {
      return
    }
    const targets = outgoing.get(connection.sourceConfigUuid) ?? new Set()
    if (!targets.has(connection.targetConfigUuid)) {
      targets.add(connection.targetConfigUuid)
      outgoing.set(connection.sourceConfigUuid, targets)
      incomingCount.set(
        connection.targetConfigUuid,
        (incomingCount.get(connection.targetConfigUuid) ?? 0) + 1,
      )
    }
  })

  const queue = configs
    .filter((config) => incomingCount.get(config.uuid) === 0)
    .map((config) => config.uuid)
  const order: ExperimentConfig[] = []

  while (queue.length) {
    queue.sort(
      (left, right) =>
        (originalIndex.get(left) ?? 0) - (originalIndex.get(right) ?? 0),
    )
    const uuid = queue.shift()
    if (!uuid) break
    const config = configByUuid.get(uuid)
    if (config) order.push(config)

    outgoing.get(uuid)?.forEach((targetUuid) => {
      const nextCount = (incomingCount.get(targetUuid) ?? 0) - 1
      incomingCount.set(targetUuid, nextCount)
      if (nextCount === 0) queue.push(targetUuid)
    })
  }

  return order.length === configs.length ? order : []
}

function findInputBinding(
  configUuid: string,
  portName: string,
  connections: PipelineConnection[],
  datasetBindings: DatasetBinding[],
) {
  return {
    connection: connections.find(
      (connection) =>
        connection.targetConfigUuid === configUuid &&
        connection.targetInputPortName === portName,
    ),
    dataset: datasetBindings.find(
      (binding) =>
        binding.targetConfigUuid === configUuid &&
        binding.targetInputPortName === portName,
    ),
  }
}

function resolveOutput(
  configUuid: string,
  outputName: string,
  configs: ExperimentConfig[],
  connections: PipelineConnection[],
  datasetBindings: DatasetBinding[],
  selectedDatasets: SelectedDatasetVersion[],
  matchingManifests?: Map<string, ExperimentTypeManifest>,
  visiting = new Set<string>(),
): ResolvedInput | null {
  const key = `${configUuid}:${outputName}`
  if (visiting.has(key)) return null
  visiting.add(key)

  const config = configs.find((candidate) => candidate.uuid === configUuid)
  if (!config) return null
  const manifest = matchingManifests?.get(configUuid)
  const output = (manifest?.outputs ?? getConfigOutputs(config)).find(
    (candidate) => candidate.name === outputName,
  )
  if (!output) return null

  if (normalize(output.type.type) === 'FIXED') {
    return {
      dataKind: normalize(output.dataKind),
      formats: output.type.format ? [normalize(output.type.format)] : [],
    }
  }

  if (!output.type.sourceInputPort) return null
  const binding = findInputBinding(
    configUuid,
    output.type.sourceInputPort,
    connections,
    datasetBindings,
  )
  if (binding.dataset) {
    const selected = selectedDatasets.find(
      (item) =>
        item.version.datasetVersionUuid === binding.dataset?.datasetVersionUuid,
    )
    return selected
      ? {
          dataKind: normalize(output.dataKind),
          formats: [normalize(selected.version.format)],
        }
      : null
  }
  if (!binding.connection) return null

  const resolved = resolveOutput(
    binding.connection.sourceConfigUuid,
    binding.connection.sourceOutputName,
    configs,
    connections,
    datasetBindings,
    selectedDatasets,
    matchingManifests,
    visiting,
  )
  return resolved
    ? { dataKind: normalize(output.dataKind), formats: resolved.formats }
    : null
}

function formatsIntersect(left: string[], right: string[]) {
  if (!left.length || !right.length) return true
  const normalizedRight = new Set(right.map(normalize))
  return left.some((format) => normalizedRight.has(normalize(format)))
}

export function getConnectionError(
  draft: ConnectionDraft,
  configs: ExperimentConfig[],
  connections: PipelineConnection[],
  datasetBindings: DatasetBinding[],
  selectedDatasets: SelectedDatasetVersion[],
): string | null {
  if (draft.sourceConfigUuid === draft.targetConfigUuid) {
    return 'A config cannot connect to itself.'
  }

  const source = configs.find(
    (config) => config.uuid === draft.sourceConfigUuid,
  )
  const target = configs.find(
    (config) => config.uuid === draft.targetConfigUuid,
  )
  if (!source || !target) return 'Both configs must be on the board.'

  const output = getConfigOutputs(source).find(
    (candidate) => candidate.name === draft.sourceOutputName,
  )
  const input = getConfigInputs(target).find(
    (candidate) => candidate.portName === draft.targetInputPortName,
  )
  if (!output || !input) return 'The selected port is not available.'
  if (normalize(output.downStreamPolicy) !== 'CONNECTABLE') {
    return `${output.name} is a terminal output.`
  }

  const existing = findInputBinding(
    draft.targetConfigUuid,
    draft.targetInputPortName,
    connections,
    datasetBindings,
  )
  if (existing.connection || existing.dataset) {
    return `${input.portName} already has a binding.`
  }

  const duplicate = connections.some(
    (connection) =>
      connection.sourceConfigUuid === draft.sourceConfigUuid &&
      connection.sourceOutputName === draft.sourceOutputName &&
      connection.targetConfigUuid === draft.targetConfigUuid &&
      connection.targetInputPortName === draft.targetInputPortName,
  )
  if (duplicate) return 'This connection already exists.'

  if (normalize(output.dataKind) !== normalize(input.contract.dataKind)) {
    return `${output.name} produces ${output.dataKind}, but ${input.portName} expects ${input.contract.dataKind}.`
  }

  const resolvedOutput = resolveOutput(
    source.uuid,
    output.name,
    configs,
    connections,
    datasetBindings,
    selectedDatasets,
  )
  if (
    resolvedOutput &&
    !formatsIntersect(resolvedOutput.formats, input.contract.acceptedFormats)
  ) {
    return `${output.name} has an incompatible format for ${input.portName}.`
  }

  if (!getPipelineOrder(configs, [...connections, draft]).length) {
    return 'This connection would create a cycle.'
  }

  return null
}

export function getDatasetBindingError(
  target: DatasetBindingTarget,
  dataset: SelectedDatasetVersion,
  configs: ExperimentConfig[],
  connections: PipelineConnection[],
  datasetBindings: DatasetBinding[],
): string | null {
  const config = configs.find(
    (candidate) => candidate.uuid === target.targetConfigUuid,
  )
  const input = config
    ? getConfigInputs(config).find(
        (candidate) => candidate.portName === target.targetInputPortName,
      )
    : null
  if (!config || !input) return 'The selected input port is not available.'

  const existing = findInputBinding(
    target.targetConfigUuid,
    target.targetInputPortName,
    connections,
    datasetBindings,
  )
  if (existing.connection || existing.dataset) {
    return `${input.portName} already has a binding.`
  }
  if (normalize(input.contract.dataKind) !== 'TABULAR_DATASET') {
    return `${input.portName} does not accept datasets.`
  }
  if (
    !input.contract.acceptedFormats
      .map(normalize)
      .includes(normalize(dataset.version.format))
  ) {
    return `${input.portName} does not accept ${dataset.version.format}.`
  }

  const order = getPipelineOrder(configs, connections)
  if (order.length && order[0]?.uuid !== config.uuid) {
    return 'The backend currently allows datasets only on the first pipeline step.'
  }

  return null
}

function resolveBoundInput(
  configUuid: string,
  input: ExperimentTypeInputManifest,
  configs: ExperimentConfig[],
  connections: PipelineConnection[],
  datasetBindings: DatasetBinding[],
  selectedDatasets: SelectedDatasetVersion[],
  matchingManifests?: Map<string, ExperimentTypeManifest>,
): ResolvedInput | null {
  const binding = findInputBinding(
    configUuid,
    input.portName,
    connections,
    datasetBindings,
  )
  if (binding.dataset) {
    const selected = selectedDatasets.find(
      (item) =>
        item.version.datasetVersionUuid === binding.dataset?.datasetVersionUuid,
    )
    return selected
      ? {
          dataKind: 'TABULAR_DATASET',
          formats: [normalize(selected.version.format)],
        }
      : null
  }
  if (!binding.connection) return null
  return resolveOutput(
    binding.connection.sourceConfigUuid,
    binding.connection.sourceOutputName,
    configs,
    connections,
    datasetBindings,
    selectedDatasets,
    matchingManifests,
  )
}

function manifestMatches(
  config: ExperimentConfig,
  manifest: ExperimentTypeManifest,
  configs: ExperimentConfig[],
  connections: PipelineConnection[],
  datasetBindings: DatasetBinding[],
  selectedDatasets: SelectedDatasetVersion[],
) {
  const suppliedPorts = new Set([
    ...connections
      .filter((item) => item.targetConfigUuid === config.uuid)
      .map((item) => item.targetInputPortName),
    ...datasetBindings
      .filter((item) => item.targetConfigUuid === config.uuid)
      .map((item) => item.targetInputPortName),
  ])
  if (suppliedPorts.size === 0) return false
  if (
    [...suppliedPorts].some(
      (portName) =>
        !manifest.inputs.some((input) => input.portName === portName),
    )
  ) {
    return false
  }
  if (
    manifest.inputs.some(
      (input) => input.required && !suppliedPorts.has(input.portName),
    )
  ) {
    return false
  }

  const resolved = new Map<string, ResolvedInput>()
  for (const input of manifest.inputs) {
    if (!suppliedPorts.has(input.portName)) continue
    const value = resolveBoundInput(
      config.uuid,
      input,
      configs,
      connections,
      datasetBindings,
      selectedDatasets,
    )
    if (!value) return false
    if (normalize(value.dataKind) !== normalize(input.contract.dataKind)) {
      return false
    }
    if (!formatsIntersect(value.formats, input.contract.acceptedFormats)) {
      return false
    }
    resolved.set(input.portName, value)
  }

  return manifest.inputRelationships.every((relationship) => {
    if (normalize(relationship.type) !== 'SAME_FORMAT') return true
    const formats = relationship.ports
      .map((portName) => resolved.get(portName)?.formats[0])
      .filter((format): format is string => Boolean(format))
    return (
      formats.length < 2 || formats.every((format) => format === formats[0])
    )
  })
}

export function validatePipeline(
  configs: ExperimentConfig[],
  connections: PipelineConnection[],
  datasetBindings: DatasetBinding[],
  selectedDatasets: SelectedDatasetVersion[],
): PipelineValidation {
  const errors: string[] = []
  const order = getPipelineOrder(configs, connections)
  const matchingManifests = new Map<string, ExperimentTypeManifest>()

  if (!configs.length) errors.push('Add at least one config to the board.')
  if (configs.length && !order.length) {
    errors.push('The pipeline contains a cycle.')
  }

  const firstConfigUuid = order[0]?.uuid
  datasetBindings.forEach((binding) => {
    if (binding.targetConfigUuid !== firstConfigUuid) {
      errors.push('Datasets can currently be bound only to the first step.')
    }
  })

  configs.forEach((config) => {
    const matches = config.formatMappings.filter((manifest) =>
      manifestMatches(
        config,
        manifest,
        configs,
        connections,
        datasetBindings,
        selectedDatasets,
      ),
    )
    if (matches.length === 1) {
      matchingManifests.set(config.uuid, matches[0])
    } else if (matches.length === 0) {
      errors.push(`${config.name} has missing or incompatible input bindings.`)
    } else {
      errors.push(`${config.name} matches more than one manifest.`)
    }
  })

  connections.forEach((connection) => {
    const sourceManifest = matchingManifests.get(connection.sourceConfigUuid)
    const output = sourceManifest?.outputs.find(
      (candidate) => candidate.name === connection.sourceOutputName,
    )
    if (!output || normalize(output.downStreamPolicy) !== 'CONNECTABLE') {
      errors.push(
        `${connection.sourceOutputName} is not a connectable output for the resolved manifest.`,
      )
    }
  })

  return {
    errors: [...new Set(errors)],
    matchingManifests,
    order,
  }
}

export function buildExecutionMode(
  order: ExperimentConfig[],
  connections: PipelineConnection[],
  datasetBindings: DatasetBinding[],
): ExperimentRunStep[] {
  const stepByConfigUuid = new Map(
    order.map((config, index) => [config.uuid, index + 1]),
  )

  return order.map((config, index) => {
    const inputs: ExperimentRunStepInput[] = [
      ...datasetBindings
        .filter((binding) => binding.targetConfigUuid === config.uuid)
        .map((binding) => ({
          file: binding.datasetVersionUuid,
          inputType: 'DATASET' as const,
          portName: binding.targetInputPortName,
        })),
      ...connections
        .filter((connection) => connection.targetConfigUuid === config.uuid)
        .map((connection) => ({
          file: connection.sourceOutputName,
          inputType: 'ARTIFACT' as const,
          portName: connection.targetInputPortName,
          sourceStepCount: stepByConfigUuid.get(connection.sourceConfigUuid),
        })),
    ]

    const inputOrder = new Map(
      getConfigInputs(config).map((input, inputIndex) => [
        input.portName,
        inputIndex,
      ]),
    )
    inputs.sort(
      (left, right) =>
        (inputOrder.get(left.portName) ?? 0) -
        (inputOrder.get(right.portName) ?? 0),
    )

    return {
      experimentConfigUuid: config.uuid,
      inputs,
      stepCount: index + 1,
    }
  })
}
