from __future__ import annotations

from typing import Any

from experiment_runtime.base_model import ExperimentOpsModel
from experiment_runtime.models.experiment_run_requested_event import (
    ExperimentRunExecutionConfig,
    ExperimentRunRequestedEvent,
)


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
