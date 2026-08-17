# Purpose: Define train/test split configuration, metrics, and report schemas.

from __future__ import annotations

from pydantic import ConfigDict, Field

from experiment_runtime.base_model import ExperimentOpsModel
from experiment_runtime.models.experiment_run_completed_event import Metric


class TabularTrainTestSplitConfig(ExperimentOpsModel):
    model_config = ConfigDict(extra="forbid")

    target_column: str = Field(min_length=1)
    test_size: float = Field(default=0.2, ge=0.1, le=0.5)
    random_seed: int = Field(default=42, ge=0, le=2_147_483_647)
    stratify: bool = True


class TabularTrainTestSplitMetrics(Metric):
    source_rows: int
    train_rows: int
    test_rows: int
    target_column: str
    test_size: float
    random_seed: int
    stratified: bool
    class_distribution: dict[str, dict[str, int]]


class TabularTrainTestSplitReport(ExperimentOpsModel):
    schema_version: int = 1
    metrics: TabularTrainTestSplitMetrics
