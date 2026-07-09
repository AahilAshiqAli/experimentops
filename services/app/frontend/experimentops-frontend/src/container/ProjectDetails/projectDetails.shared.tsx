import { useState, type ReactNode } from 'react'
import { Link, NavLink } from 'react-router-dom'

import { useMutationUpdateProject } from '../../queries'
import type { Dataset } from '../../services/dataset.service'
import type { Experiment } from '../../services/experiment.service'
import type { ProjectSummary } from '../../services/project.service'
import { Toaster } from '../../services/toaster.service'
import { formatDate } from './projectDetails.utils'

export function ProjectFrame({
  activeTab,
  children,
  project,
}: {
  activeTab: 'datasets' | 'experiments' | 'overview'
  children: ReactNode
  project: ProjectSummary
}) {
  const updateProjectMutation = useMutationUpdateProject(project.projectUuid)
  const [editingField, setEditingField] = useState<
    'description' | 'name' | null
  >(null)
  const [draft, setDraft] = useState('')

  const beginEditing = (field: 'description' | 'name') => {
    setEditingField(field)
    setDraft(project[field])
  }

  const cancelEditing = () => {
    setEditingField(null)
    setDraft('')
  }

  const saveEdit = () => {
    if (!editingField) return

    const value = draft.trim()
    if (!value) {
      Toaster.error(
        editingField === 'name'
          ? 'Project name cannot be empty.'
          : 'Project description cannot be empty.',
      )
      return
    }
    if (value === project[editingField]) {
      cancelEditing()
      return
    }

    updateProjectMutation.mutate(
      {
        description:
          editingField === 'description' ? value : project.description,
        name: editingField === 'name' ? value : project.name,
      },
      {
        onError: (error) => {
          Toaster.error(
            error instanceof Error
              ? error.message
              : 'Unable to update the project.',
          )
        },
        onSuccess: () => {
          cancelEditing()
          Toaster.success('Project updated successfully.')
        },
      },
    )
  }

  return (
    <section>
      <header className="border-b border-slate-200 pb-0">
        <div className="pb-2">
          <h1 className="font-heading text-2xl font-semibold text-secondary">
            {editingField === 'name' ? (
              <input
                aria-label="Project name"
                autoFocus
                className="w-full max-w-2xl rounded-md border border-primary bg-white px-2 py-1 font-heading text-2xl font-semibold text-secondary outline-none ring-2 ring-primary/20"
                disabled={updateProjectMutation.isPending}
                onChange={(event) => setDraft(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') saveEdit()
                  if (event.key === 'Escape') cancelEditing()
                }}
                value={draft}
              />
            ) : (
              <button
                className="rounded text-left font-heading hover:text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
                onDoubleClick={() => beginEditing('name')}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' || event.key === 'F2') {
                    beginEditing('name')
                  }
                }}
                title="Double-click to edit project name"
                type="button"
              >
                {project.name}
              </button>
            )}
          </h1>
          <div className="mt-1 max-w-3xl text-sm text-slate-600">
            {editingField === 'description' ? (
              <input
                aria-label="Project description"
                autoFocus
                className="w-full rounded-md border border-primary bg-white px-2 py-1 text-slate-600 outline-none ring-2 ring-primary/20"
                disabled={updateProjectMutation.isPending}
                onChange={(event) => setDraft(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') saveEdit()
                  if (event.key === 'Escape') cancelEditing()
                }}
                value={draft}
              />
            ) : (
              <button
                className="rounded text-left hover:text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
                onDoubleClick={() => beginEditing('description')}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' || event.key === 'F2') {
                    beginEditing('description')
                  }
                }}
                title="Double-click to edit project description"
                type="button"
              >
                {project.description || 'No description provided.'}
              </button>
            )}
          </div>
        </div>
        <ProjectTabs activeTab={activeTab} projectUuid={project.projectUuid} />
      </header>
      <div className="py-3 sm:py-4">{children}</div>
    </section>
  )
}

function ProjectTabs({
  activeTab,
  projectUuid,
}: {
  activeTab: 'datasets' | 'experiments' | 'overview'
  projectUuid: string
}) {
  const tabs = [
    { key: 'overview', label: 'Overview', to: `/projects/${projectUuid}` },
    {
      key: 'experiments',
      label: 'Experiments',
      to: `/projects/${projectUuid}/experiments`,
    },
    {
      key: 'datasets',
      label: 'Datasets',
      to: `/projects/${projectUuid}/datasets`,
    },
  ] as const

  return (
    <nav aria-label="Project navigation" className="flex gap-6 overflow-x-auto">
      {tabs.map((tab) => (
        <NavLink
          className={`whitespace-nowrap border-b-2 pb-1.5 text-sm font-semibold transition ${
            activeTab === tab.key
              ? 'border-primary text-primary'
              : 'border-transparent text-slate-500 hover:border-slate-300 hover:text-secondary'
          }`}
          key={tab.key}
          to={tab.to}
        >
          {tab.label}
        </NavLink>
      ))}
    </nav>
  )
}

export function ExperimentPreview({
  experiment,
  projectUuid,
}: {
  experiment: Experiment
  projectUuid: string
}) {
  return (
    <article className="border-b border-slate-100 px-4 py-4 last:border-b-0">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0 flex-1">
          <h3 className="font-semibold text-secondary">{experiment.name}</h3>
          <p className="mt-1 line-clamp-2 text-sm text-slate-600">
            {experiment.description || 'No description provided.'}
          </p>
          <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500">
            <span>{experiment.runCount} runs</span>
            <span>{experiment.configCount} configs</span>
            <span>{formatDate(experiment.createdAt)}</span>
          </div>
        </div>
        <div
          aria-label="Experiment actions"
          className="flex shrink-0 gap-2 sm:flex-col"
        >
          <ExperimentActions
            experimentUuid={experiment.experimentUuid}
            projectUuid={projectUuid}
          />
        </div>
      </div>
    </article>
  )
}

