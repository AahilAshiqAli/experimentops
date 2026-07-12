const PERMISSIONS_KEYS = {
  DATASET: {
    ADD_DATASET: ':dataset:add',
    GET_DATASET: ':dataset:get',
  },
  EXPERIMENT: {
    GET_EXPERIMENT: ':experiment:get',
  },
  EXPERIMENT_RUN: {
    GET_EXPERIMENT_RUNS: ':experiment:run:get',
  },
  EXPERIMENT_CONFIG: {
    ADD_EXPERIMENT_CONFIG: ':experiment-config:add',
    GET_EXPERIMENT_CONFIG: ':experiment-config:get',
  },
  EXPERIMENT_TYPE: {
    GET_EXPERIMENT_TYPE: ':experiment-type:get',
  },
  PROJECT: {
    GET_PROJECT: ':project:get',
  },
} as const

export default PERMISSIONS_KEYS
