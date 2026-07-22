from __future__ import annotations

from datetime import UTC, datetime
from typing import Any
from uuid import uuid4

from pydantic import Field

from experiment_runtime.base_model import ExperimentOpsModel
from experiment_runtime.logging.run_sink import ExperimentRunUserLogRecord
from experiment_runtime.models.experiment_execution_context import (
    ExperimentExecutionContext,
)


class ExperimentRunProgressEvent(ExperimentOpsModel):
    """Event emitted when an experiment run reports execution progress."""

    request_uuid: str | None
    workspace_uuid: str | None
    experiment_uuid: str | None
    experiment_run_uuid: str | None
    progress: int
    current_step: int | None = None
    sequence: int = 0
    logs: tuple[ExperimentRunUserLogRecord, ...] = ()
    event_uuid: str = Field(default_factory=lambda: str(uuid4()))
    event_timestamp: int = Field(default_factory=lambda: _current_epoch_millis())

    @classmethod
    def from_execution_context(
        cls,
        context: ExperimentExecutionContext,
        progress: int | float,
        current_step: int | None = None,
        sequence: int = 0,
        logs: list[ExperimentRunUserLogRecord] | tuple[ExperimentRunUserLogRecord, ...] = (),
    ) -> "ExperimentRunProgressEvent":
        """Create a progress event from the active experiment execution context."""

        return cls(
            request_uuid=context.request_uuid,
            workspace_uuid=context.workspace_uuid,
            experiment_uuid=context.experiment_uuid,
            experiment_run_uuid=context.experiment_run_uuid,
            progress=_progress_value(progress),
            current_step=current_step,
            sequence=sequence,
            logs=tuple(logs),
        )

    def to_payload(self) -> dict[str, Any]:
        """Serialize the progress event into the broker payload shape."""

        return {
            "metadata": {
                "traceUuid": _required_string(self.request_uuid),
                "requesterUuid": "",
                "eventType": "EXPERIMENT_RUN_PROGRESS",
                "eventUuid": self.event_uuid,
                "eventTimestamp": self.event_timestamp,
                "uuid": _required_string(self.experiment_run_uuid),
                "workspaceUuid": _required_string(self.workspace_uuid),
                "requestTimestamp": 0,
                "internalTraceUuid": None,
                "className": self.__class__.__name__,
                "triggeredBy": None,
                "userRole": None,
            },
            "payload": {
                "experimentUuid": self.experiment_uuid,
                "experimentRunUuid": self.experiment_run_uuid,
                "progress": _progress_payload(self.progress),
                "currentStep": self.current_step,
                "sequence": self.sequence,
                "logs": [log.to_payload() for log in self.logs],
            },
        }


def _current_epoch_millis() -> int:
    """Return the current UTC time as epoch milliseconds."""

    return int(datetime.now(UTC).timestamp() * 1000)


def _required_string(value: str | None) -> str:
    """Convert optional metadata values to the required empty-string fallback."""

    return value or ""


def _progress_payload(progress: int | float) -> str:
    return str(_progress_value(progress))


def _progress_value(progress: int | float) -> int:
    return round(max(0, min(float(progress), 100)))
