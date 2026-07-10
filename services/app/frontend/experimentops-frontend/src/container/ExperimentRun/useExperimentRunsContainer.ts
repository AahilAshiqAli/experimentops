import { useNavigate, useParams } from 'react-router-dom'

import { useQueryProject } from '../../queries'

export function useExperimentRunsContainer() {
  const { experimentUuid, projectUuid } = useParams<{
    experimentUuid: string
    projectUuid: string
  }>()
  const navigate = useNavigate()
  const projectQuery = useQueryProject(projectUuid)

  const handleAddRun = () => {
    navigate(
      `/projects/${projectUuid}/experiments/${experimentUuid}/experiment-runs/new`,
    )
  }

  return {
    handleAddRun,
    projectQuery,
  }
}
