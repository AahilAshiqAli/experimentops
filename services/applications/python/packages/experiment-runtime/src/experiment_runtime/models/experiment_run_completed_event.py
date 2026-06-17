from __future__ import annotations

import json
from dataclasses import dataclass, field
from datetime import UTC, datetime
from typing import Any, Mapping
from uuid import uuid4

from experiment_runtime.models.experiment_run_requested_event import (
    ExperimentRunRequestedEvent,
)


@dataclass(frozen=True)
class ExperimentRunCompletedEvent:
    request_uuid: str | None
    requester_uuid: str | None
    workspace_uuid: str | None
    project_uuid: str | None
    experiment_uuid: str | None
    experiment_run_uuid: str | None
    experiment_type: str | None
    status: str
    result: Mapping[str, Any] | None = field(default=None)
    request_timestamp: int | None = None
    user_role: str | None = None
    event_uuid: str = field(default_factory=lambda: str(uuid4()))
    event_timestamp: int = field(default_factory=lambda: _current_epoch_millis())

    @classmethod
    def from_requested_event(
        cls,
        event: ExperimentRunRequestedEvent,
        result: Mapping[str, Any] | None,
    ) -> "ExperimentRunCompletedEvent":
        status = "SUCCEEDED"

        if result and isinstance(result.get("status"), str):
            status = result["status"]

        return cls(
            request_uuid=event.request_uuid,
            requester_uuid=event.requester_uuid,
            workspace_uuid=event.workspace_uuid,
            project_uuid=event.project_uuid,
            experiment_uuid=event.experiment_uuid,
            experiment_run_uuid=event.experiment_run_uuid,
            experiment_type=event.experiment_type,
            status=status,
            result=result,
            request_timestamp=event.request_timestamp,
            user_role=event.user_role,
        )

    def to_payload(self) -> dict[str, Any]:
        return {
            "metadata": {
                "traceUuid": _required_string(self.request_uuid),
                "requesterUuid": _required_string(self.requester_uuid),
                "eventType": "EXPERIMENT_RUN_COMPLETED",
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
                "status": self.status,
                "resultJson": (
                    json.dumps(dict(self.result), separators=(",", ":"))
                    if self.result is not None
                    else None
                ),
            },
        }


def _current_epoch_millis() -> int:
    return int(datetime.now(UTC).timestamp() * 1000)


def _required_string(value: str | None) -> str:
    return value or ""
