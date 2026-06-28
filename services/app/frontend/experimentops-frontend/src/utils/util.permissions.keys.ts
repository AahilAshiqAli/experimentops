const PERMISSIONS_KEYS = {
  DATASET: {
    GET_DATASET: ':dataset:get',
  },
  EXPERIMENT: {
    GET_EXPERIMENT: ':experiment:get',
  },
  PROJECT: {
    GET_PROJECT: ':project:get',
  },
} as const

export default PERMISSIONS_KEYS
