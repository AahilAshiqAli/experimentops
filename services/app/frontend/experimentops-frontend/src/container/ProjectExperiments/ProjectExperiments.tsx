import { useState, type FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import { DataTable } from '../../components/DataTable'
import { TableSkeleton } from '../../components/TableSkeleton'
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
  PageSkeleton,
  PageState,
  ProjectFrame,
  SectionState,
} from '../ProjectDetails/projectDetails.shared'
import { getErrorMessage } from '../ProjectDetails/projectDetails.utils'
import { getExperimentColumns } from './columns'

const EXPERIMENTS_PER_PAGE = 8

export function ProjectExperiments() {
  const { i18n, t } = useTranslation()
  const language = i18n.resolvedLanguage ?? i18n.language
  const { projectUuid } = useParams<{ projectUuid: string }>()
  const { hasPermission } = useLogin()
  const projectQuery = useQueryProject(projectUuid)
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
  const totalExperiments = experimentsQuery.data?.totalElements ?? 0
  const totalPages = Math.max(
    1,
    Math.ceil(totalExperiments / EXPERIMENTS_PER_PAGE),
  )
  const activePage = Math.min(page, totalPages)

  useDocumentTitle(
    projectQuery.data
      ? t('experiments.titleWithProject', { project: projectQuery.data.name })
      : t('experiments.title'),
  )

  if (projectQuery.isLoading) return <PageSkeleton />
  if (projectQuery.error) {
    return (
      <PageState
        message={getErrorMessage(
          projectQuery.error,
          t('datasets.errors.loadProject'),
        )}
        title={t('project.errors.loadTitle')}
        tone="error"
      />
    )
  }
  if (!projectQuery.data) {
    return (
      <PageState
        message={t('project.notAvailable')}
        title={t('project.errors.notFoundTitle')}
      />
    )
  }

  return (
    <ProjectFrame activeTab="experiments" project={projectQuery.data}>
      <div className="mb-2">
        <h2 className="font-heading text-xl font-semibold text-secondary">
          {t('experiments.title')}
        </h2>
      </div>
      <div>
        {!canListExperiments ? (
          <SectionState message={t('experiments.permissionDenied')} />
        ) : experimentsQuery.isLoading ? (
          <TableSkeleton />
        ) : experimentsQuery.error ? (
          <SectionState
            message={getErrorMessage(
              experimentsQuery.error,
              t('experiments.errors.load'),
            )}
            tone="error"
          />
        ) : (
          <DataTable
            columns={getExperimentColumns(
              projectQuery.data.projectUuid,
              t,
              language,
            )}
            data={experimentsQuery.data?.data ?? []}
            emptyMessage={t('experiments.empty')}
            getRowKey={(experiment) => experiment.experimentUuid}
            pagination={{
              onPageChange: setPage,
              page: activePage,
              totalItems: totalExperiments,
              totalPages,
            }}
            searchPlaceholder={t('experiments.search')}
            toolbarEnd={
              <button
                className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90"
                onClick={() => setIsCreateOpen(true)}
                type="button"
              >
                + {t('experiments.add')}
              </button>
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
                    : t('experiments.errors.create'),
                )
              },
              onSuccess: () => {
                setIsCreateOpen(false)
                Toaster.success(t('experiments.created'))
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
  onSubmit: (input: { description: string; name: string }) => void
}) {
  const { t } = useTranslation()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    onSubmit({
      description: description.trim(),
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
            {t('experiments.add')}
          </h2>
          <button
            aria-label={t('experiments.closeDialog')}
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
            {t('experiments.name')}
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
            {t('experiments.description')}
            <textarea
              className="mt-1 min-h-24 w-full resize-y rounded-md border border-slate-300 px-3 py-2 outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
              disabled={isPending}
              onChange={(event) => setDescription(event.target.value)}
              required
              value={description}
            />
          </label>
          <div className="flex justify-end gap-3 pt-2">
            <button
              className="rounded-md border border-slate-300 px-4 py-2 text-sm font-semibold text-secondary hover:bg-slate-50"
              disabled={isPending}
              onClick={onClose}
              type="button"
            >
              {t('common.actions.cancel')}
            </button>
            <button
              className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={isPending}
              type="submit"
            >
              {isPending ? t('experiments.creating') : t('experiments.create')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
