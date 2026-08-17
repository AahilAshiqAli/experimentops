# Purpose: Expose the logistic-regression training executor for registry discovery.

from analysis_worker.executors.logistic_regression_training.executor import (
    LogisticRegressionTrainingExecutor,
)

__all__ = ["LogisticRegressionTrainingExecutor"]
