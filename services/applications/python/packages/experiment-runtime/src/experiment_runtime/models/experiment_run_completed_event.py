from __future__ import annotations

import json
from datetime import UTC, datetime
from uuid import uuid4

from pydantic import Field, SerializeAsAny

from experiment_runtime.base_model import ExperimentOpsModel
from experiment_runtime.models.experiment_run_requested_event import (
    ExperimentRunRequestedEvent,
)


class Artifact(ExperimentOpsModel):
    """Metadata for an artifact produced by an experiment run."""

    format: str
    type: str
    uri: str
    size: int
    experiment_type: str | None = Field(default=None, exclude=True)


class Metric(ExperimentOpsModel):
    """Base model for typed metrics emitted by experiment-specific results."""

    experiment_type: str | None = Field(default=None, exclude=True)


class Result(ExperimentOpsModel):
    """Experiment result payload containing artifacts and optional metrics."""

    artifact: list[Artifact]
    metrics: list[SerializeAsAny[Metric]] | None


class ExperimentRunCompletedEvent(ExperimentOpsModel):
    """Event emitted when an experiment run completes with a final status."""

    request_uuid: str | None
    requester_uuid: str | None
    workspace_uuid: str | None
    project_uuid: str | None
    experiment_uuid: str | None
    experiment_run_uuid: str | None
    experiment_type: str | None
    status: str
    request_timestamp: int | None = None
    user_role: str | None = None
    result: Result | None
    event_uuid: str = Field(default_factory=lambda: str(uuid4()))
    event_timestamp: int = Field(default_factory=lambda: _current_epoch_millis())

    @classmethod
    def from_requested_event(
        cls,
        event: ExperimentRunRequestedEvent,
        result: Result | None,
        experiment_type: str | None = None,
    ) -> "ExperimentRunCompletedEvent":
        """Create a completed event from the original run request and result."""

        status = "SUCCEEDED"

        return cls(
            request_uuid=event.request_uuid,
            requester_uuid=event.requester_uuid,
            workspace_uuid=event.workspace_uuid,
            project_uuid=event.project_uuid,
            experiment_uuid=event.experiment_uuid,
            experiment_run_uuid=event.experiment_run_uuid,
            experiment_type=experiment_type or event.experiment_type,
            status=status,
            result=result,
            request_timestamp=event.request_timestamp,
            user_role=event.user_role,
        )

    def to_payload(self) -> dict[str, object]:
        """Serialize the completed event into the broker payload shape."""

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
                "result": _result_payload(self.result),
            },
        }


def _current_epoch_millis() -> int:
    """Return the current UTC time as epoch milliseconds."""

    return int(datetime.now(UTC).timestamp() * 1000)


def _result_payload(result: Result | None) -> dict[str, object] | None:
    """Serialize typed artifacts and experiment-specific metrics JSON."""

    if result is None:
        return None

    return {
        "artifact": [
            _artifact_payload(artifact)
            for artifact in result.artifact
        ],
        "metrics": _metrics_entries_payload(result.metrics),
        "metricsJson": _metrics_json(result.metrics),
    }


def _artifact_payload(artifact: Artifact) -> dict[str, object]:
    payload = artifact.model_dump(by_alias=True, mode="json")

    if artifact.experiment_type is not None:
        payload["experimentType"] = artifact.experiment_type

    return payload


def _metrics_entries_payload(
    metrics: list[SerializeAsAny[Metric]] | None,
) -> list[dict[str, object]]:
    if metrics is None:
        return []

    return [
        {
            "experimentType": metric.experiment_type,
            "metricsJson": json.dumps(
                metric.model_dump(by_alias=True, mode="json"),
                separators=(",", ":"),
            ),
        }
        for metric in metrics
    ]


def _metrics_json(
    metrics: list[SerializeAsAny[Metric]] | None,
) -> str | None:
    if metrics is None:
        return None

    return json.dumps(
        [
            _metric_payload(metric)
            for metric in metrics
        ],
        separators=(",", ":"),
    )


def _metric_payload(metric: Metric) -> dict[str, object]:
    return {
        "experimentType": metric.experiment_type,
        "metricsJson": metric.model_dump(by_alias=True, mode="json"),
    }


def _required_string(value: str | None) -> str:
    """Convert optional metadata values to the required empty-string fallback."""

    return value or ""