export function ExperimentActions({
  experimentUuid,
  projectUuid,
}: {
  experimentUuid: string
  projectUuid: string
}) {
  const basePath = `/projects/${projectUuid}/experiments/${experimentUuid}`

  return (
    <>
      <Link
        className="rounded-md border border-slate-300 px-2.5 py-1.5 text-xs font-semibold text-secondary transition hover:border-primary hover:text-primary"
        to={`${basePath}/experiment-configs`}
      >
        Configs
      </Link>
      <Link
        className="rounded-md border border-slate-300 px-2.5 py-1.5 text-xs font-semibold text-secondary transition hover:border-primary hover:text-primary"
        to={`${basePath}/experiment-runs`}
      >
        Runs
      </Link>
    </>
  )
}

export function DatasetFolderCard({
  dataset,
  onOpen,
  projectUuid,
}: {
  dataset: Dataset
  onOpen?: (dataset: Dataset) => void
  projectUuid: string
}) {
  const className =
    'relative block min-w-0 rounded-lg border border-slate-200 bg-white p-3 text-left shadow-sm transition hover:-translate-y-0.5 hover:border-primary/50 hover:shadow-card focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary'
  const content = (
    <>
      <span
        aria-label={`${dataset.versionCount} versions`}
        className="absolute right-3 top-3 flex h-6 min-w-6 items-center justify-center rounded-full bg-primary px-1.5 text-[11px] font-bold text-white"
        title={`${dataset.versionCount} versions`}
      >
        {dataset.versionCount}
      </span>
      <FolderIcon />
      <h3 className="mt-2 truncate pr-7 text-sm font-semibold text-secondary">
        {dataset.name}
      </h3>
      <div className="mt-2 flex items-center justify-end gap-2 text-xs text-slate-500">
        <span>{formatDate(dataset.updatedAt)}</span>
      </div>
    </>
  )

  if (onOpen) {
    return (
      <button
        className={className}
        onClick={() => onOpen(dataset)}
        type="button"
      >
        {content}
      </button>
    )
  }

  return (
    <Link
      className={className}
      state={{ dataset }}
      to={`/projects/${projectUuid}/datasets/${dataset.datasetUuid}`}
    >
      {content}
    </Link>
  )
}

export function FolderIcon({
  className = 'h-10 w-10',
}: {
  className?: string
}) {
  return (
    <svg
      aria-hidden="true"
      className={`${className} text-amber-400`}
      fill="none"
      viewBox="0 0 64 64"
    >
      <path
        d="M6 17a5 5 0 0 1 5-5h14l6 7h22a5 5 0 0 1 5 5v25a5 5 0 0 1-5 5H11a5 5 0 0 1-5-5V17Z"
        fill="currentColor"
      />
      <path d="M6 25h52v24a5 5 0 0 1-5 5H11a5 5 0 0 1-5-5V25Z" fill="#FBBF24" />
    </svg>
  )
}

export function PageState({
  message,
  title,
  tone = 'neutral',
}: {
  message: string
  title: string
  tone?: 'error' | 'neutral'
}) {
  if (tone === 'error') return null

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-8 text-secondary shadow-card">
      <h1 className="font-heading text-2xl font-semibold">{title}</h1>
      <p className="mt-2 text-sm">{message}</p>
    </section>
  )
}

export function SectionState({
  message,
  tone = 'neutral',
}: {
  message: string
  tone?: 'error' | 'neutral'
}) {
  if (tone === 'error') return null

  return (
    <div className="rounded-lg border border-dashed border-slate-300 bg-white px-5 py-10 text-center text-sm text-slate-600">
      {message}
    </div>
  )
}

export function PageSkeleton() {
  return (
    <section
      className="space-y-4 rounded-2xl border border-slate-200 bg-white p-8 shadow-card"
      role="status"
    >
      <div className="h-9 w-64 animate-pulse rounded bg-slate-100" />
      <div className="h-5 max-w-xl animate-pulse rounded bg-slate-100" />
      <div className="h-56 animate-pulse rounded-xl bg-slate-100" />
      <span className="sr-only">Loading project</span>
    </section>
  )
}

export function Pagination({
  onPageChange,
  page,
  totalPages,
}: {
  onPageChange: (page: number) => void
  page: number
  totalPages: number
}) {
  if (totalPages <= 1) return null

  return (
    <nav
      aria-label="Pagination"
      className="flex items-center justify-center gap-2"
    >
      <button
        className="rounded-md border border-slate-300 px-3 py-2 text-sm font-medium text-secondary transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-40"
        disabled={page === 1}
        onClick={() => onPageChange(page - 1)}
        type="button"
      >
        Previous
      </button>
      <span className="px-2 text-sm text-slate-500">
        Page {page} of {totalPages}
      </span>
      <button
        className="rounded-md border border-slate-300 px-3 py-2 text-sm font-medium text-secondary transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-40"
        disabled={page === totalPages}
        onClick={() => onPageChange(page + 1)}
        type="button"
      >
        Next
      </button>
    </nav>
  )
}
