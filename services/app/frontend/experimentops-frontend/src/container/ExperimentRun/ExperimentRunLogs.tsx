import { useMemo, useState } from 'react'

import {
  EXPERIMENT_RUN_LOG_LEVELS,
  type ExperimentRunLog,
  type ExperimentRunLogLevel,
} from '../../services/experimentRunLog.service'
import { formatLabel } from '../ProjectDetails/projectDetails.utils'

type LogLevelFilter = 'ALL' | ExperimentRunLogLevel

function SearchIcon() {
  return (
    <svg aria-hidden="true" className="h-5 w-5" fill="none" viewBox="0 0 24 24">
      <circle cx="11" cy="11" r="7" stroke="currentColor" strokeWidth="1.8" />
      <path
        d="m16 16 4 4"
        stroke="currentColor"
        strokeLinecap="round"
        strokeWidth="1.8"
      />
    </svg>
  )
}

function DownloadIcon({ isLoading }: { isLoading: boolean }) {
  if (isLoading) {
    return (
      <svg
        aria-hidden="true"
        className="h-5 w-5 animate-spin"
        fill="none"
        viewBox="0 0 24 24"
      >
        <circle
          className="opacity-25"
          cx="12"
          cy="12"
          r="9"
          stroke="currentColor"
          strokeWidth="3"
        />
        <path
          className="opacity-75"
          d="M21 12a9 9 0 0 0-9-9"
          stroke="currentColor"
          strokeLinecap="round"
          strokeWidth="3"
        />
      </svg>
    )
  }

  return (
    <svg aria-hidden="true" className="h-5 w-5" fill="none" viewBox="0 0 24 24">
      <path
        d="M12 3v12m0 0 4-4m-4 4-4-4M5 17v2a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-2"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  )
}

function RefreshIcon({ isLoading }: { isLoading: boolean }) {
  return (
    <svg
      aria-hidden="true"
      className={`h-5 w-5 ${isLoading ? 'animate-spin' : ''}`}
      fill="none"
      viewBox="0 0 24 24"
    >
      <path
        d="M20 7v5h-5M4 17v-5h5m10.2-3A8 8 0 0 0 6.1 6.1L4 8m16 8-2.1 1.9A8 8 0 0 1 4.8 15"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  )
}

function ChevronIcon({ direction }: { direction: 'left' | 'right' }) {
  return (
    <svg aria-hidden="true" className="h-4 w-4" fill="none" viewBox="0 0 24 24">
      <path
        d={direction === 'left' ? 'm15 18-6-6 6-6' : 'm9 6 6 6-6 6'}
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      />
    </svg>
  )
}

function formatLogTimestamp(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'medium',
      }).format(date)
}

function LogLevelBadge({ level }: { level: ExperimentRunLogLevel }) {
  return (
    <span
      className={`inline-flex min-w-16 justify-center rounded-full border px-2.5 py-1 text-[11px] font-bold tracking-wide ${
        level === 'ERROR'
          ? 'border-red-200 bg-red-50 text-red-700'
          : 'border-sky-200 bg-sky-50 text-sky-700'
      }`}
    >
      {level}
    </span>
  )
}

