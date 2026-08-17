# Purpose: Define CSV cleaning configuration, execution context, metrics, and report models.

from __future__ import annotations

from collections.abc import Mapping
from typing import Any, Literal

from pydantic import ConfigDict, Field

from experiment_runtime.base_model import ExperimentOpsModel
from experiment_runtime.models import ExperimentExecutionContext
from experiment_runtime.models.experiment_run_completed_event import Metric, Result


class CsvCleaningConfig(ExperimentOpsModel):
    """CSV cleaning options supplied with a profile analysis request."""

    model_config = ConfigDict(extra="forbid")

    normalize_column_names: bool = True
    remove_unnamed_columns: bool = True
    trim_string_values: bool = True
    empty_strings_as_null: bool = True
    drop_fully_empty_rows: bool = True
    drop_duplicate_rows: bool = True
    required_columns: list[str] = Field(default_factory=list)


class CsvProfileAnalysisContext(ExperimentOpsModel):
    """Execution context used to locate input data and configure CSV cleaning."""

    model_config = ConfigDict(extra="allow")

    workspace_uuid: str | None = None
    project_uuid: str | None = None
    experiment_uuid: str | None = None
    experiment_run_uuid: str
    dataset_uri: str
    output_dir: str | None = None
    cleaning_config: CsvCleaningConfig = Field(default_factory=CsvCleaningConfig)

    @classmethod
    def from_execution_context(
        cls,
        context: ExperimentExecutionContext,
    ) -> "CsvProfileAnalysisContext":
        """Bind the saved step config to the cleaner's typed configuration."""

        config_json = context.config_json
        if config_json is None:
            config_json = {}
        if not isinstance(config_json, Mapping):
            raise ValueError("experimentConfigJson must be a JSON object")

        return cls(
            workspace_uuid=context.workspace_uuid,
            project_uuid=context.project_uuid,
            experiment_uuid=context.experiment_uuid,
            experiment_run_uuid=context.experiment_run_uuid,
            dataset_uri=context.dataset_uri,
            cleaning_config=CsvCleaningConfig.model_validate(config_json),
        )


class CsvCleaningMetrics(Metric):
    """Row, column, and data quality counts produced by the cleaning step."""

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
    """Completed cleaning result containing output artifacts and typed metrics."""

    metrics: CsvCleaningMetrics


class CsvCleaningReport(ExperimentOpsModel):
    """Persisted report payload for a successful CSV cleaning execution."""

    status: Literal["SUCCEEDED"]
    experiment_run_uuid: str
    metrics: CsvCleaningMetrics
    context: dict[str, Any]
