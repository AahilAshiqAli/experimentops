import { useMemo, useState, type FormEvent } from 'react'
import { useParams } from 'react-router-dom'

import { useLogin } from '../../context-api/logincontext'
import { useDocumentTitle } from '../../hooks'
import {
  useMutationCreateExperiment,
  useQueryProject,
  useQueryProjectExperiments,
} from '../../queries'
import { Toaster } from '../../services/toaster.service'
import { PERMISSIONS_KEYS } from '../../utils'
import {
  ExperimentActions,
  PageSkeleton,
  PageState,
  Pagination,
  ProjectFrame,
  SectionState,
} from '../ProjectDetails/projectDetails.shared'
import {
  formatDate,
  getErrorMessage,
} from '../ProjectDetails/projectDetails.utils'

const EXPERIMENTS_PER_PAGE = 8

export function ProjectExperiments() {
  const { projectUuid } = useParams<{ projectUuid: string }>()
  const { hasPermission } = useLogin()
  const projectQuery = useQueryProject(projectUuid)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const experimentsQuery = useQueryProjectExperiments(projectUuid, {
    page: page - 1,
    size: EXPERIMENTS_PER_PAGE,
  })
  const createExperimentMutation = useMutationCreateExperiment(
    projectUuid ?? '',
  )
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const canListExperiments = hasPermission(
    PERMISSIONS_KEYS.EXPERIMENT.GET_EXPERIMENT,
  )
  const filteredExperiments = useMemo(() => {
    const term = search.trim().toLowerCase()
    const experiments = experimentsQuery.data?.data ?? []

    return term
      ? experiments.filter((experiment) =>
          [
            experiment.name,
            experiment.description,
            experiment.status,
            experiment.experimentType,
          ]
            .join(' ')
            .toLowerCase()
            .includes(term),
        )
      : experiments
  }, [experimentsQuery.data, search])
  const totalExperiments = search
    ? filteredExperiments.length
    : (experimentsQuery.data?.totalElements ?? 0)
  const totalPages = Math.max(
    1,
    Math.ceil(totalExperiments / EXPERIMENTS_PER_PAGE),
  )
  const activePage = Math.min(page, totalPages)
  const visibleExperiments = search
    ? filteredExperiments.slice(
        (activePage - 1) * EXPERIMENTS_PER_PAGE,
        activePage * EXPERIMENTS_PER_PAGE,
      )
    : filteredExperiments

  useDocumentTitle(
    projectQuery.data ? `${projectQuery.data.name} experiments` : 'Experiments',
  )

  if (projectQuery.isLoading) return <PageSkeleton />
  if (projectQuery.error) {
    return (
      <PageState
        message={getErrorMessage(
          projectQuery.error,
          'Unable to load this project.',
        )}
        title="Unable to load project"
        tone="error"
      />
    )
  }
  if (!projectQuery.data) {
    return (
      <PageState
        message="This project is not available."
        title="Project not found"
      />
    )
  }

  return (
    <ProjectFrame activeTab="experiments" project={projectQuery.data}>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="font-heading text-2xl font-semibold text-secondary">
            Experiments
          </h2>
          <p className="mt-1 text-sm text-slate-600">
            Browse all experiments in this project.
          </p>
        </div>
        <div className="flex flex-col gap-3 sm:flex-row">
          <label className="block sm:w-72">
            <span className="sr-only">Search experiments</span>
            <input
              className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none transition placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/20"
              onChange={(event) => {
                setSearch(event.target.value)
                setPage(1)
              }}
              placeholder="Search experiments..."
              type="search"
              value={search}
            />
          </label>
          <button
            className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90"
            onClick={() => setIsCreateOpen(true)}
            type="button"
          >
            + Add Experiment
          </button>
        </div>
      </div>

      <div className="mt-6">
        {!canListExperiments ? (
          <SectionState message="You do not have permission to view experiments." />
        ) : experimentsQuery.isLoading ? (
          <TableSkeleton />
        ) : experimentsQuery.error ? (
          <SectionState
            message={getErrorMessage(
              experimentsQuery.error,
              'Unable to load experiments. Please try again.',
            )}
            tone="error"
          />
        ) : visibleExperiments.length ? (
          <>
            <div className="overflow-x-auto rounded-lg border border-slate-200">
              <table className="min-w-full divide-y divide-slate-200 text-left">
                <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="px-5 py-3 font-semibold">
                      Experiment
                    </th>
                    <th className="px-5 py-3 text-center font-semibold">
                      Experiment Type
                    </th>
                    <th className="px-5 py-3 text-center font-semibold">
                      Runs
                    </th>
                    <th className="px-5 py-3 text-center font-semibold">
                      Configs
                    </th>
                    <th className="px-5 py-3 font-semibold">Created</th>
                    <th className="px-5 py-3 text-right font-semibold">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 bg-white">
                  {visibleExperiments.map((experiment) => (
                    <tr key={experiment.experimentUuid}>
                      <td className="min-w-72 px-5 py-4">
                        <p className="text-sm font-semibold text-secondary">
                          {experiment.name}
                        </p>
                        <p className="mt-1 line-clamp-1 text-xs text-slate-500">
                          {experiment.description || 'No description provided.'}
                        </p>
                      </td>
                      <td className="px-5 py-4 text-center text-sm text-secondary">
                        {experiment.experimentType}
                      </td>
                      <td className="px-5 py-4 text-center text-sm text-secondary">
                        {experiment.runCount}
                      </td>
                      <td className="px-5 py-4 text-center text-sm text-secondary">
                        {experiment.configCount}
                      </td>
                      <td className="whitespace-nowrap px-5 py-4 text-sm text-slate-500">
                        {formatDate(experiment.createdAt)}
                      </td>
                      <td className="whitespace-nowrap px-5 py-4 text-right">
                        <span className="inline-flex gap-2">
                          <ExperimentActions
                            experimentUuid={experiment.experimentUuid}
                            projectUuid={projectQuery.data.projectUuid}
                          />
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="mt-5">
              <Pagination
                onPageChange={setPage}
                page={activePage}
                totalPages={totalPages}
              />
            </div>
          </>
        ) : (
          <SectionState
            message={
              search
                ? 'No experiments match your search.'
                : 'No experiments have been created yet.'
            }
          />
        )}
      </div>

      {isCreateOpen && (
        <CreateExperimentDialog
          isPending={createExperimentMutation.isPending}
          onClose={() => setIsCreateOpen(false)}
          onSubmit={(input) => {
            createExperimentMutation.mutate(input, {
              onError: (error) => {
                Toaster.error(
                  error instanceof Error
                    ? error.message
                    : 'Unable to create the experiment.',
                )
              },
              onSuccess: () => {
                setIsCreateOpen(false)
                Toaster.success('Experiment created successfully.')
              },
            })
          }}
        />
      )}
    </ProjectFrame>
  )
}

function CreateExperimentDialog({
  isPending,
  onClose,
  onSubmit,
}: {
  isPending: boolean
  onClose: () => void
  onSubmit: (input: {
    description: string
    experimentType: string
    name: string
  }) => void
}) {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [experimentType, setExperimentType] = useState('')

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    onSubmit({
      description: description.trim(),
      experimentType: experimentType.trim(),
      name: name.trim(),
    })
  }

  return (
    <div
      aria-labelledby="create-experiment-title"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-secondary/40 p-4"
      role="dialog"
    >
      <div className="w-full max-w-lg rounded-xl bg-white p-6 shadow-2xl">
        <div className="flex items-center justify-between gap-4">
          <h2
            className="font-heading text-2xl font-semibold text-secondary"
            id="create-experiment-title"
          >
            Add Experiment
          </h2>
          <button
            aria-label="Close add experiment dialog"
            className="rounded-md p-2 text-slate-500 hover:bg-slate-100"
            disabled={isPending}
            onClick={onClose}
            type="button"
          >
            ×
          </button>
        </div>

        <form className="mt-5 space-y-4" onSubmit={handleSubmit}>
          <label className="block text-sm font-medium text-secondary">
            Name
            <input
              autoFocus
              className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
              disabled={isPending}
              onChange={(event) => setName(event.target.value)}
              required
              value={name}
            />
          </label>
          <label className="block text-sm font-medium text-secondary">
            Description
            <textarea
              className="mt-1 min-h-24 w-full resize-y rounded-md border border-slate-300 px-3 py-2 outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
              disabled={isPending}
              onChange={(event) => setDescription(event.target.value)}
              required
              value={description}
            />
          </label>
          <label className="block text-sm font-medium text-secondary">
            Experiment type
            <input
              className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
              disabled={isPending}
              onChange={(event) => setExperimentType(event.target.value)}
              placeholder="Logging Profile Analysis"
              required
              value={experimentType}
            />
          </label>
          <div className="flex justify-end gap-3 pt-2">
            <button
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-semibold text-secondary hover:bg-slate-50"
              disabled={isPending}
              onClick={onClose}
              type="button"
            >
              Cancel
            </button>
            <button
              className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={isPending}
              type="submit"
            >
              {isPending ? 'Creating…' : 'Create experiment'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

function TableSkeleton() {
  return (
    <div className="space-y-2" role="status">
      <div className="h-12 animate-pulse rounded bg-slate-100" />
      <div className="h-20 animate-pulse rounded bg-slate-100" />
      <div className="h-20 animate-pulse rounded bg-slate-100" />
      <span className="sr-only">Loading experiments</span>
    </div>
  )
}
