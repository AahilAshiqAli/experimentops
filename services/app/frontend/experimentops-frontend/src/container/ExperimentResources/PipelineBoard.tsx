import { useState, type PointerEvent } from 'react'

import type { ExperimentConfig } from '../../services/experimentConfig.service'
import type {
  BoardPoint,
  DraggingCard,
  DrawingConnection,
  PipelineConnection,
} from './experimentRunBuilder.types'

const BOARD_NODE_HEIGHT = 96
const BOARD_NODE_WIDTH_PERCENT = 18
const BOARD_NODE_VERTICAL_GAP = 72

export function PipelineBoard({
  configs,
  connections,
  onConnect,
  onRemoveConnection,
  onRemoveFromBoard,
  pipelineOrder,
}: {
  configs: ExperimentConfig[]
  connections: PipelineConnection[]
  onConnect: (fromUuid: string, targetUuid: string) => void
  onRemoveConnection: (connection: PipelineConnection) => void
  onRemoveFromBoard: (configUuid: string) => void
  pipelineOrder: ExperimentConfig[]
}) {
  const [drawingConnection, setDrawingConnection] =
    useState<DrawingConnection | null>(null)
  const [draggingCard, setDraggingCard] = useState<DraggingCard | null>(null)
  const [nodePositions, setNodePositions] = useState<
    Record<string, BoardPoint>
  >({})
  const boardHeight = Math.max(
    640,
    configs.length * (BOARD_NODE_HEIGHT + BOARD_NODE_VERTICAL_GAP) + 80,
  )
  const orderByUuid = new Map(
    pipelineOrder.map((config, index) => [config.uuid, index + 1]),
  )
  const nodeHeightPercent = (BOARD_NODE_HEIGHT / boardHeight) * 100
  const getNodePosition = (config: ExperimentConfig, index: number) =>
    nodePositions[config.uuid] ?? {
      x: 50 - BOARD_NODE_WIDTH_PERCENT / 2,
      y:
        ((40 + index * (BOARD_NODE_HEIGHT + BOARD_NODE_VERTICAL_GAP)) /
          boardHeight) *
        100,
    }
  const getNodeCenter = (configUuid: string) => {
    const index = configs.findIndex((config) => config.uuid === configUuid)
    const config = configs[index]
    if (!config) return null
    const position = getNodePosition(config, index)

    return {
      x: position.x + BOARD_NODE_WIDTH_PERCENT / 2,
      y: position.y + nodeHeightPercent / 2,
    }
  }
  const clampPosition = (point: BoardPoint): BoardPoint => ({
    x: Math.min(100 - BOARD_NODE_WIDTH_PERCENT - 1, Math.max(1, point.x)),
    y: Math.min(100 - nodeHeightPercent - 1, Math.max(1, point.y)),
  })
  const toBoardPoint = (event: PointerEvent<HTMLDivElement>): BoardPoint => {
    const rect = event.currentTarget.getBoundingClientRect()

    return {
      x: ((event.clientX - rect.left) / rect.width) * 100,
      y: ((event.clientY - rect.top) / rect.height) * 100,
    }
  }
  const toBoardPointFromElement = (
    event: PointerEvent<HTMLElement>,
    element: HTMLElement | null,
  ): BoardPoint => {
    const rect = element?.getBoundingClientRect()

    if (!rect) return { x: 0, y: 0 }

    return {
      x: ((event.clientX - rect.left) / rect.width) * 100,
      y: ((event.clientY - rect.top) / rect.height) * 100,
    }
  }
  const endDrawing = (event: PointerEvent<HTMLDivElement>) => {
    if (!drawingConnection) return

    const target = (event.target as HTMLElement).closest<HTMLElement>(
      '[data-board-config-uuid]',
    )
    const targetUuid = target?.dataset.boardConfigUuid

    if (targetUuid && targetUuid !== drawingConnection.fromUuid) {
      onConnect(drawingConnection.fromUuid, targetUuid)
    }

    setDrawingConnection(null)
  }
  const endCardDrag = () => setDraggingCard(null)

  return (
    <div
      className="relative mt-6 flex-1 cursor-crosshair overflow-hidden rounded-xl border border-dashed border-slate-300 bg-slate-50 bg-[radial-gradient(circle_at_1px_1px,rgba(148,163,184,0.35)_1px,transparent_0)] [background-size:24px_24px]"
      onPointerCancel={() => setDrawingConnection(null)}
      onPointerMove={(event) => {
        const point = toBoardPoint(event)

        if (drawingConnection) {
          setDrawingConnection({
            ...drawingConnection,
            pointer: point,
          })
          return
        }

        if (draggingCard) {
          setNodePositions((current) => ({
            ...current,
            [draggingCard.uuid]: clampPosition({
              x: point.x - draggingCard.offset.x,
              y: point.y - draggingCard.offset.y,
            }),
          }))
        }
      }}
      onPointerUp={(event) => {
        endDrawing(event)
        endCardDrag()
      }}
      style={{ minHeight: boardHeight }}
    >
      {configs.length ? (
        <>
          <svg
            aria-hidden="true"
            className="pointer-events-none absolute inset-0 h-full w-full"
            preserveAspectRatio="none"
            viewBox="0 0 100 100"
          >
            <defs>
              <marker
                id="pipeline-arrow"
                markerHeight="8"
                markerWidth="8"
                orient="auto"
                refX="6"
                refY="4"
                viewBox="0 0 8 8"
              >
                <path d="M0,0 L8,4 L0,8 Z" fill="#2563EB" />
              </marker>
            </defs>
            {connections.map((connection) => {
              const from = getNodeCenter(connection.fromUuid)
              const to = getNodeCenter(connection.toUuid)
              if (!from || !to) return null

              const midY = from.y + (to.y - from.y) / 2

              return (
                <path
                  d={`M ${from.x} ${from.y} C ${from.x} ${midY}, ${to.x} ${midY}, ${to.x} ${to.y}`}
                  fill="none"
                  key={`${connection.fromUuid}-${connection.toUuid}`}
                  markerEnd="url(#pipeline-arrow)"
                  stroke="#2563EB"
                  strokeLinecap="round"
                  strokeWidth="0.5"
                />
              )
            })}
            {drawingConnection ? (
              <path
                d={`M ${getNodeCenter(drawingConnection.fromUuid)?.x ?? 50} ${getNodeCenter(drawingConnection.fromUuid)?.y ?? 50} L ${drawingConnection.pointer.x} ${drawingConnection.pointer.y}`}
                fill="none"
                markerEnd="url(#pipeline-arrow)"
                stroke="#0EA5E9"
                strokeDasharray="2 1.5"
                strokeLinecap="round"
                strokeWidth="0.45"
              />
            ) : null}
          </svg>

          {configs.map((config, index) => {
            const isDrawingFrom = drawingConnection?.fromUuid === config.uuid
            const stepNumber = orderByUuid.get(config.uuid)
            const position = getNodePosition(config, index)

            return (
              <div
                className={`absolute w-56 select-none rounded-xl border bg-white p-4 shadow-sm transition hover:shadow-card ${
                  isDrawingFrom
                    ? 'border-primary ring-4 ring-primary/10'
                    : 'border-slate-200'
                }`}
                data-board-config-uuid={config.uuid}
                key={config.uuid}
                onPointerDown={(event) => {
                  if ((event.target as HTMLElement).closest('button')) return
                  const point = toBoardPointFromElement(
                    event,
                    event.currentTarget.parentElement,
                  )
                  setDraggingCard({
                    offset: {
                      x: point.x - position.x,
                      y: point.y - position.y,
                    },
                    uuid: config.uuid,
                  })
                }}
                style={{
                  cursor:
                    draggingCard?.uuid === config.uuid ? 'grabbing' : 'grab',
                  left: `${position.x}%`,
                  top: `${position.y}%`,
                }}
              >
                {[
                  '-left-1 -top-1',
                  '-right-1 -top-1',
                  '-bottom-1 -left-1',
                  '-bottom-1 -right-1',
                ].map((className) => (
                  <button
                    aria-label={`Draw arrow from ${config.name}`}
                    className={`absolute h-4 w-4 rounded-full border-2 border-white bg-primary shadow-sm hover:scale-125 ${className}`}
                    key={className}
                    onPointerDown={(event) => {
                      event.stopPropagation()
                      const point = toBoardPointFromElement(
                        event,
                        event.currentTarget.parentElement?.parentElement ??
                          null,
                      )
                      setDrawingConnection({
                        fromUuid: config.uuid,
                        pointer: point,
                      })
                    }}
                    style={{ cursor: 'crosshair' }}
                    title="Draw arrow"
                    type="button"
                  />
                ))}
                <div className="flex items-start justify-between gap-3">
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
                  <button
                    aria-label={`Remove ${config.name} from board`}
                    className="rounded-full px-2 py-1 text-xs font-bold leading-none text-red-600 hover:bg-red-50"
                    onClick={() => onRemoveFromBoard(config.uuid)}
                    type="button"
                  >
                    ×
                  </button>
                </div>
                <div className="mt-3 flex items-center justify-between gap-3 text-xs text-slate-500">
                  {stepNumber ? (
                    <span className="rounded-full bg-primary px-2 py-1 font-bold text-white">
                      {stepNumber}
                    </span>
                  ) : null}
                </div>
              </div>
            )
          })}

          {connections.length ? (
            <div className="absolute bottom-4 right-4 max-w-xs rounded-lg border border-slate-200 bg-white/95 p-3 shadow-sm">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                Arrows
              </p>
              <div className="mt-2 space-y-1">
                {connections.map((connection) => {
                  const from = configs.find(
                    (config) => config.uuid === connection.fromUuid,
                  )
                  const to = configs.find(
                    (config) => config.uuid === connection.toUuid,
                  )

                  return (
                    <div
                      className="flex items-center gap-2 text-xs text-slate-600"
                      key={`${connection.fromUuid}-${connection.toUuid}`}
                    >
                      <span className="min-w-0 flex-1 truncate">
                        {from?.name ?? 'Config'} → {to?.name ?? 'Config'}
                      </span>
                      <button
                        className="font-semibold text-red-600 hover:underline"
                        onClick={() => onRemoveConnection(connection)}
                        type="button"
                      >
                        Remove
                      </button>
                    </div>
                  )
                })}
              </div>
            </div>
          ) : null}
        </>
      ) : (
        <div className="flex h-full min-h-[36rem] items-center justify-center px-6 text-center text-sm text-slate-500">
          Move configs from Available configs to begin sketching the workflow. Drag card to move. Drag corner dot to connect.
        </div>
      )}
    </div>
  )
}
