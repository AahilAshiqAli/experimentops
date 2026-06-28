import { Link, useParams } from 'react-router-dom'

import { useLogin } from '../../context-api/logincontext'
import { useDocumentTitle } from '../../hooks'
import {
  useQueryProject,
  useQueryProjectDatasets,
  useQueryProjectExperiments,
} from '../../queries'
import { PERMISSIONS_KEYS } from '../../utils'
import {
  DatasetFolderCard,
  ExperimentPreview,
  PageSkeleton,
  PageState,
  ProjectFrame,
  SectionState,
} from './projectDetails.shared'
import { getErrorMessage } from './projectDetails.utils'

export function ProjectDetails() {
  const { projectUuid } = useParams<{ projectUuid: string }>()
  const { hasPermission } = useLogin()
  const projectQuery = useQueryProject(projectUuid)
  const datasetsQuery = useQueryProjectDatasets(projectUuid, {
    page: 0,
    size: 4,
  })
  const experimentsQuery = useQueryProjectExperiments(projectUuid, {
    page: 0,
    size: 3,
  })
  const project = projectQuery.data
  const canListDatasets = hasPermission(PERMISSIONS_KEYS.DATASET.GET_DATASET)
  const canListExperiments = hasPermission(
    PERMISSIONS_KEYS.EXPERIMENT.GET_EXPERIMENT,
  )

  useDocumentTitle(project?.name ?? 'Project overview')

  if (projectQuery.isLoading) return <PageSkeleton />
  if (projectQuery.error) {
    return (
      <PageState
        message={getErrorMessage(
          projectQuery.error,
          'Unable to load this project. Please try again.',
        )}
        title="Unable to load project"
        tone="error"
      />
    )
  }
  if (!project) {
    return (
      <PageState
        message="This project is not available."
        title="Project not found"
      />
    )
  }

  const metrics = [
    { label: 'Dataset folders', value: project.datasetCount, icon: 'folder' },
    {
      label: 'Dataset versions',
      value: project.datasetVersionCount,
      icon: 'database',
    },
    { label: 'Experiments', value: project.experimentCount, icon: 'flask' },
    {
      label: 'Configs',
      value: project.experimentConfigCount,
      icon: 'settings',
    },
    { label: 'Runs', value: project.experimentRunCount, icon: 'play' },
  ] as const

  return (
    <ProjectFrame activeTab="overview" project={project}>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        {metrics.map((metric) => (
          <MetricCard {...metric} key={metric.label} />
        ))}
      </div>

      <div className="mt-8 grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
        <PreviewPanel
          link={`/projects/${project.projectUuid}/experiments`}
          title="Recent experiments"
        >
          {!canListExperiments ? (
            <SectionState message="You do not have permission to view experiments." />
          ) : experimentsQuery.isLoading ? (
            <PreviewSkeleton />
          ) : experimentsQuery.error ? (
            <SectionState
              message={getErrorMessage(
                experimentsQuery.error,
                'Unable to load experiments.',
              )}
              tone="error"
            />
          ) : experimentsQuery.data?.data.length ? (
            experimentsQuery.data.data
              .map((experiment) => (
                <ExperimentPreview
                  experiment={experiment}
                  key={experiment.experimentUuid}
                  projectUuid={project.projectUuid}
                />
              ))
          ) : (
            <SectionState message="No experiments have been created yet." />
          )}
        </PreviewPanel>

        <PreviewPanel
          link={`/projects/${project.projectUuid}/datasets`}
          title="Dataset folders"
        >
          {!canListDatasets ? (
            <SectionState message="You do not have permission to view datasets." />
          ) : datasetsQuery.isLoading ? (
            <PreviewSkeleton />
          ) : datasetsQuery.error ? (
            <SectionState
              message={getErrorMessage(
                datasetsQuery.error,
                'Unable to load dataset folders.',
              )}
              tone="error"
            />
          ) : datasetsQuery.data?.data.length ? (
            <div className="grid gap-3 p-4 sm:grid-cols-2 lg:grid-cols-1 xl:grid-cols-2">
              {datasetsQuery.data.data.map((dataset) => (
                <DatasetFolderCard
                  dataset={dataset}
                  key={dataset.datasetUuid}
                  projectUuid={project.projectUuid}
                />
              ))}
            </div>
          ) : (
            <div className="p-4">
              <SectionState message="No dataset folders have been created yet." />
            </div>
          )}
        </PreviewPanel>
      </div>
    </ProjectFrame>
  )
}

function MetricCard({
  icon,
  label,
  value,
}: {
  icon: 'database' | 'flask' | 'folder' | 'play' | 'settings'
  label: string
  value: number
}) {
  const iconPath = {
    database:
      'M5 6c0-1.1 3.1-2 7-2s7 .9 7 2-3.1 2-7 2-7-.9-7-2Zm0 0v6c0 1.1 3.1 2 7 2s7-.9 7-2V6m-14 6v6c0 1.1 3.1 2 7 2s7-.9 7-2v-6',
    flask:
      'M9 3h6m-5 0v6l-5 9a2 2 0 0 0 1.7 3h10.6a2 2 0 0 0 1.7-3l-5-9V3M8 15h8',
    folder:
      'M3 6.5A2.5 2.5 0 0 1 5.5 4h4l2 2h7A2.5 2.5 0 0 1 21 8.5v9a2.5 2.5 0 0 1-2.5 2.5h-13A2.5 2.5 0 0 1 3 17.5v-11Z',
    play: 'm9 7 8 5-8 5V7Z',
    settings:
      'M12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Zm7-3.5 2-1-2-4-2.2.7a8 8 0 0 0-1.3-.8L15 4h-6l-.5 2.9a8 8 0 0 0-1.3.8L5 7l-2 4 2 1-2 1 2 4 2.2-.7a8 8 0 0 0 1.3.8L9 20h6l.5-2.9a8 8 0 0 0 1.3-.8L19 17l2-4-2-1Z',
  }[icon]

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5 text-center shadow-sm">
      <svg
        aria-hidden="true"
        className="mx-auto h-7 w-7 text-primary"
        fill="none"
        viewBox="0 0 24 24"
      >
        <path
          d={iconPath}
          stroke="currentColor"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.8"
        />
      </svg>
      <p className="mt-3 text-2xl font-bold text-secondary">{value}</p>
      <p className="mt-1 text-xs font-medium text-slate-500">{label}</p>
    </div>
  )
}

function PreviewPanel({
  children,
  link,
  title,
}: {
  children: React.ReactNode
  link: string
  title: string
}) {
  return (
    <section className="overflow-hidden rounded-xl border border-slate-200 bg-white">
      <header className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
        <h2 className="font-heading text-xl font-semibold text-secondary">
          {title}
        </h2>
        <Link
          className="text-sm font-semibold text-primary hover:underline"
          to={link}
        >
          View all
        </Link>
      </header>
      {children}
    </section>
  )
}

function PreviewSkeleton() {
  return (
    <div className="space-y-3 p-4" role="status">
      <div className="h-24 animate-pulse rounded-lg bg-slate-100" />
      <div className="h-24 animate-pulse rounded-lg bg-slate-100" />
      <span className="sr-only">Loading preview</span>
    </div>
  )
}