export function ExperimentRunLogs({
  isDownloading,
  isFetching,
  isLoading,
  logs,
  onDownload,
  onPageChange,
  onRefresh,
  page,
  pageSize,
  totalElements,
}: {
  isDownloading: boolean
  isFetching: boolean
  isLoading: boolean
  logs: ExperimentRunLog[]
  onDownload: () => void
  onPageChange: (page: number) => void
  onRefresh: () => void
  page: number
  pageSize: number
  totalElements: number
}) {
  const [level, setLevel] = useState<LogLevelFilter>('ALL')
  const [search, setSearch] = useState('')
  const totalPages = Math.max(1, Math.ceil(totalElements / pageSize))

  const filteredLogs = useMemo(() => {
    const term = search.trim().toLocaleLowerCase()
    return logs.filter((log) => {
      const matchesLevel = level === 'ALL' || log.level === level
      const matchesSearch =
        !term ||
        log.message.toLocaleLowerCase().includes(term) ||
        log.experimentType.toLocaleLowerCase().includes(term) ||
        log.level.toLocaleLowerCase().includes(term) ||
        String(log.sequence).includes(term)
      return matchesLevel && matchesSearch
    })
  }, [level, logs, search])

  const visibleStart = totalElements ? page * pageSize + 1 : 0
  const visibleEnd = Math.min((page + 1) * pageSize, totalElements)

  return (
    <div className="mt-4" role="tabpanel">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
        <label className="relative block min-w-0 flex-1 lg:max-w-md">
          <span className="sr-only">Search logs on this page</span>
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">
            <SearchIcon />
          </span>
          <input
            className="w-full rounded-lg border border-slate-300 bg-white py-2.5 pl-10 pr-3 text-sm outline-none placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/20"
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search logs on this page..."
            type="search"
            value={search}
          />
        </label>

        <div
          aria-label="Filter logs by level"
          className="inline-flex w-fit overflow-hidden rounded-lg border border-slate-300 bg-white"
          role="group"
        >
          {(['ALL', ...EXPERIMENT_RUN_LOG_LEVELS] as const).map((option) => (
            <button
              aria-pressed={level === option}
              className={`border-r border-slate-200 px-4 py-2.5 text-sm font-medium transition last:border-r-0 ${
                level === option
                  ? 'bg-sky-50 text-primary shadow-inner'
                  : 'text-slate-600 hover:bg-slate-50'
              }`}
              key={option}
              onClick={() => setLevel(option)}
              type="button"
            >
              {formatLabel(option)}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-2 lg:ml-auto">
          <button
            aria-label="Refresh logs"
            className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-slate-300 bg-white text-slate-500 transition hover:border-primary hover:text-primary disabled:cursor-wait disabled:opacity-60"
            disabled={isFetching}
            onClick={onRefresh}
            title="Refresh logs"
            type="button"
          >
            <RefreshIcon isLoading={isFetching} />
          </button>
          <button
            className="inline-flex h-10 items-center gap-2 rounded-lg bg-primary px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-primary/90 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:cursor-wait disabled:opacity-60"
            disabled={isDownloading}
            onClick={onDownload}
            type="button"
          >
            <DownloadIcon isLoading={isDownloading} />
            {isDownloading ? 'Preparing...' : 'Download logs'}
          </button>
        </div>
      </div>

      {isLoading ? (
        <div
          aria-label="Loading experiment run logs"
          className="mt-4 space-y-2 rounded-xl border border-slate-200 bg-white p-4"
        >
          {[0, 1, 2, 3].map((row) => (
            <div
              className="h-14 animate-pulse rounded bg-slate-100"
              key={row}
            />
          ))}
        </div>
      ) : filteredLogs.length === 0 ? (
        <div className="mt-4 rounded-xl border border-dashed border-slate-300 bg-white px-6 py-12 text-center text-sm text-slate-500">
          {logs.length
            ? 'No logs on this page match the current search or level.'
            : 'No logs are available for this run.'}
        </div>
      ) : (
        <div className="mt-4 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="w-20 px-4 py-3 font-semibold" scope="col">
                    Seq.
                  </th>
                  <th className="min-w-52 px-4 py-3 font-semibold" scope="col">
                    Timestamp
                  </th>
                  <th className="w-24 px-4 py-3 font-semibold" scope="col">
                    Level
                  </th>
                  <th className="min-w-48 px-4 py-3 font-semibold" scope="col">
                    Experiment type
                  </th>
                  <th className="min-w-96 px-4 py-3 font-semibold" scope="col">
                    Message
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredLogs.map((log, index) => (
                  <tr
                    className={`align-top transition hover:bg-slate-50 ${
                      log.level === 'ERROR' ? 'bg-red-50/30' : ''
                    }`}
                    key={`${log.timestamp}-${log.sequence}-${index}`}
                  >
                    <td className="px-4 py-3.5 font-mono text-xs text-slate-500">
                      {log.sequence}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3.5 text-slate-600">
                      {formatLogTimestamp(log.timestamp)}
                    </td>
                    <td className="px-4 py-3">
                      <LogLevelBadge level={log.level} />
                    </td>
                    <td className="px-4 py-3.5 text-xs font-semibold text-slate-600">
                      {formatLabel(log.experimentType)}
                    </td>
                    <td className="px-4 py-3.5 font-mono text-xs leading-5 text-secondary">
                      {log.message}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {!isLoading && totalElements > 0 ? (
        <div className="mt-3 flex flex-col gap-3 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between">
          <p aria-live="polite">
            Showing {visibleStart}–{visibleEnd} of {totalElements} logs
            {filteredLogs.length !== logs.length
              ? ` · ${filteredLogs.length} match${filteredLogs.length === 1 ? '' : 'es'} on this page`
              : ''}
          </p>
          <div className="flex items-center gap-2">
            <button
              aria-label="Previous log page"
              className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-slate-300 bg-white text-slate-600 transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-40"
              disabled={page === 0 || isFetching}
              onClick={() => onPageChange(page - 1)}
              type="button"
            >
              <ChevronIcon direction="left" />
            </button>
            <span className="min-w-24 text-center font-medium text-slate-600">
              Page {page + 1} of {totalPages}
            </span>
            <button
              aria-label="Next log page"
              className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-slate-300 bg-white text-slate-600 transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-40"
              disabled={page >= totalPages - 1 || isFetching}
              onClick={() => onPageChange(page + 1)}
              type="button"
            >
              <ChevronIcon direction="right" />
            </button>
          </div>
        </div>
      ) : null}
    </div>
  )
}
