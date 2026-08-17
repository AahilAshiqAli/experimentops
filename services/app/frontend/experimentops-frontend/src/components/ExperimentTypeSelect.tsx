import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'

import { useDebouncedValue } from '../hooks'
import { useQueryExperimentTypes } from '../queries'
import type { ExperimentType } from '../services/experimentType.service'
import { ApiServiceError } from '../utils/api.service'

const EXPERIMENT_TYPES_PER_PAGE = 10

export function ExperimentTypeSelect({
  disabled = false,
  label,
  onChange,
  required = false,
  selectedType,
}: {
  disabled?: boolean
  label: string
  onChange: (experimentType: ExperimentType | null) => void
  required?: boolean
  selectedType: ExperimentType | null
}) {
  const { t } = useTranslation()
  const [page, setPage] = useState(1)
  const [name, setName] = useState('')
  const debouncedName = useDebouncedValue(name.trim())
  const params = useMemo(
    () => ({
      name: debouncedName || undefined,
      page: page - 1,
      size: EXPERIMENT_TYPES_PER_PAGE,
    }),
    [debouncedName, page],
  )
  const experimentTypesQuery = useQueryExperimentTypes(params, !disabled)
  const totalElements = experimentTypesQuery.data?.totalElements ?? 0
  const totalPages = Math.max(
    1,
    Math.ceil(totalElements / EXPERIMENT_TYPES_PER_PAGE),
  )
  const activePage = Math.min(page, totalPages)
  const experimentTypes = experimentTypesQuery.data?.data ?? []
  const options =
    selectedType &&
    !experimentTypes.some((type) => type.uuid === selectedType.uuid)
      ? [selectedType, ...experimentTypes]
      : experimentTypes

  const handleSearchChange = (nextName: string) => {
    setName(nextName)
    setPage(1)
  }

  return (
    <div>
      <span className="block text-sm font-medium text-secondary">{label}</span>
      <label className="mt-1 block">
        <span className="sr-only">{t('configs.searchTypes')}</span>
        <input
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/20"
          disabled={disabled}
          onChange={(event) => handleSearchChange(event.target.value)}
          placeholder={t('configs.searchTypes')}
          type="search"
          value={name}
        />
      </label>
      <label className="mt-2 block">
        <span className="sr-only">{label}</span>
        <select
          className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
          disabled={disabled || experimentTypesQuery.isFetching}
          onChange={(event) => {
            const experimentType = options.find(
              (type) => type.uuid === event.target.value,
            )
            onChange(experimentType ?? null)
          }}
          required={required}
          value={selectedType?.uuid ?? ''}
        >
          <option value="">
            {experimentTypesQuery.isFetching
              ? t('configs.loadingTypes')
              : t('configs.selectType')}
          </option>
          {options.map((experimentType) => (
            <option key={experimentType.uuid} value={experimentType.uuid}>
              {experimentType.name}
            </option>
          ))}
        </select>
      </label>

      {experimentTypesQuery.error ? (
        <p className="mt-2 text-sm text-red-600">
          {experimentTypesQuery.error instanceof ApiServiceError
            ? experimentTypesQuery.error.message
            : t('configs.errors.loadTypes')}
        </p>
      ) : null}

      {totalPages > 1 ? (
        <div className="mt-2 flex items-center justify-between gap-3 text-xs text-slate-500">
          <span>
            {t('common.table.pageOf', {
              page: activePage,
              totalPages,
            })}
          </span>
          <div className="flex gap-1.5">
            <button
              className="rounded border border-slate-300 px-2 py-1 font-medium disabled:opacity-50"
              disabled={disabled || activePage <= 1}
              onClick={() => setPage(activePage - 1)}
              type="button"
            >
              {t('common.table.previous')}
            </button>
            <button
              className="rounded border border-slate-300 px-2 py-1 font-medium disabled:opacity-50"
              disabled={disabled || activePage >= totalPages}
              onClick={() => setPage(activePage + 1)}
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
