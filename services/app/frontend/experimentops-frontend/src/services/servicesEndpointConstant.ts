export const ServicesUrlEndpoints = {
  AUTH_LOGIN: '/auth/login/:workspaceName',
  GET_DATASET: '/dataset/:datasetUuid',
  GET_PROJECT: '/project/:projectUuid',
  GET_PROJECT_DATASETS: '/project/:projectUuid/dataset',
  GET_PROJECT_EXPERIMENTS: '/project/:projectUuid/experiment',
  GET_PROJECTS: '/project',
  GET_ROLES: '/role',
} as const
