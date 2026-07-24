import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

export type DataTableColumn<T> = {
  align?: 'left' | 'center' | 'right'
  cell?: (row: T, rowIndex: number) => ReactNode
  className?: string
  filter?: boolean
  header: ReactNode
  key: string
  sort?: boolean
  value?: (row: T) => unknown
}

export type DataTablePagination = {
  onPageChange: (page: number) => void
  page: number
  totalItems?: number
  totalPages: number
}

export type DataTableSort = {
  direction: 'asc' | 'desc'
  key: string
} | null

export type DataTableQuery = {
  filters: Record<string, string>
  search: string
  sort: DataTableSort
}

type DataTableProps<T> = {
  columns: DataTableColumn<T>[]
  data: T[]
  emptyMessage?: string
  getRowKey: (row: T) => string
  isLoading?: boolean
  loadingLabel?: string
  onQueryChange?: (query: DataTableQuery) => void
  onRowClick?: (row: T) => void
  pagination?: DataTablePagination
  searchPlaceholder?: string
  searchable?: boolean
  toolbarEnd?: ReactNode
  toolbarStart?: ReactNode
}

function comparable(value: unknown) {
  if (typeof value === 'number') return value
  return String(value ?? '').toLocaleLowerCase()
}

function displayValue(value: unknown) {
  return value === null || value === undefined || value === ''
    ? '—'
    : String(value)
}

const alignmentClasses = {
  center: 'text-center',
  left: 'text-left',
  right: 'text-right',
} as const

