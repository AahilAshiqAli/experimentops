from __future__ import annotations

from typing import Any, Mapping

from experiment_runtime.base_model import ExperimentOpsModel
from experiment_runtime.kafka.message import KafkaMessage


class ExperimentRunExecutionPlanInput(ExperimentOpsModel):
    port_name: str | None = None
    input_type: str | None = None
    data_kind: str | None = None
    format: str | None = None
    dataset_version_uuid: str | None = None
    dataset_uri: str | None = None
    source_step_count: int | None = None
    artifact_name: str | None = None

    @classmethod
    def from_payload(cls, payload: Mapping[str, Any]) -> "ExperimentRunExecutionPlanInput":
        contract = payload.get("contract")
        if not isinstance(contract, Mapping):
            contract = {}

        accepted_formats = contract.get("acceptedFormats")
        inferred_format = (
            accepted_formats[0]
            if isinstance(accepted_formats, list) and accepted_formats
            else None
        )

        return cls(
            port_name=payload.get("portName"),
            input_type=payload.get("inputType"),
            data_kind=payload.get("dataKind") or contract.get("dataKind"),
            format=payload.get("format") or inferred_format,
            dataset_version_uuid=payload.get("datasetVersionUuid"),
            dataset_uri=payload.get("datasetUri"),
            source_step_count=payload.get("sourceStepCount"),
            artifact_name=payload.get("artifactName"),
        )


class ExperimentRunExecutionPlanOutput(ExperimentOpsModel):
    name: str | None = None
    data_kind: str | None = None
    format_strategy: str | None = None
    format: str | None = None
    source_input_port: str | None = None
    required_for_run: bool | None = None
    down_stream_policy: str | None = None

    @classmethod
    def from_payload(cls, payload: Mapping[str, Any]) -> "ExperimentRunExecutionPlanOutput":
        output_type = payload.get("type")
        if not isinstance(output_type, Mapping):
            output_type = {}

        return cls(
            name=payload.get("name"),
            data_kind=payload.get("dataKind"),
            format_strategy=payload.get("formatStrategy") or output_type.get("type"),
            format=payload.get("format") or output_type.get("format"),
            source_input_port=payload.get("sourceInputPort") or output_type.get("sourceInputPort"),
            required_for_run=payload.get("requiredForRun", payload.get("required")),
            down_stream_policy=payload.get("downStreamPolicy"),
        )


class ExperimentRunExecutionPlanStep(ExperimentOpsModel):
    step_count: int | None = None
    experiment_config_uuid: str | None = None
    experiment_type: str | None = None
    experiment_config_json: Any | None = None
    time_weight: float | None = None
    inputs: tuple[ExperimentRunExecutionPlanInput, ...] = ()
    outputs: tuple[ExperimentRunExecutionPlanOutput, ...] = ()

    @classmethod
    def from_payload(cls, payload: Mapping[str, Any]) -> "ExperimentRunExecutionPlanStep":
        return cls(
            step_count=payload.get("stepCount"),
            experiment_config_uuid=payload.get("experimentConfigUuid"),
            experiment_type=payload.get("experimentType"),
            experiment_config_json=payload.get("experimentConfigJson"),
            time_weight=payload.get("timeWeight"),
            inputs=tuple(
                ExperimentRunExecutionPlanInput.from_payload(item)
                for item in _mapping_items(payload.get("inputs"))
            ),
            outputs=tuple(
                ExperimentRunExecutionPlanOutput.from_payload(item)
                for item in _mapping_items(payload.get("outputs"))
            ),
        )


class ExperimentRunExecutionConfig(ExperimentRunExecutionPlanStep):
    """Backward-compatible name for execution-plan steps."""


class ExperimentRunExecutionPlan(ExperimentOpsModel):
    schema_version: int | None = None
    steps: tuple[ExperimentRunExecutionConfig, ...] = ()

    @classmethod
    def from_payload(cls, payload: Mapping[str, Any]) -> "ExperimentRunExecutionPlan":
        return cls(
            schema_version=payload.get("schemaVersion"),
            steps=tuple(
                ExperimentRunExecutionConfig.from_payload(item)
                for item in _mapping_items(payload.get("steps"))
            ),
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
    execution_plan: ExperimentRunExecutionPlan | None = None
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

        execution_plan = _execution_plan_from_payload(_event_payload.get("executionPlan"))

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
            execution_plan=execution_plan,
            execution_configs=execution_plan.steps if execution_plan else (),
            request_timestamp=_metadata.get("requestTimestamp"),
            user_role=_metadata.get("userRole"),
        )


def _execution_plan_from_payload(payload: Any) -> ExperimentRunExecutionPlan | None:
    if not isinstance(payload, Mapping):
        return None

    return ExperimentRunExecutionPlan.from_payload(payload)


def _mapping_items(value: Any) -> tuple[Mapping[str, Any], ...]:
    if not isinstance(value, list):
        return ()

    return tuple(item for item in value if isinstance(item, Mapping))
