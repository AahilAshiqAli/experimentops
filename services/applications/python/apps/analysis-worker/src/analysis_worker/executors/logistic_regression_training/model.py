# Purpose: Define logistic-regression configuration, training metrics, and report schemas.

from __future__ import annotations

from typing import Literal

from pydantic import ConfigDict, Field

from experiment_runtime.base_model import ExperimentOpsModel
from experiment_runtime.models.experiment_run_completed_event import Metric


class LogisticRegressionTrainingConfig(ExperimentOpsModel):
    model_config = ConfigDict(extra="forbid")

    target_column: str = Field(min_length=1)
    c: float = Field(default=1.0, gt=0, le=1_000_000)
    max_iter: int = Field(default=1_000, ge=1, le=10_000)
    class_weight: Literal["NONE", "BALANCED"] = "NONE"
    random_seed: int = Field(default=42, ge=0, le=2_147_483_647)


class LogisticRegressionTrainingMetrics(Metric):
    algorithm: Literal["LOGISTIC_REGRESSION"] = "LOGISTIC_REGRESSION"
    problem_type: Literal["BINARY_CLASSIFICATION"] = "BINARY_CLASSIFICATION"
    target_column: str
    training_rows: int
    feature_count: int
    feature_names: list[str]
    classes: list[int]
    iterations: int
    hyperparameters: dict[str, object]


class LogisticRegressionTrainingReport(ExperimentOpsModel):
    schema_version: int = 1
    metrics: LogisticRegressionTrainingMetrics
