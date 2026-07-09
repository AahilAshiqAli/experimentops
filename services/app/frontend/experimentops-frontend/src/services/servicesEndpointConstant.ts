export const ServicesUrlEndpoints = {
  AUTH_LOGIN: '/auth/login/:workspaceName',
  GET_DATASET: '/project/:projectUuid/dataset/:datasetUuid',
  GET_DATASET_VERSION: '/dataset/:datasetUuid/version/:datasetVersionUuid',
  GET_EXPERIMENT_CONFIGS: '/experiment/:experimentUuid/experiment-config',
  CREATE_EXPERIMENT_RUN: '/experiment/:experimentUuid/experiment-run/',
  GET_EXPERIMENT_TYPES: '/experiment-type',
  UPDATE_EXPERIMENT_CONFIG:
    '/experiment/:experimentUuid/experiment-config/:uuid',
  GET_PROJECT: '/project/:projectUuid',
  GET_PROJECT_DATASETS: '/project/:projectUuid/dataset',
  GET_PROJECT_EXPERIMENTS: '/project/:projectUuid/experiment',
  GET_PROJECTS: '/project',
  GET_ROLES: '/role',
  INITIATE_DATASET_VERSION_UPLOAD: '/dataset/:datasetUuid/version',
  UPDATE_DATASET_VERSION_STATUS:
    '/dataset/:datasetUuid/version/:datasetVersionUuid',
} as const
