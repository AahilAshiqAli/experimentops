import {
  useCallback,
  useLayoutEffect,
  useRef,
  useState,
  type PointerEvent,
} from 'react'

import type { ExperimentConfig } from '../services/experimentConfig.service'
import type {
  ExperimentTypeInputManifest,
  ExperimentTypeOutputManifest,
} from '../services/experimentType.service'
import {
  getConfigInputs,
  getConfigOutputs,
} from '../container/ExperimentRun/pipeline'
import { DATASET_DRAG_TYPE } from './datasetDrag'
import type {
  ConnectionDraft,
  DatasetBinding,
  DatasetBindingTarget,
  PipelineConnection,
  SelectedDatasetVersion,
} from './pipeline.types'

export type { DatasetBinding, PipelineConnection } from './pipeline.types'

type BoardPoint = {
  x: number
  y: number
}

type DrawingConnection = {
  pointer: BoardPoint
  sourceConfigUuid: string
  sourceOutputName: string
}

type DraggingCard = {
  offset: BoardPoint
  uuid: string
}

const BOARD_NODE_WIDTH = 300
const BOARD_NODE_VERTICAL_GAP = 25

const inputAnchorKey = (configUuid: string, portName: string) =>
  `input:${configUuid}:${portName}`
const outputAnchorKey = (configUuid: string, outputName: string) =>
  `output:${configUuid}:${outputName}`

function PortTooltip({
  binding,
  port,
  side,
}: {
  binding?: Record<string, unknown>
  port: ExperimentTypeInputManifest | ExperimentTypeOutputManifest
  side: 'input' | 'output'
}) {
  return (
    <div
      className={`pointer-events-none absolute top-1/2 z-50 hidden w-80 -translate-y-1/2 rounded-lg border border-slate-700 bg-slate-900 p-3 text-left text-[11px] text-slate-100 shadow-xl group-hover:block group-focus-within:block ${
        side === 'input' ? 'left-8' : 'right-8'
      }`}
      role="tooltip"
    >
      <pre className="max-h-72 overflow-auto whitespace-pre-wrap break-words font-mono">
        {JSON.stringify(binding ? { ...port, binding } : port, null, 2)}
      </pre>
    </div>
  )
}

function inputBindingDetails(
  configUuid: string,
  portName: string,
  connections: PipelineConnection[],
  datasetBindings: DatasetBinding[],
  selectedDatasets: SelectedDatasetVersion[],
) {
  const datasetBinding = datasetBindings.find(
    (binding) =>
      binding.targetConfigUuid === configUuid &&
      binding.targetInputPortName === portName,
  )
  if (datasetBinding) {
    const selected = selectedDatasets.find(
      (item) =>
        item.version.datasetVersionUuid === datasetBinding.datasetVersionUuid,
    )
    return {
      binding: {
        datasetUuid: datasetBinding.datasetUuid,
        datasetVersionUuid: datasetBinding.datasetVersionUuid,
        fileName: selected?.version.originalFileName,
        inputType: 'DATASET',
      },
      datasetBinding,
      label: selected?.version.originalFileName ?? 'Dataset version',
    }
  }

  const connection = connections.find(
    (item) =>
      item.targetConfigUuid === configUuid &&
      item.targetInputPortName === portName,
  )
  if (connection) {
    return {
      binding: {
        inputType: 'ARTIFACT',
        sourceConfigUuid: connection.sourceConfigUuid,
        sourceOutputName: connection.sourceOutputName,
      },
      connection,
      label: connection.sourceOutputName,
    }
  }

  return null
}