export function DataTable<T>({
  columns,
  data,
  emptyMessage,
  getRowKey,
  isLoading = false,
  loadingLabel,
  onQueryChange,
  onRowClick,
  pagination,
  searchPlaceholder,
  searchable = true,
  toolbarEnd,
  toolbarStart,
}: DataTableProps<T>) {
  const { i18n, t } = useTranslation()
  const language = i18n.resolvedLanguage ?? i18n.language
  const [search, setSearch] = useState('')
  const [filters, setFilters] = useState<Record<string, string>>({})
  const [sort, setSort] = useState<DataTableSort>(null)

  useEffect(() => {
    onQueryChange?.({ filters, search, sort })
  }, [filters, onQueryChange, search, sort])

  const filterOptions = useMemo(
    () =>
      Object.fromEntries(
        columns
          .filter((column) => column.filter && column.value)
          .map((column) => [
            column.key,
            [...new Set(data.map((row) => displayValue(column.value?.(row))))]
              .filter((value) => value !== '—')
              .sort((left, right) => left.localeCompare(right, language)),
          ]),
      ),
    [columns, data, language],
  )

  const visibleData = useMemo(() => {
    const term = search.trim().toLocaleLowerCase()
    const rows = data.filter((row) => {
      const matchesSearch =
        !term ||
        columns.some((column) =>
          displayValue(column.value?.(row)).toLocaleLowerCase().includes(term),
        )
      const matchesFilters = columns.every((column) => {
        const filterValue = filters[column.key]
        return !filterValue || displayValue(column.value?.(row)) === filterValue
      })
      return matchesSearch && matchesFilters
    })

    if (!sort) return rows
    const column = columns.find((candidate) => candidate.key === sort.key)
    if (!column?.value) return rows

    return [...rows].sort((left, right) => {
      const leftValue = comparable(column.value?.(left))
      const rightValue = comparable(column.value?.(right))
      const result =
        typeof leftValue === 'number' && typeof rightValue === 'number'
          ? leftValue - rightValue
          : String(leftValue).localeCompare(String(rightValue), language, {
              numeric: true,
            })
      return sort.direction === 'asc' ? result : -result
    })
  }, [columns, data, filters, language, search, sort])

  const hasFilters = columns.some((column) => column.filter)

  return (
    <div>
      {searchable || hasFilters || toolbarStart || toolbarEnd ? (
        <div
          className={`mb-3 flex flex-wrap items-center gap-2 ${
            toolbarStart ? 'justify-between' : 'justify-end'
          }`}
        >
          {toolbarStart ? <div>{toolbarStart}</div> : null}
          <div className="ml-auto flex flex-wrap items-center justify-end gap-2">
            {hasFilters
              ? columns
                  .filter((column) => column.filter)
                  .map((column) => (
                    <label className="min-w-40" key={column.key}>
                      <span className="sr-only">
                        {t('common.table.filterColumn')}
                      </span>
                      <select
                        className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-600 outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
                        onChange={(event) =>
                          setFilters((current) => ({
                            ...current,
                            [column.key]: event.target.value,
                          }))
                        }
                        value={filters[column.key] ?? ''}
                      >
                        <option value="">
                          {t('common.table.allColumn', {
                            column: String(column.header),
                          })}
                        </option>
                        {(filterOptions[column.key] ?? []).map((option) => (
                          <option key={option} value={option}>
                            {option}
                          </option>
                        ))}
                      </select>
                    </label>
                  ))
              : null}
            {searchable ? (
              <label className="block min-w-56 sm:w-72">
                <span className="sr-only">{t('common.table.search')}</span>
                <input
                  className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/20"
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder={
                    searchPlaceholder ?? t('common.table.searchPlaceholder')
                  }
                  type="search"
                  value={search}
                />
              </label>
            ) : null}
            {toolbarEnd}
          </div>
        </div>
      ) : null}

      <div className="overflow-x-auto rounded-lg border border-slate-200">
        <table className="min-w-full divide-y divide-slate-200 text-left">
          <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
            <tr>
              {columns.map((column) => (
                <th
                  className={`px-5 py-3 font-semibold ${alignmentClasses[column.align ?? 'left']}`}
                  key={column.key}
                  scope="col"
                >
                  {column.sort && column.value ? (
                    <button
                      className="inline-flex items-center gap-1 hover:text-primary"
                      onClick={() =>
                        setSort((current) =>
                          current?.key === column.key
                            ? current.direction === 'asc'
                              ? { direction: 'desc', key: column.key }
                              : null
                            : { direction: 'asc', key: column.key },
                        )
                      }
                      type="button"
                    >
                      {column.header}
                      <span aria-hidden="true">
                        {sort?.key === column.key
                          ? sort.direction === 'asc'
                            ? '↑'
                            : '↓'
                          : '↕'}
                      </span>
                    </button>
                  ) : (
                    column.header
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 bg-white">
            {isLoading
              ? [0, 1, 2].map((row) => (
                  <tr key={row}>
                    {columns.map((column) => (
                      <td className="px-5 py-4" key={column.key}>
                        <div className="h-5 animate-pulse rounded bg-slate-100" />
                      </td>
                    ))}
                  </tr>
                ))
              : visibleData.map((row, rowIndex) => (
                  <tr
                    className={
                      onRowClick
                        ? 'cursor-pointer transition hover:bg-slate-50 focus-visible:bg-slate-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-inset focus-visible:outline-primary'
                        : 'transition hover:bg-slate-50'
                    }
                    key={getRowKey(row)}
                    onClick={() => onRowClick?.(row)}
                    onKeyDown={(event) => {
                      if (
                        onRowClick &&
                        (event.key === 'Enter' || event.key === ' ')
                      ) {
                        event.preventDefault()
                        onRowClick(row)
                      }
                    }}
                    role={onRowClick ? 'link' : undefined}
                    tabIndex={onRowClick ? 0 : undefined}
                  >
                    {columns.map((column) => (
                      <td
                        className={`px-5 py-4 ${alignmentClasses[column.align ?? 'left']} ${column.className ?? ''}`}
                        key={column.key}
                      >
                        {column.cell
                          ? column.cell(row, rowIndex)
                          : displayValue(column.value?.(row))}
                      </td>
                    ))}
                  </tr>
                ))}
          </tbody>
        </table>
        {!isLoading && visibleData.length === 0 ? (
          <p className="px-6 py-12 text-center text-sm text-slate-500">
            {search || Object.values(filters).some(Boolean)
              ? t('common.table.noMatches')
              : (emptyMessage ?? t('common.table.empty'))}
          </p>
        ) : null}
        {isLoading ? (
          <span className="sr-only">
            {loadingLabel ?? t('common.table.loadingData')}
          </span>
        ) : null}
      </div>

      {pagination && pagination.totalPages > 1 ? (
        <div className="mt-5 flex items-center justify-between gap-4 text-sm text-slate-600">
          <span>
            {t('common.table.pageOf', {
              page: pagination.page,
              totalPages: pagination.totalPages,
            })}
            {pagination.totalItems !== undefined
              ? ` · ${t('common.table.items', {
                  count: pagination.totalItems,
                  formattedCount:
                    pagination.totalItems.toLocaleString(language),
                })}`
              : ''}
          </span>
          <div className="flex gap-2">
            <button
              className="rounded-md border border-slate-300 px-3 py-2 font-medium disabled:opacity-50"
              disabled={pagination.page <= 1}
              onClick={() => pagination.onPageChange(pagination.page - 1)}
              type="button"
            >
              {t('common.table.previous')}
            </button>
            <button
              className="rounded-md border border-slate-300 px-3 py-2 font-medium disabled:opacity-50"
              disabled={pagination.page >= pagination.totalPages}
              onClick={() => pagination.onPageChange(pagination.page + 1)}
              type="button"
            >
              {t('common.table.next')}
            </button>
          </div>
        </div>
      ) : null}
    </div>
  )
}
