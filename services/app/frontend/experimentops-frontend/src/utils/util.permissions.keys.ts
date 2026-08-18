const PERMISSIONS_KEYS = {
  DATASET: {
    ADD_DATASET: ':dataset:add',
    GET_DATASET: ':dataset:get',
  },
  EXPERIMENT: {
    GET_EXPERIMENT: ':experiment:get',
  },
  EXPERIMENT_RUN: {
    COMPARE_EXPERIMENT_RUNS: ':compare:experiment:run',
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
  USER: {
    ADD_USER: ':user:add',
    GET_USER: ':user:get',
  },
} as const

export default PERMISSIONS_KEYS
