import { useDocumentTitle } from '../../hooks'
import {
  PageSkeleton,
  PageState,
  ProjectFrame,
} from '../ProjectDetails/projectDetails.shared'
import { getErrorMessage } from '../ProjectDetails/projectDetails.utils'
import { ExperimentRunsPanel } from './ExperimentRunsPanel'
import { useExperimentRunsContainer } from './useExperimentRunsContainer'

export function ExperimentRuns() {
  const { handleAddRun, projectQuery } = useExperimentRunsContainer()

  useDocumentTitle('Experiment runs')

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
      <ExperimentRunsPanel onAdd={handleAddRun} />
    </ProjectFrame>
  )
}
