from __future__ import annotations

from typing import Any, Mapping

from experiment_runtime.base_model import ExperimentOpsModel
from experiment_runtime.kafka.message import KafkaMessage


class ExperimentRunRequestedEvent(ExperimentOpsModel):
    event_uuid: str | None
    request_uuid: str | None
    requester_uuid: str | None
    workspace_uuid: str | None
    project_uuid: str | None
    experiment_uuid: str | None
    experiment_run_uuid: str | None
    experiment_type: str | None
    dataset_uri: str | None
    config_json: Any | None
    request_timestamp: int | None
    user_role: str | None

    @classmethod
    def from_kafka_message(
        cls,
        message: KafkaMessage,
    ) -> "ExperimentRunRequestedEvent":
        return cls.from_payload(message.value or {})

    @classmethod
    def from_payload(
        cls,
        payload: Mapping[str, Any],
    ) -> "ExperimentRunRequestedEvent":
        if isinstance(payload.get("metadata"), Mapping):
            _payload = payload
        else:
            _payload = payload.get("payload") or payload
            if not isinstance(_payload, Mapping):
                _payload = {}

        _metadata = _payload.get("metadata") or {}
        if not isinstance(_metadata, Mapping):
            _metadata = {}

        _event_payload = _payload.get("payload") or {}
        if not isinstance(_event_payload, Mapping):
            _event_payload = {}

        return cls(
            event_uuid=_metadata.get("eventUuid"),
            request_uuid=_metadata.get("requestUuid") or _metadata.get("traceUuid"),
            requester_uuid=_metadata.get("requesterUuid"),
            workspace_uuid=_metadata.get("workspaceUuid"),
            project_uuid=_event_payload.get("projectUuid"),
            experiment_uuid=_event_payload.get("experimentUuid"),
            experiment_run_uuid=_metadata.get("uuid"),
            experiment_type=_event_payload.get("experimentType"),
            dataset_uri=_event_payload.get("datasetUri"),
            config_json=_event_payload.get("configJson"),
            request_timestamp=_metadata.get("requestTimestamp"),
            user_role=_metadata.get("userRole"),
        )
