# Purpose: Define evaluation configuration, metrics, and persisted report schemas.

from __future__ import annotations

from typing import Literal

from pydantic import ConfigDict, Field

from experiment_runtime.base_model import ExperimentOpsModel
from experiment_runtime.models.experiment_run_completed_event import Metric


class BinaryClassificationEvaluationConfig(ExperimentOpsModel):
    model_config = ConfigDict(extra="forbid")

    decision_threshold: float = Field(default=0.5, gt=0, lt=1)


class BinaryClassificationEvaluationMetrics(Metric):
    problem_type: Literal["BINARY_CLASSIFICATION"] = "BINARY_CLASSIFICATION"
    algorithm: Literal["LOGISTIC_REGRESSION"] = "LOGISTIC_REGRESSION"
    target_column: str
    rows_evaluated: int
    decision_threshold: float
    accuracy: float
    precision: float
    recall: float
    f1: float
    roc_auc: float
    true_negative: int
    false_positive: int
    false_negative: int
    true_positive: int


class BinaryClassificationEvaluationReport(ExperimentOpsModel):
    schema_version: int = 1
    metrics: BinaryClassificationEvaluationMetrics
