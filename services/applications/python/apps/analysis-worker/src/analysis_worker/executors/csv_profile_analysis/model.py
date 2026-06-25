from __future__ import annotations

from typing import Any, Literal

from pydantic import ConfigDict, Field

from experiment_runtime.base_model import ExperimentOpsModel
from experiment_runtime.models.experiment_run_completed_event import Metric, Result


class CsvCleaningConfig(ExperimentOpsModel):
    normalize_column_names: bool = True
    remove_unnamed_columns: bool = True
    trim_string_values: bool = True
    empty_strings_as_null: bool = True
    drop_fully_empty_rows: bool = True
    drop_duplicate_rows: bool = True
    required_columns: list[str] = Field(default_factory=list)


class CsvProfileAnalysisContext(ExperimentOpsModel):
    model_config = ConfigDict(extra="allow")

    workspace_uuid: str | None = None
    project_uuid: str | None = None
    experiment_uuid: str | None = None
    experiment_run_uuid: str
    dataset_uri: str
    output_dir: str | None = None
    cleaning_config: CsvCleaningConfig = Field(default_factory=CsvCleaningConfig)


class CsvCleaningMetrics(Metric):
    input_rows: int
    output_rows: int
    input_columns: int
    output_columns: int
    missing_values_before: int
    missing_values_after: int
    unnamed_columns_removed: int
    fully_empty_rows_removed: int
    rows_removed_missing_required_values: int
    duplicate_rows_removed: int
    final_columns: list[str]


class CsvCleaningOutput(Result):
    metrics: CsvCleaningMetrics
    cleaned_dataset_path: str = Field(exclude=True)
    cleaning_report_path: str = Field(exclude=True)
    experiment_run_uuid: str = Field(exclude=True)

    @property
    def cleaned_dataset_uri(self) -> str:
        return self._artifact_uri("CLEANED_DATASET")

    @property
    def cleaning_report_uri(self) -> str:
        return self._artifact_uri("CLEANING_REPORT")

    @property
    def cleaning_metrics(self) -> CsvCleaningMetrics:
        return self.metrics

    @property
    def rows_processed(self) -> int:
        return self.metrics.output_rows

    def _artifact_uri(self, artifact_type: str) -> str:
        for artifact in self.artifact:
            if artifact.type == artifact_type:
                return artifact.uri

        raise ValueError(f"Missing artifact with type={artifact_type}")


class CsvCleaningReport(ExperimentOpsModel):
    status: Literal["SUCCEEDED"]
    experiment_run_uuid: str
    metrics: CsvCleaningMetrics
    context: dict[str, Any]
