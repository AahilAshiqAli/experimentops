from __future__ import annotations

import traceback
from datetime import UTC, datetime
from typing import Any
from uuid import uuid4

from pydantic import Field

from experiment_runtime.base_model import ExperimentOpsModel
from experiment_runtime.models.experiment_run_requested_event import (
    ExperimentRunRequestedEvent,
)


class ExperimentRunFailureErrorEntry(ExperimentOpsModel):
    error_type: str
    error_message: str
    stack_trace: str | None = None

    @classmethod
    def from_exception(cls, exception: BaseException) -> "ExperimentRunFailureErrorEntry":
        return cls(
            error_type=exception.__class__.__name__,
            error_message=str(exception) or exception.__class__.__name__,
            stack_trace="".join(
                traceback.format_exception(
                    type(exception),
                    exception,
                    exception.__traceback__,
                )
            ),
        )

    def to_payload(self) -> dict[str, Any]:
        return self.model_dump(by_alias=True)


class ExperimentRunFailureEvent(ExperimentOpsModel):
    request_uuid: str | None
    requester_uuid: str | None
    workspace_uuid: str | None
    project_uuid: str | None
    experiment_uuid: str | None
    experiment_run_uuid: str | None
    experiment_type: str | None
    errors: tuple[ExperimentRunFailureErrorEntry, ...]
    request_timestamp: int | None = None
    user_role: str | None = None
    event_uuid: str = Field(default_factory=lambda: str(uuid4()))
    event_timestamp: int = Field(default_factory=lambda: _current_epoch_millis())

    @classmethod
    def from_requested_event(
        cls,
        event: ExperimentRunRequestedEvent,
        exception: BaseException,
        experiment_type: str | None = None,
    ) -> "ExperimentRunFailureEvent":
        return cls(
            request_uuid=event.request_uuid,
            requester_uuid=event.requester_uuid,
            workspace_uuid=event.workspace_uuid,
            project_uuid=event.project_uuid,
            experiment_uuid=event.experiment_uuid,
            experiment_run_uuid=event.experiment_run_uuid,
            experiment_type=experiment_type or event.experiment_type,
            errors=(ExperimentRunFailureErrorEntry.from_exception(exception),),
            request_timestamp=event.request_timestamp,
            user_role=event.user_role,
        )

    def to_payload(self) -> dict[str, Any]:
        return {
            "metadata": {
                "traceUuid": _required_string(self.request_uuid),
                "requesterUuid": _required_string(self.requester_uuid),
                "eventType": "EXPERIMENT_RUN_FAILURE",
                "eventUuid": self.event_uuid,
                "eventTimestamp": self.event_timestamp,
                "uuid": _required_string(self.experiment_run_uuid),
                "workspaceUuid": _required_string(self.workspace_uuid),
                "requestTimestamp": self.request_timestamp or 0,
                "internalTraceUuid": None,
                "className": self.__class__.__name__,
                "triggeredBy": None,
                "userRole": self.user_role,
            },
            "payload": {
                "projectUuid": self.project_uuid,
                "experimentUuid": self.experiment_uuid,
                "experimentType": self.experiment_type,
                "status": "FAILED",
                "errors": [error.to_payload() for error in self.errors],
            },
        }


def _current_epoch_millis() -> int:
    return int(datetime.now(UTC).timestamp() * 1000)


def _required_string(value: str | None) -> str:
    return value or ""