export function PipelineBoard({
  configs,
  connections,
  datasetBindings,
  getConnectionError,
  getDatasetBindingError,
  onBindDataset,
  onConnect,
  onRemoveConnection,
  onRemoveDatasetBinding,
  onRemoveFromBoard,
  pipelineOrder,
  selectedDatasets,
}: {
  configs: ExperimentConfig[]
  connections: PipelineConnection[]
  datasetBindings: DatasetBinding[]
  getConnectionError: (draft: ConnectionDraft) => string | null
  getDatasetBindingError: (
    target: DatasetBindingTarget,
    dataset: SelectedDatasetVersion,
  ) => string | null
  onBindDataset: (
    target: DatasetBindingTarget,
    datasetVersionUuid: string,
  ) => void
  onConnect: (draft: ConnectionDraft) => void
  onRemoveConnection: (connection: PipelineConnection) => void
  onRemoveDatasetBinding: (binding: DatasetBinding) => void
  onRemoveFromBoard: (configUuid: string) => void
  pipelineOrder: ExperimentConfig[]
  selectedDatasets: SelectedDatasetVersion[]
}) {
  const boardRef = useRef<HTMLDivElement | null>(null)
  const cardRefs = useRef(new Map<string, HTMLDivElement>())
  const anchorRefs = useRef(new Map<string, HTMLElement>())
  const [anchorPoints, setAnchorPoints] = useState<Record<string, BoardPoint>>(
    {},
  )
  const [boardWidth, setBoardWidth] = useState(0)
  const [drawingConnection, setDrawingConnection] =
    useState<DrawingConnection | null>(null)
  const [draggingCard, setDraggingCard] = useState<DraggingCard | null>(null)
  const [nodePositions, setNodePositions] = useState<
    Record<string, BoardPoint>
  >({})
  const [openDatasetTarget, setOpenDatasetTarget] =
    useState<DatasetBindingTarget | null>(null)
  const [datasetDragTarget, setDatasetDragTarget] = useState<string | null>(
    null,
  )

  const nodeHeight = (config: ExperimentConfig) =>
    104 +
    Math.max(getConfigInputs(config).length, getConfigOutputs(config).length) *
      36
  const boardHeight = Math.max(
    640,
    configs.reduce(
      (height, config) => height + nodeHeight(config) + BOARD_NODE_VERTICAL_GAP,
      80,
    ),
  )
  const getDefaultY = (index: number) =>
    configs
      .slice(0, index)
      .reduce(
        (top, config) => top + nodeHeight(config) + BOARD_NODE_VERTICAL_GAP,
        40,
      )
  const getDefaultX = () => {
    if (!boardWidth) return 20
    const cardWidth = Math.min(BOARD_NODE_WIDTH, boardWidth - 48)
    return ((boardWidth - cardWidth) / 2 / boardWidth) * 100
  }
  const getNodePosition = (config: ExperimentConfig, index: number) =>
    nodePositions[config.uuid] ?? {
      x: getDefaultX(),
      y: (getDefaultY(index) / boardHeight) * 100,
    }
  const orderByUuid = new Map(
    pipelineOrder.map((config, index) => [config.uuid, index + 1]),
  )

  const measureAnchors = useCallback(() => {
    const boardRect = boardRef.current?.getBoundingClientRect()
    if (!boardRect) return
    setBoardWidth((current) =>
      current === boardRect.width ? current : boardRect.width,
    )
    const nextPoints: Record<string, BoardPoint> = {}
    anchorRefs.current.forEach((element, key) => {
      const rect = element.getBoundingClientRect()
      nextPoints[key] = {
        x: rect.left - boardRect.left + rect.width / 2,
        y: rect.top - boardRect.top + rect.height / 2,
      }
    })
    setAnchorPoints(nextPoints)
  }, [])

  useLayoutEffect(() => {
    measureAnchors()
    const observer = new ResizeObserver(measureAnchors)
    if (boardRef.current) observer.observe(boardRef.current)
    cardRefs.current.forEach((card) => observer.observe(card))
    return () => observer.disconnect()
  }, [boardHeight, configs, datasetBindings, measureAnchors, nodePositions])

  const registerAnchor = (key: string, element: HTMLElement | null) => {
    if (element) anchorRefs.current.set(key, element)
    else anchorRefs.current.delete(key)
  }
  const registerCard = (uuid: string, element: HTMLDivElement | null) => {
    if (element) cardRefs.current.set(uuid, element)
    else cardRefs.current.delete(uuid)
  }
  const toBoardPoint = (clientX: number, clientY: number): BoardPoint => {
    const rect = boardRef.current?.getBoundingClientRect()
    if (!rect) return { x: 0, y: 0 }
    return { x: clientX - rect.left, y: clientY - rect.top }
  }
  const toBoardPercent = (clientX: number, clientY: number): BoardPoint => {
    const rect = boardRef.current?.getBoundingClientRect()
    if (!rect) return { x: 0, y: 0 }
    return {
      x: ((clientX - rect.left) / rect.width) * 100,
      y: ((clientY - rect.top) / rect.height) * 100,
    }
  }
  const clampPosition = (uuid: string, point: BoardPoint): BoardPoint => {
    const boardRect = boardRef.current?.getBoundingClientRect()
    const cardRect = cardRefs.current.get(uuid)?.getBoundingClientRect()
    const widthPercent = boardRect
      ? ((cardRect?.width ?? BOARD_NODE_WIDTH) / boardRect.width) * 100
      : 40
    const heightPercent = boardRect
      ? ((cardRect?.height ?? 160) / boardRect.height) * 100
      : 25
    return {
      x: Math.min(99 - widthPercent, Math.max(1, point.x)),
      y: Math.min(99 - heightPercent, Math.max(1, point.y)),
    }
  }
  const endDrawing = (event: PointerEvent<HTMLDivElement>) => {
    if (!drawingConnection) return
    const target = (event.target as HTMLElement).closest<HTMLElement>(
      '[data-input-config-uuid][data-input-port-name]',
    )
    const targetConfigUuid = target?.dataset.inputConfigUuid
    const targetInputPortName = target?.dataset.inputPortName
    if (targetConfigUuid && targetInputPortName) {
      onConnect({
        sourceConfigUuid: drawingConnection.sourceConfigUuid,
        sourceOutputName: drawingConnection.sourceOutputName,
        targetConfigUuid,
        targetInputPortName,
      })
    }
    setDrawingConnection(null)
  }

  return (
    <div
      className="relative mt-6 flex-1 overflow-hidden rounded-xl border border-dashed border-slate-300 bg-slate-50 bg-[radial-gradient(circle_at_1px_1px,rgba(148,163,184,0.35)_1px,transparent_0)] [background-size:24px_24px]"
      onPointerCancel={() => {
        setDrawingConnection(null)
        setDraggingCard(null)
      }}
      onPointerMove={(event) => {
        if (drawingConnection) {
          setDrawingConnection({
            ...drawingConnection,
            pointer: toBoardPoint(event.clientX, event.clientY),
          })
          return
        }
        if (!draggingCard) return
        const point = toBoardPercent(event.clientX, event.clientY)
        setNodePositions((current) => ({
          ...current,
          [draggingCard.uuid]: clampPosition(draggingCard.uuid, {
            x: point.x - draggingCard.offset.x,
            y: point.y - draggingCard.offset.y,
          }),
        }))
      }}
      onPointerUp={(event) => {
        endDrawing(event)
        setDraggingCard(null)
      }}
      ref={boardRef}
      style={{ minHeight: boardHeight }}
    >
      {configs.length ? (
        <>
          <svg
            aria-hidden="true"
            className="pointer-events-none absolute inset-0 h-full w-full"
            height={boardHeight}
            preserveAspectRatio="none"
            width="100%"
          >
            <defs>
              <marker
                id="pipeline-arrow"
                markerHeight="8"
                markerWidth="8"
                orient="auto"
                refX="7"
                refY="4"
                viewBox="0 0 8 8"
              >
                <path d="M0,0 L8,4 L0,8 Z" fill="#2563EB" />
              </marker>
            </defs>
            {connections.map((connection) => {
              const from =
                anchorPoints[
                  outputAnchorKey(
                    connection.sourceConfigUuid,
                    connection.sourceOutputName,
                  )
                ]
              const to =
                anchorPoints[
                  inputAnchorKey(
                    connection.targetConfigUuid,
                    connection.targetInputPortName,
                  )
                ]
              if (!from || !to) return null
              const distance = Math.max(60, Math.abs(to.x - from.x) / 2)
              const path = `M ${from.x} ${from.y} C ${from.x + distance} ${from.y}, ${to.x - distance} ${to.y}, ${to.x} ${to.y}`
              return (
                <g
                  key={`${connection.sourceConfigUuid}-${connection.sourceOutputName}-${connection.targetConfigUuid}-${connection.targetInputPortName}`}
                >
                  <path
                    cursor="pointer"
                    d={path}
                    fill="none"
                    onDoubleClick={(event) => {
                      event.stopPropagation()
                      onRemoveConnection(connection)
                    }}
                    pointerEvents="stroke"
                    stroke="transparent"
                    strokeLinecap="round"
                    strokeWidth="16"
                  >
                    <title>Double-click to remove connection</title>
                  </path>
                  <path
                    d={path}
                    fill="none"
                    markerEnd="url(#pipeline-arrow)"
                    stroke="#2563EB"
                    strokeLinecap="round"
                    strokeWidth="3"
                  />
                </g>
              )
            })}
            {drawingConnection
              ? (() => {
                  const from =
                    anchorPoints[
                      outputAnchorKey(
                        drawingConnection.sourceConfigUuid,
                        drawingConnection.sourceOutputName,
                      )
                    ]
                  if (!from) return null
                  return (
                    <path
                      d={`M ${from.x} ${from.y} L ${drawingConnection.pointer.x} ${drawingConnection.pointer.y}`}
                      fill="none"
                      markerEnd="url(#pipeline-arrow)"
                      stroke="#0EA5E9"
                      strokeDasharray="8 6"
                      strokeLinecap="round"
                      strokeWidth="3"
                    />
                  )
                })()
              : null}
          </svg>

          {configs.map((config, index) => {
            const inputs = getConfigInputs(config)
            const outputs = getConfigOutputs(config)
            const stepNumber = orderByUuid.get(config.uuid)
            const position = getNodePosition(config, index)
            return (
              <div
                className="absolute select-none rounded-lg border border-slate-200 bg-white shadow-sm transition hover:shadow-card"
                key={config.uuid}
                onPointerDown={(event) => {
                  if (
                    (event.target as HTMLElement).closest(
                      'button,[data-no-card-drag]',
                    )
                  ) {
                    return
                  }
                  const point = toBoardPercent(event.clientX, event.clientY)
                  setOpenDatasetTarget(null)
                  setDraggingCard({
                    offset: {
                      x: point.x - position.x,
                      y: point.y - position.y,
                    },
                    uuid: config.uuid,
                  })
                }}
                ref={(element) => registerCard(config.uuid, element)}
                style={{
                  cursor:
                    draggingCard?.uuid === config.uuid ? 'grabbing' : 'grab',
                  left: `${position.x}%`,
                  top: `${position.y}%`,
                  width: `min(calc(100% - 3rem), ${BOARD_NODE_WIDTH}px)`,
                }}
              >
                <div className="flex items-start justify-between gap-2 border-b border-slate-100 p-3">
                  <div className="min-w-0">
                    <p
                      className="truncate text-sm font-semibold text-secondary"
                      title={config.name}
                    >
                      {config.name}
                    </p>
                    <p className="mt-1 truncate text-xs text-slate-500">
                      {config.experimentType}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    {stepNumber ? (
                      <span className="rounded-full bg-primary px-2 py-1 text-xs font-bold text-white">
                        {stepNumber}
                      </span>
                    ) : null}
                    <button
                      aria-label={`Remove ${config.name} from board`}
                      className="rounded-full px-2 py-1 text-xs font-bold leading-none text-red-600 hover:bg-red-50"
                      onClick={() => onRemoveFromBoard(config.uuid)}
                      type="button"
                    >
                      ×
                    </button>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4 p-3">
                  <div>
                    <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Inputs
                    </p>
                    <div className="space-y-1.5">
                      {inputs.map((input) => {
                        const target = {
                          targetConfigUuid: config.uuid,
                          targetInputPortName: input.portName,
                        }
                        const targetKey = inputAnchorKey(
                          config.uuid,
                          input.portName,
                        )
                        const bindingDetails = inputBindingDetails(
                          config.uuid,
                          input.portName,
                          connections,
                          datasetBindings,
                          selectedDatasets,
                        )
                        const draft = drawingConnection
                          ? {
                              sourceConfigUuid:
                                drawingConnection.sourceConfigUuid,
                              sourceOutputName:
                                drawingConnection.sourceOutputName,
                              ...target,
                            }
                          : null
                        const connectionError = draft
                          ? getConnectionError(draft)
                          : null
                        const isDragTarget = datasetDragTarget === targetKey
                        const isPickerOpen =
                          openDatasetTarget?.targetConfigUuid === config.uuid &&
                          openDatasetTarget.targetInputPortName ===
                            input.portName
                        return (
                          <div
                            className="relative flex min-h-7 items-center gap-1.5"
                            key={input.portName}
                          >
                            <div className="group relative -ml-6 shrink-0">
                              <button
                                aria-label={`${config.name} input ${input.portName}`}
                                className={`flex h-7 w-7 items-center justify-center rounded border-2 text-[10px] font-black shadow-sm transition ${
                                  bindingDetails
                                    ? 'border-emerald-600 bg-emerald-600 text-white'
                                    : draft
                                      ? connectionError
                                        ? 'border-red-300 bg-red-50 text-red-500'
                                        : 'scale-110 border-emerald-500 bg-emerald-50 text-emerald-700'
                                      : isDragTarget
                                        ? 'scale-110 border-sky-500 bg-sky-50 text-sky-700'
                                        : 'border-slate-300 bg-white text-slate-500 hover:border-primary hover:text-primary'
                                }`}
                                data-input-config-uuid={config.uuid}
                                data-input-port-name={input.portName}
                                data-no-card-drag
                                onClick={() => {
                                  if (bindingDetails) return
                                  setOpenDatasetTarget((current) =>
                                    current?.targetConfigUuid === config.uuid &&
                                    current.targetInputPortName ===
                                      input.portName
                                      ? null
                                      : target,
                                  )
                                }}
                                onDragEnter={() =>
                                  setDatasetDragTarget(targetKey)
                                }
                                onDragLeave={() => setDatasetDragTarget(null)}
                                onDragOver={(event) => {
                                  if (
                                    event.dataTransfer.types.includes(
                                      DATASET_DRAG_TYPE,
                                    )
                                  ) {
                                    event.preventDefault()
                                  }
                                }}
                                onDrop={(event) => {
                                  event.preventDefault()
                                  setDatasetDragTarget(null)
                                  onBindDataset(
                                    target,
                                    event.dataTransfer.getData(
                                      DATASET_DRAG_TYPE,
                                    ),
                                  )
                                }}
                                ref={(element) =>
                                  registerAnchor(targetKey, element)
                                }
                                type="button"
                              >
                                {bindingDetails?.datasetBinding ? 'D' : 'I'}
                              </button>
                              <PortTooltip
                                binding={bindingDetails?.binding}
                                port={input}
                                side="input"
                              />
                            </div>
                            <div className="min-w-0 flex-1">
                              <p
                                className="truncate text-xs font-semibold text-secondary"
                                title={input.portName}
                              >
                                {input.portName}
                                {input.required ? (
                                  <span className="ml-0.5 text-red-500">*</span>
                                ) : null}
                              </p>
                              <p
                                className="truncate text-[10px] text-slate-400"
                                title={bindingDetails?.label}
                              >
                                {bindingDetails?.label ??
                                  input.contract.acceptedFormats.join(', ')}
                              </p>
                            </div>
                            {bindingDetails?.datasetBinding ? (
                              <button
                                aria-label={`Remove dataset from ${input.portName}`}
                                className="text-xs font-bold text-red-500 hover:text-red-700"
                                onClick={() =>
                                  onRemoveDatasetBinding(
                                    bindingDetails.datasetBinding,
                                  )
                                }
                                type="button"
                              >
                                ×
                              </button>
                            ) : null}

                            {isPickerOpen ? (
                              <div
                                className="absolute left-0 top-9 z-40 w-80 rounded-lg border border-slate-200 bg-white p-3 shadow-xl"
                                data-no-card-drag
                              >
                                <div className="flex items-center justify-between gap-2">
                                  <p className="text-xs font-semibold text-secondary">
                                    Bind a selected dataset
                                  </p>
                                  <button
                                    className="px-1 text-slate-400 hover:text-slate-700"
                                    onClick={() => setOpenDatasetTarget(null)}
                                    type="button"
                                  >
                                    ×
                                  </button>
                                </div>
                                {selectedDatasets.length ? (
                                  <div className="mt-2 max-h-48 space-y-1 overflow-auto">
                                    {selectedDatasets.map((dataset) => {
                                      const error = getDatasetBindingError(
                                        target,
                                        dataset,
                                      )
                                      return (
                                        <button
                                          className="block w-full rounded-md px-2 py-2 text-left hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-45"
                                          disabled={Boolean(error)}
                                          key={
                                            dataset.version.datasetVersionUuid
                                          }
                                          onClick={() => {
                                            onBindDataset(
                                              target,
                                              dataset.version
                                                .datasetVersionUuid,
                                            )
                                            setOpenDatasetTarget(null)
                                          }}
                                          title={error ?? undefined}
                                          type="button"
                                        >
                                          <span className="block truncate text-xs font-semibold text-secondary">
                                            {dataset.version.originalFileName}
                                          </span>
                                          <span className="text-[10px] text-slate-500">
                                            {dataset.dataset.name} ·{' '}
                                            {dataset.version.format}
                                          </span>
                                        </button>
                                      )
                                    })}
                                  </div>
                                ) : (
                                  <p className="mt-2 text-xs text-slate-500">
                                    Select dataset versions from the sidebar
                                    first.
                                  </p>
                                )}
                              </div>
                            ) : null}
                          </div>
                        )
                      })}
                    </div>
                  </div>

                  <div>
                    <p className="mb-2 text-right text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Outputs
                    </p>
                    <div className="space-y-1.5">
                      {outputs.map((output) => {
                        const isConnectable =
                          output.downStreamPolicy.toUpperCase() ===
                          'CONNECTABLE'
                        const anchorKey = outputAnchorKey(
                          config.uuid,
                          output.name,
                        )
                        return (
                          <div
                            className="flex min-h-7 items-center justify-end gap-1.5"
                            key={output.name}
                          >
                            <div className="min-w-0 flex-1 text-right">
                              <p
                                className="truncate text-xs font-semibold text-secondary"
                                title={output.name}
                              >
                                {output.name}
                                {output.required ? (
                                  <span className="ml-0.5 text-red-500">*</span>
                                ) : null}
                              </p>
                              <p className="truncate text-[10px] text-slate-400">
                                {isConnectable ? output.dataKind : 'Terminal'}
                              </p>
                            </div>
                            <div className="group relative -mr-6 shrink-0">
                              <button
                                aria-label={`${config.name} output ${output.name}${isConnectable ? '' : ', terminal'}`}
                                className={`flex h-7 w-7 items-center justify-center rounded border-2 text-[10px] font-black shadow-sm transition ${
                                  isConnectable
                                    ? 'border-primary bg-primary text-white hover:scale-110 hover:bg-primary/90'
                                    : 'cursor-not-allowed border-slate-200 bg-slate-100 text-slate-400'
                                }`}
                                data-no-card-drag
                                disabled={!isConnectable}
                                onPointerDown={(event) => {
                                  if (!isConnectable) return
                                  event.stopPropagation()
                                  setOpenDatasetTarget(null)
                                  setDrawingConnection({
                                    pointer:
                                      anchorPoints[anchorKey] ??
                                      toBoardPoint(
                                        event.clientX,
                                        event.clientY,
                                      ),
                                    sourceConfigUuid: config.uuid,
                                    sourceOutputName: output.name,
                                  })
                                }}
                                ref={(element) =>
                                  registerAnchor(anchorKey, element)
                                }
                                type="button"
                              >
                                O
                              </button>
                              <PortTooltip port={output} side="output" />
                            </div>
                          </div>
                        )
                      })}
                    </div>
                  </div>
                </div>
              </div>
            )
          })}
        </>
      ) : (
        <div className="flex h-full min-h-[36rem] items-center justify-center px-6 text-center text-sm text-slate-500">
          Move configs from Available configs to begin building the workflow.
          Drag cards to arrange them, then connect an output box to a compatible
          input box.
        </div>
      )}

      <div className="sr-only" aria-live="polite">
        {drawingConnection
          ? `Drawing from ${drawingConnection.sourceOutputName}`
          : ''}
      </div>
    </div>
  )
}
