import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'

import { useMutationRunArtifactDownloadUrl } from '../../queries'
import type { RunArtifact } from '../../services/runArtifact.service'
import { Toaster } from '../../services/toaster.service'
import { formatLabel } from '../ProjectDetails/projectDetails.utils'
import { ArtifactIcon } from './ExperimentRunDetail.components'

type ArtifactFilter = 'ALL' | 'INTERMEDIATE' | 'PRIMARY'

const artifactFilterKeys = {
  ALL: 'runs.artifactList.all',
  INTERMEDIATE: 'runs.artifactList.intermediate',
  PRIMARY: 'runs.artifactList.primary',
} as const

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

function getDownloadFileName(artifact: RunArtifact) {
  const extension = artifact.format.trim().toLowerCase()
  return extension && !artifact.portName.toLowerCase().endsWith(`.${extension}`)
    ? `${artifact.portName}.${extension}`
    : artifact.portName
}

function triggerBrowserDownload(url: string, fileName: string) {
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = fileName
  anchor.rel = 'noopener'
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
}

function ArtifactTable({
  artifacts,
  downloadingUuid,
  onDownload,
  title,
}: {
  artifacts: RunArtifact[]
  downloadingUuid: string | null
  onDownload: (artifact: RunArtifact) => void
  title: string
}) {
  const { t } = useTranslation()
  return (
    <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <div className="flex items-center gap-3 border-b border-slate-200 px-4 py-3">
        <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-sky-50 text-primary">
          <ArtifactIcon className="h-4 w-4" />
        </span>
        <h3 className="font-semibold text-secondary">{title}</h3>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
            <tr>
              <th className="px-4 py-3 font-semibold" scope="col">
                {t('runs.artifactList.name')}
              </th>
              <th className="px-4 py-3 font-semibold" scope="col">
                {t('runs.artifactList.type')}
              </th>
              <th className="px-4 py-3 font-semibold" scope="col">
                {t('runs.artifactList.producedBy')}
              </th>
              <th className="px-4 py-3 font-semibold" scope="col">
                {t('runs.artifactList.format')}
              </th>
              <th className="px-4 py-3 font-semibold" scope="col">
                {t('runs.artifactList.downstreamPolicy')}
              </th>
              <th className="px-4 py-3 text-center font-semibold" scope="col">
                {t('runs.artifactList.actions')}
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {artifacts.map((artifact) => {
              const isDownloading = downloadingUuid === artifact.uuid
              return (
                <tr
                  className="transition hover:bg-slate-50"
                  key={artifact.uuid}
                >
                  <td className="px-4 py-3.5">
                    <span className="flex min-w-44 items-center gap-2.5 font-semibold text-secondary">
                      <span className="text-primary">
                        <ArtifactIcon className="h-5 w-5" />
                      </span>
                      {artifact.portName}
                    </span>
                  </td>
                  <td className="px-4 py-3.5 text-slate-600">
                    {formatLabel(artifact.artifactType)}
                  </td>
                  <td className="px-4 py-3.5 text-slate-600">
                    {formatLabel(artifact.experimentType)}
                  </td>
                  <td className="px-4 py-3.5 font-medium uppercase text-slate-600">
                    {artifact.format}
                  </td>
                  <td className="px-4 py-3.5 text-slate-600">
                    {formatLabel(artifact.downstreamPolicy)}
                  </td>
                  <td className="px-4 py-3.5 text-center">
                    <button
                      aria-label={t('runs.artifactList.download', {
                        name: artifact.portName,
                      })}
                      className="inline-flex h-9 w-9 items-center justify-center rounded-md text-slate-500 transition hover:bg-sky-50 hover:text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary disabled:cursor-wait disabled:opacity-60"
                      disabled={isDownloading}
                      onClick={() => onDownload(artifact)}
                      title={t('runs.artifactList.download', {
                        name: artifact.portName,
                      })}
                      type="button"
                    >
                      <DownloadIcon isLoading={isDownloading} />
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </section>
  )
}

export function ExperimentRunArtifacts({
  artifacts,
  error,
  isFetching,
  isLoading,
  onRefresh,
}: {
  artifacts: RunArtifact[]
  error: unknown
  isFetching: boolean
  isLoading: boolean
  onRefresh: () => void
}) {
  const { t } = useTranslation()
  const [filter, setFilter] = useState<ArtifactFilter>('ALL')
  const [search, setSearch] = useState('')
  const [downloadingUuid, setDownloadingUuid] = useState<string | null>(null)
  const downloadMutation = useMutationRunArtifactDownloadUrl()

  const filteredArtifacts = useMemo(() => {
    const term = search.trim().toLowerCase()
    return artifacts.filter((artifact) => {
      const matchesFilter =
        filter === 'ALL' || artifact.status.toUpperCase() === filter
      const matchesSearch =
        !term || artifact.portName.toLowerCase().includes(term)
      return matchesFilter && matchesSearch
    })
  }, [artifacts, filter, search])

  const primaryArtifacts = filteredArtifacts.filter(
    (artifact) => artifact.status.toUpperCase() === 'PRIMARY',
  )
  const intermediateArtifacts = filteredArtifacts.filter(
    (artifact) => artifact.status.toUpperCase() !== 'PRIMARY',
  )

  const handleDownload = async (artifact: RunArtifact) => {
    setDownloadingUuid(artifact.uuid)
    try {
      const download = await downloadMutation.mutateAsync(artifact.uuid)
      triggerBrowserDownload(download.url, getDownloadFileName(artifact))
    } catch (downloadError) {
      Toaster.error(
        downloadError instanceof Error
          ? downloadError.message
          : t('runs.artifactList.errors.download'),
      )
    } finally {
      setDownloadingUuid(null)
    }
  }

  return (
    <div className="mt-4" role="tabpanel">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <label className="relative block min-w-0 flex-1 sm:max-w-sm">
          <span className="sr-only">{t('runs.artifactList.searchLabel')}</span>
          <svg
            aria-hidden="true"
            className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              cx="11"
              cy="11"
              r="7"
              stroke="currentColor"
              strokeWidth="1.8"
            />
            <path
              d="m16 16 4 4"
              stroke="currentColor"
              strokeLinecap="round"
              strokeWidth="1.8"
            />
          </svg>
          <input
            className="w-full rounded-lg border border-slate-300 bg-white py-2.5 pl-10 pr-3 text-sm outline-none placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/20"
            onChange={(event) => setSearch(event.target.value)}
            placeholder={t('runs.artifactList.search')}
            type="search"
            value={search}
          />
        </label>

        <div className="inline-flex w-fit overflow-hidden rounded-lg border border-slate-300 bg-white">
          {(['ALL', 'PRIMARY', 'INTERMEDIATE'] as const).map((option) => (
            <button
              className={`border-r border-slate-200 px-4 py-2.5 text-sm font-medium transition last:border-r-0 ${
                filter === option
                  ? 'bg-sky-50 text-primary shadow-inner'
                  : 'text-slate-600 hover:bg-slate-50'
              }`}
              key={option}
              onClick={() => setFilter(option)}
              type="button"
            >
              {t(artifactFilterKeys[option])}
            </button>
          ))}
        </div>

        <button
          aria-label={t('runs.artifactList.refresh')}
          className="inline-flex h-10 w-10 items-center justify-center self-start rounded-lg border border-slate-300 bg-white text-slate-500 transition hover:border-primary hover:text-primary sm:ml-auto"
          disabled={isFetching}
          onClick={onRefresh}
          title={t('runs.artifactList.refresh')}
          type="button"
        >
          <RefreshIcon isLoading={isFetching} />
        </button>
      </div>

      {isLoading ? (
        <div className="mt-4 space-y-2 rounded-xl border border-slate-200 bg-white p-4">
          {[0, 1, 2].map((row) => (
            <div
              className="h-12 animate-pulse rounded bg-slate-100"
              key={row}
            />
          ))}
        </div>
      ) : error ? (
        <div className="mt-4 rounded-xl border border-red-200 bg-red-50 px-5 py-8 text-center text-sm text-red-700">
          {error instanceof Error
            ? error.message
            : t('runs.artifactList.errors.load')}
        </div>
      ) : filteredArtifacts.length === 0 ? (
        <div className="mt-4 rounded-xl border border-dashed border-slate-300 bg-white px-6 py-12 text-center text-sm text-slate-500">
          {artifacts.length
            ? t('runs.artifactList.noMatches')
            : t('runs.artifactList.none')}
        </div>
      ) : (
        <div className="mt-4 space-y-4">
          {primaryArtifacts.length ? (
            <ArtifactTable
              artifacts={primaryArtifacts}
              downloadingUuid={downloadingUuid}
              onDownload={handleDownload}
              title={t('runs.artifactList.primaryOutputs')}
            />
          ) : null}
          {intermediateArtifacts.length ? (
            <ArtifactTable
              artifacts={intermediateArtifacts}
              downloadingUuid={downloadingUuid}
              onDownload={handleDownload}
              title={t('runs.artifactList.intermediateOutputs')}
            />
          ) : null}
          <p className="text-xs text-slate-500">
            {t('runs.artifactList.showing', {
              total: artifacts.length,
              visible: filteredArtifacts.length,
            })}
          </p>
        </div>
      )}
    </div>
  )
}
