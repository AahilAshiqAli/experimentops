from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Mapping

from experiment_runtime.kafka.message import KafkaMessage


def _first_present(payload: Mapping[str, Any], *keys: str) -> Any:
    for key in keys:
        value = payload.get(key)

        if value is not None:
            return value

    return None


def _metadata_value(payload: Mapping[str, Any], *keys: str) -> Any:
    metadata = payload.get("metadata")

    if not isinstance(metadata, Mapping):
        return None

    return _first_present(metadata, *keys)


def _event_payload(payload: Mapping[str, Any]) -> Mapping[str, Any]:
    event_payload = payload.get("payload")

    if isinstance(event_payload, Mapping):
        return event_payload

    return {}


@dataclass(frozen=True)
class ExperimentRunRequestedEvent:
    event_uuid: str | None
    request_uuid: str | None
    workspace_uuid: str | None
    project_uuid: str | None
    experiment_uuid: str | None
    experiment_run_uuid: str | None
    experiment_type: str | None
    dataset_uri: str | None
    config_uri: str | None
    triggered_by_user_uuid: str | None
    raw: Mapping[str, Any] = field(default_factory=dict)

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
        event_payload = _event_payload(payload)

        return cls(
            event_uuid=_metadata_value(payload, "uuid", "event_uuid", "eventUuid"),
            request_uuid=(
                _first_present(payload, "request_uuid", "requestUuid")
                or _metadata_value(payload, "request_uuid", "requestUuid")
            ),
            workspace_uuid=(
                _first_present(payload, "workspace_uuid", "workspaceUuid")
                or _metadata_value(payload, "workspace_uuid", "workspaceUuid")
            ),
            project_uuid=(
                _first_present(event_payload, "project_uuid", "projectUuid")
                or _first_present(payload, "project_uuid", "projectUuid")
                or _metadata_value(payload, "project_uuid", "projectUuid")
            ),
            experiment_uuid=(
                _first_present(event_payload, "experiment_uuid", "experimentUuid")
                or _first_present(
                    payload,
                    "experiment_uuid",
                    "experimentUuid",
                )
            ),
            experiment_run_uuid=(
                _first_present(
                    event_payload,
                    "experiment_run_uuid",
                    "experimentRunUuid",
                    "run_uuid",
                    "runUuid",
                )
                or _first_present(
                    payload,
                    "experiment_run_uuid",
                    "experimentRunUuid",
                    "run_uuid",
                    "runUuid",
                )
            ),
            experiment_type=(
                _first_present(event_payload, "experiment_type", "experimentType")
                or _first_present(
                    payload,
                    "experiment_type",
                    "experimentType",
                )
            ),
            dataset_uri=(
                _first_present(event_payload, "dataset_uri", "datasetUri")
                or _first_present(
                    payload,
                    "dataset_uri",
                    "datasetUri",
                )
            ),
            config_uri=(
                _first_present(event_payload, "config_uri", "configUri")
                or _first_present(
                    payload,
                    "config_uri",
                    "configUri",
                )
            ),
            triggered_by_user_uuid=(
                _first_present(
                    event_payload,
                    "triggered_by_user_uuid",
                    "triggeredByUserUuid",
                    "user_uuid",
                    "userUuid",
                )
                or _first_present(
                    payload,
                    "triggered_by_user_uuid",
                    "triggeredByUserUuid",
                    "user_uuid",
                    "userUuid",
                )
            ),
            raw=payload,
        )
