import { Link, useParams } from 'react-router-dom'

import { useDocumentTitle } from '../../hooks'
import { useQueryProject } from '../../queries'
import {
  PageSkeleton,
  PageState,
  ProjectFrame,
} from '../ProjectDetails/projectDetails.shared'
import { getErrorMessage } from '../ProjectDetails/projectDetails.utils'

export function ExperimentConfigs() {
  return <ExperimentResource resource="configs" />
}

export function ExperimentRuns() {
  return <ExperimentResource resource="runs" />
}

function ExperimentResource({ resource }: { resource: 'configs' | 'runs' }) {
  const { experimentUuid, projectUuid } = useParams<{
    experimentUuid: string
    projectUuid: string
  }>()
  const projectQuery = useQueryProject(projectUuid)
  const title =
    resource === 'configs' ? 'Experiment configs' : 'Experiment runs'

  useDocumentTitle(title)

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
      <Link
        className="text-sm font-semibold text-primary hover:underline"
        to={`/projects/${projectQuery.data.projectUuid}/experiments`}
      >
        ← Back to experiments
      </Link>
      <h2 className="mt-5 font-heading text-2xl font-semibold text-secondary">
        {title}
      </h2>
      <p className="mt-2 text-sm text-slate-600">
        Experiment:{' '}
        <span className="font-medium text-secondary">{experimentUuid}</span>
      </p>
      <div className="mt-6 rounded-lg border border-dashed border-slate-300 px-6 py-12 text-center text-sm text-slate-600">
        The {resource} API and content will be connected here next.
      </div>
    </ProjectFrame>
  )
}
