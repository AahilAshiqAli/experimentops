from __future__ import annotations

import json
from typing import Any, Mapping

from experiment_runtime.base_model import ExperimentOpsModel
from experiment_runtime.kafka.message import KafkaMessage


class ExperimentRunExecutionConfig(ExperimentOpsModel):
    step_count: int | None = None
    experiment_type: str | None = None
    experiment_config_json: Any | None = None
    time_weight: float | None = None

    @classmethod
    def from_payload(cls, payload: Mapping[str, Any]) -> "ExperimentRunExecutionConfig":
        return cls(
            step_count=payload.get("stepCount"),
            experiment_type=payload.get("experimentType"),
            experiment_config_json=payload.get("experimentConfigJson"),
            time_weight=payload.get("timeWeight"),
        )


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
    execution_configs: tuple[ExperimentRunExecutionConfig, ...] = ()
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

        config_json = _event_payload.get("configJson")

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
            config_json=config_json,
            execution_configs=_execution_configs_from_config_json(
                config_json=config_json,
                experiment_type=_event_payload.get("experimentType"),
            ),
            request_timestamp=_metadata.get("requestTimestamp"),
            user_role=_metadata.get("userRole"),
        )


def _execution_configs_from_config_json(
    config_json: Any,
    experiment_type: str | None,
) -> tuple[ExperimentRunExecutionConfig, ...]:
    parsed_config_json = _parse_config_json(config_json)
    if isinstance(parsed_config_json, list):
        return tuple(
            ExperimentRunExecutionConfig.from_payload(item)
            for item in parsed_config_json
            if isinstance(item, Mapping)
        )

    if experiment_type:
        return (
            ExperimentRunExecutionConfig(
                step_count=1,
                experiment_type=experiment_type,
                experiment_config_json=parsed_config_json,
            ),
        )

    return ()


def _parse_config_json(config_json: Any) -> Any:
    if not isinstance(config_json, str):
        return config_json

    try:
        return json.loads(config_json)
    except json.JSONDecodeError:
        return config_json
