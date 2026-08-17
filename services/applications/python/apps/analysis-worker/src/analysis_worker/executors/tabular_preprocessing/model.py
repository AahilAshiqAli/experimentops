# Purpose: Define tabular preprocessing configuration, metrics, and report schemas.

from __future__ import annotations

from typing import Literal

from pydantic import ConfigDict, Field, model_validator

from experiment_runtime.base_model import ExperimentOpsModel
from experiment_runtime.models.experiment_run_completed_event import Metric


class TabularPreprocessingConfig(ExperimentOpsModel):
    model_config = ConfigDict(extra="forbid")

    target_column: str = Field(min_length=1)
    numerical_columns: list[str] = Field(default_factory=list)
    categorical_columns: list[str] = Field(default_factory=list)
    numerical_imputation: Literal["MEDIAN"] = "MEDIAN"
    categorical_imputation: Literal["MOST_FREQUENT"] = "MOST_FREQUENT"
    numerical_scaling: Literal["STANDARD"] = "STANDARD"
    categorical_encoding: Literal["ONE_HOT"] = "ONE_HOT"
    unknown_category_handling: Literal["IGNORE"] = "IGNORE"

    @model_validator(mode="after")
    def validate_feature_lists(self) -> "TabularPreprocessingConfig":
        all_features = self.numerical_columns + self.categorical_columns
        if not all_features:
            raise ValueError(
                "At least one numerical or categorical feature is required"
            )
        if any(not value.strip() for value in all_features):
            raise ValueError("Feature column names must be non-empty")
        if len(all_features) != len(set(all_features)):
            raise ValueError("Feature columns must be unique across both feature lists")
        if self.target_column in all_features:
            raise ValueError("targetColumn cannot also be a feature column")
        return self


class TabularPreprocessingMetrics(Metric):
    train_rows: int
    test_rows: int
    input_feature_count: int
    output_feature_count: int
    target_column: str
    numerical_columns: list[str]
    categorical_columns: list[str]
    output_feature_names: list[str]


class TabularPreprocessingReport(ExperimentOpsModel):
    schema_version: int = 1
    metrics: TabularPreprocessingMetrics
