from __future__ import annotations

from typing import Any

from pydantic import Field, SerializeAsAny

from experiment_runtime.base_model import ExperimentOpsModel
from experiment_runtime.models.experiment_run_completed_event import Result
from experiment_runtime.models.experiment_run_requested_event import (
    ExperimentRunExecutionConfig,
    ExperimentRunRequestedEvent,
)

""" Used duck type serialization which enables to get the fields defined in classes overridden by Result."""
class ExperimentExecutionContext(ExperimentOpsModel):
    event_uuid: str | None
    request_uuid: str | None
    workspace_uuid: str | None
    project_uuid: str | None
    experiment_uuid: str | None
    experiment_run_uuid: str | None
    experiment_type: str | None
    dataset_uri: str | None
    config_json: Any | None
    previous_result: SerializeAsAny[Result] | None = Field(default=None, exclude=True)
    pipeline_results: list[SerializeAsAny[Result]] = Field(
        default_factory=list,
        exclude=True,
    )
    progress_completed_weight: float | None = Field(default=None, exclude=True)
    progress_step_weight: float | None = Field(default=None, exclude=True)
    progress_total_weight: float | None = Field(default=None, exclude=True)

    @classmethod
    def from_requested_event(
        cls,
        event: ExperimentRunRequestedEvent,
        execution_config: ExperimentRunExecutionConfig | None = None,
    ) -> "ExperimentExecutionContext":
        return cls(
            event_uuid=event.event_uuid,
            request_uuid=event.request_uuid,
            workspace_uuid=event.workspace_uuid,
            project_uuid=event.project_uuid,
            experiment_uuid=event.experiment_uuid,
            experiment_run_uuid=event.experiment_run_uuid,
            experiment_type=(
                execution_config.experiment_type
                if execution_config is not None
                else event.experiment_type
            ),
            dataset_uri=event.dataset_uri,
            config_json=(
                execution_config.experiment_config_json
                if execution_config is not None
                else event.config_json
            ),
        )
