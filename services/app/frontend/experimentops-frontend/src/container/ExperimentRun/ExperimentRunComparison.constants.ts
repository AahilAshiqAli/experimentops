export const COMPARISON_METRIC_KEYS = [
  'accuracy',
  'precision',
  'recall',
  'f1',
  'rocAuc',
] as const

export const COMPARISON_EVALUATION_CONTEXT_KEYS = [
  'algorithm',
  'problemType',
  'targetColumn',
  'rowsEvaluated',
  'decisionThreshold',
] as const

export const CONFUSION_MATRIX_KEYS = [
  'trueNegative',
  'falsePositive',
  'falseNegative',
  'truePositive',
] as const
