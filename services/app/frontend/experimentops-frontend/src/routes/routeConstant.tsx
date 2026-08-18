import type { RouteObject } from 'react-router-dom'

import { AdminLayout } from '../container/AdminLayout/AdminLayout'
import { Dashboard } from '../container/Dashboard/Dashboard'
import { DatasetDetails } from '../container/DatasetDetails/DatasetDetails'
import {
  CreateExperimentRun,
  ExperimentConfigs,
  ExperimentRunComparison,
  ExperimentRunDetails,
  ExperimentRuns,
} from '../container/ExperimentResources/ExperimentResources'
import { Login } from '../container/Login/Login'
import { ProjectDetails } from '../container/ProjectDetails/ProjectDetails'
import { ProjectDatasets } from '../container/ProjectDatasets/ProjectDatasets'
import { ProjectExperiments } from '../container/ProjectExperiments/ProjectExperiments'
import { Projects } from '../container/Projects/Projects'
import { ResetPassword } from '../container/ResetPassword/ResetPassword'
import { Users } from '../container/Users/Users'

export const unAuthenticatedRoutesConstant = {
  LOGIN: '/login',
  RESET_PASSWORD: '/reset-password',
} as const

export const authenticatedRoutesConstant = {
  HOME: '/',
  DASHBOARD: '/dashboard',
  PROJECTS: '/projects',
  USERS: '/users',
  PROJECT_DETAILS: '/projects/:projectUuid',
  PROJECT_DATASETS: '/projects/:projectUuid/datasets',
  PROJECT_DATASET_DETAILS: '/projects/:projectUuid/datasets/:datasetUuid',
  PROJECT_EXPERIMENTS: '/projects/:projectUuid/experiments',
  EXPERIMENT_CONFIGS:
    '/projects/:projectUuid/experiments/:experimentUuid/experiment-configs',
  EXPERIMENT_RUNS:
    '/projects/:projectUuid/experiments/:experimentUuid/experiment-runs',
  EXPERIMENT_RUN_CREATE:
    '/projects/:projectUuid/experiments/:experimentUuid/experiment-runs/new',
  EXPERIMENT_RUN_COMPARE:
    '/projects/:projectUuid/experiments/:experimentUuid/experiment-runs/compare',
  EXPERIMENT_RUN_DETAILS:
    '/projects/:projectUuid/experiments/:experimentUuid/experiment-runs/:experimentRunUuid',
} as const

export const UNAUTHENTICATED_ROUTES: RouteObject[] = [
  {
    path: unAuthenticatedRoutesConstant.LOGIN,
    element: <Login />,
  },
  {
    path: unAuthenticatedRoutesConstant.RESET_PASSWORD,
    element: <ResetPassword />,
  },
]

export const AUTHENTICATED_ROUTES: RouteObject[] = [
  {
    path: unAuthenticatedRoutesConstant.RESET_PASSWORD,
    element: <ResetPassword />,
  },
  {
    path: authenticatedRoutesConstant.HOME,
    element: <AdminLayout />,
    children: [
      {
        index: true,
        element: <Dashboard />,
      },
      {
        path: authenticatedRoutesConstant.DASHBOARD,
        element: <Dashboard />,
      },
      {
        path: authenticatedRoutesConstant.PROJECTS,
        element: <Projects />,
      },
      {
        path: authenticatedRoutesConstant.USERS,
        element: <Users />,
      },
      {
        path: authenticatedRoutesConstant.PROJECT_DETAILS,
        element: <ProjectDetails />,
      },
      {
        path: authenticatedRoutesConstant.PROJECT_EXPERIMENTS,
        element: <ProjectExperiments />,
      },
      {
        path: authenticatedRoutesConstant.EXPERIMENT_CONFIGS,
        element: <ExperimentConfigs />,
      },
      {
        path: authenticatedRoutesConstant.EXPERIMENT_RUN_CREATE,
        element: <CreateExperimentRun />,
      },
      {
        path: authenticatedRoutesConstant.EXPERIMENT_RUN_COMPARE,
        element: <ExperimentRunComparison />,
      },
      {
        path: authenticatedRoutesConstant.EXPERIMENT_RUN_DETAILS,
        element: <ExperimentRunDetails />,
      },
      {
        path: authenticatedRoutesConstant.EXPERIMENT_RUNS,
        element: <ExperimentRuns />,
      },
      {
        path: authenticatedRoutesConstant.PROJECT_DATASETS,
        element: <ProjectDatasets />,
      },
      {
        path: authenticatedRoutesConstant.PROJECT_DATASET_DETAILS,
        element: <DatasetDetails />,
      },
    ],
  },
]
