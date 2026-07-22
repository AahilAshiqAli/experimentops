from __future__ import annotations

import tempfile
import re
from dataclasses import dataclass
from pathlib import Path

from experiment_runtime.kafka import (
    ExperimentOpsKafkaProducer,
    KafkaMessage,
)
from experiment_runtime.logging.run_sink import ExperimentRunLogSink
from experiment_runtime.logging.context import ExperimentOpsLogger
from experiment_runtime.models.experiment_execution_context import (
    ExperimentExecutionContext,
    ExperimentInputContext,
)
from experiment_runtime.models.experiment_run_completed_event import (
    Artifact,
    ExperimentRunCompletedEvent,
    Metric,
    Result,
)
from experiment_runtime.models.experiment_run_failure_event import (
    ExperimentRunFailureEvent,
)
from experiment_runtime.models.experiment_run_requested_event import (
    ExperimentRunExecutionConfig,
    ExperimentRunExecutionPlanInput,
    ExperimentRunExecutionPlanOutput,
    ExperimentRunRequestedEvent,
)
from experiment_runtime.registry import ExperimentRegistry
from experiment_runtime.storage import ObjectStorage

from analysis_worker.handlers.experiment_run_progress_publisher import (
    ExperimentRunProgressPublisher,
)


logger = ExperimentOpsLogger.get_logger("analysis-worker")
DEFAULT_TIME_WEIGHT = 1.0
CONNECTABLE_DOWN_STREAM_POLICY = "CONNECTABLE"
INTERNAL_DOWN_STREAM_POLICY = "INTERNAL"


@dataclass(frozen=True)
class _PipelineExecution:
    step_results: list[tuple[ExperimentRunExecutionConfig, Result]]
    progress_sequence: int
    last_published_progress: int | None
    current_step: int | None


class _PipelineExecutionError(Exception):
    def __init__(
        self,
        original: Exception,
        *,
        progress_sequence: int,
        last_published_progress: int | None,
        current_step: int | None,
    ) -> None:
        super().__init__(str(original))
        self.original = original
        self.progress_sequence = progress_sequence
        self.last_published_progress = last_published_progress
        self.current_step = current_step


def build_experiment_run_requested_handler(
    registry: ExperimentRegistry,
    producer: ExperimentOpsKafkaProducer,
    producer_topic: str,
    failure_producer: ExperimentOpsKafkaProducer,
    failure_producer_topic: str,
    progress_publisher: ExperimentRunProgressPublisher | None = None,
    object_storage: ObjectStorage | None = None,
    log_root_dir: Path | None = None,
):
    def handle_experiment_run_requested(message: KafkaMessage) -> None:
        event = ExperimentRunRequestedEvent.from_kafka_message(message)
        run_log_sink = _create_run_log_sink(event, log_root_dir)

        logger.info(
            "Received experiment run request. execution_config_count=%s topic=%s partition=%s offset=%s",
            len(event.execution_configs),
            message.topic,
            message.partition,
            message.offset,
        )

        if not event.execution_configs:
            logger.warning(
                "Skipping experiment run request because execution_configs are missing. topic=%s partition=%s offset=%s",
                message.topic,
                message.partition,
                message.offset,
            )
            return

        execution_configs = _sort_execution_configs(event.execution_configs)

        for execution_config in execution_configs:
            if not _is_supported_execution_config(
                execution_config=execution_config,
                registry=registry,
                message=message,
            ):
                return

        try:
            pipeline_execution = _execute_pipeline(
                event=event,
                execution_configs=execution_configs,
                registry=registry,
                run_log_sink=run_log_sink,
            )
        except _PipelineExecutionError as pipeline_error:
            exception = pipeline_error.original
            run_log_sink.error(
                _experiment_types_label(execution_configs),
                "Experiment processing failed: %s",
                str(exception) or exception.__class__.__name__,
            )
            _publish_final_progress(
                event=event,
                progress_publisher=progress_publisher,
                progress=pipeline_error.last_published_progress or 0,
                current_step=pipeline_error.current_step,
                progress_sequence=pipeline_error.progress_sequence,
                run_log_sink=run_log_sink,
            )
            log_file_url = _upload_run_logs(
                event=event,
                run_log_sink=run_log_sink,
                object_storage=object_storage,
            )
            _handle_experiment_run_exception(
                event=event,
                exception=exception,
                producer=failure_producer,
                failure_producer_topic=failure_producer_topic,
                experiment_type=_experiment_types_label(execution_configs),
                log_file_url=log_file_url,
            )
            return

        if (
            pipeline_execution.last_published_progress != 100
            or run_log_sink.has_pending_progress_logs()
        ):
            _publish_final_progress(
                event=event,
                progress_publisher=progress_publisher,
                progress=100,
                current_step=pipeline_execution.current_step,
                progress_sequence=pipeline_execution.progress_sequence,
                run_log_sink=run_log_sink,
            )
        log_file_url = _upload_run_logs(
            event=event,
            run_log_sink=run_log_sink,
            object_storage=object_storage,
        )
        completed_event = ExperimentRunCompletedEvent.from_requested_event(
            event=event,
            result=_merge_results(pipeline_execution.step_results),
            experiment_type=_experiment_types_label(execution_configs),
            log_file_url=log_file_url,
        )

        producer.produce_sync(
            topic=producer_topic,
            key=event.experiment_run_uuid,
            value=completed_event.to_payload(),
        )

        logger.info(
            "Experiment processing finished and completion event published. experiment_types=%s producer_topic=%s result_count=%s",
            _experiment_types_label(execution_configs),
            producer_topic,
            len(pipeline_execution.step_results),
        )

    return handle_experiment_run_requested


def _handle_experiment_run_exception(
    event: ExperimentRunRequestedEvent,
    exception: BaseException,
    producer: ExperimentOpsKafkaProducer,
    failure_producer_topic: str,
    experiment_type: str | None = None,
    log_file_url: str | None = None,
) -> None:
    logger.exception(
        "Experiment processing failed. experiment_type=%s",
        experiment_type or event.experiment_type,
    )

    _publish_failure_event(
        event=event,
        exception=exception,
        producer=producer,
        failure_producer_topic=failure_producer_topic,
        experiment_type=experiment_type,
        log_file_url=log_file_url,
    )


def _publish_failure_event(
    event: ExperimentRunRequestedEvent,
    exception: BaseException,
    producer: ExperimentOpsKafkaProducer,
    failure_producer_topic: str,
    experiment_type: str | None = None,
    log_file_url: str | None = None,
) -> None:
    failure_event = ExperimentRunFailureEvent.from_requested_event(
        event=event,
        exception=exception,
        experiment_type=experiment_type,
        log_file_url=log_file_url,
    )

    producer.produce_sync(
        topic=failure_producer_topic,
        key=event.experiment_run_uuid,
        value=failure_event.to_payload(),
    )

    logger.info(
        "Experiment processing failed and failure event published. experiment_type=%s producer_topic=%s error_type=%s",
        experiment_type or event.experiment_type,
        failure_producer_topic,
        exception.__class__.__name__,
    )


def _create_run_log_sink(
    event: ExperimentRunRequestedEvent,
    log_root_dir: Path | None,
) -> ExperimentRunLogSink:
    root_dir = log_root_dir or Path(tempfile.gettempdir()) / "experimentops"
    run_id = event.experiment_run_uuid or event.event_uuid or "unknown-run"

    return ExperimentRunLogSink(root_dir / run_id / "run.log.jsonl")


def _publish_final_progress(
    event: ExperimentRunRequestedEvent,
    progress_publisher: ExperimentRunProgressPublisher | None,
    progress: int,
    current_step: int | None,
    progress_sequence: int,
    run_log_sink: ExperimentRunLogSink,
) -> None:
    if progress_publisher is None:
        return

    context = ExperimentExecutionContext.from_requested_event(event).model_copy(
        update={
            "current_step": current_step,
            "progress_sequence": progress_sequence,
            "run_log_sink": run_log_sink,
        }
    )
    progress_publisher.publish(context, progress)


def _upload_run_logs(
    event: ExperimentRunRequestedEvent,
    run_log_sink: ExperimentRunLogSink,
    object_storage: ObjectStorage | None,
) -> str | None:
    if object_storage is None:
        return None

    key = _run_log_object_key(event)
    return object_storage.upload_file(key, run_log_sink.read_bytes_for_upload())


def _run_log_object_key(event: ExperimentRunRequestedEvent) -> str:
    workspace_uuid = event.workspace_uuid or "unknown-workspace"
    project_uuid = event.project_uuid or "unknown-project"
    experiment_uuid = event.experiment_uuid or "unknown-experiment"
    run_uuid = event.experiment_run_uuid or "unknown-run"

    return (
        f"workspaces/{workspace_uuid}/projects/{project_uuid}/"
        f"experiments/{experiment_uuid}/runs/{run_uuid}/logs/run.jsonl"
    )


def _current_step(
    execution_configs: tuple[ExperimentRunExecutionConfig, ...],
    index: int,
) -> int | None:
    if not execution_configs:
        return None

    execution_config = execution_configs[index]
    return execution_config.step_count or index + 1


def _is_supported_execution_config(
    execution_config: ExperimentRunExecutionConfig,
    registry: ExperimentRegistry,
    message: KafkaMessage,
) -> bool:
    experiment_type = execution_config.experiment_type
    if not experiment_type:
        logger.warning(
            "Skipping experiment run request because experiment_type is missing in execution config. step_count=%s topic=%s partition=%s offset=%s",
            execution_config.step_count,
            message.topic,
            message.partition,
            message.offset,
        )
        return False

    if experiment_type.strip().upper() not in registry.supported_types():
        logger.warning(
            "Skipping experiment run request because experiment_type is unsupported. experiment_type=%s supported_experiment_types=%s topic=%s partition=%s offset=%s",
            experiment_type,
            registry.supported_types(),
            message.topic,
            message.partition,
            message.offset,
        )
        return False

    return True


def _build_execution_context(
    event: ExperimentRunRequestedEvent,
    execution_config: ExperimentRunExecutionConfig,
    dataset_uri: str | None = None,
    inputs: dict[str, ExperimentInputContext] | None = None,
    previous_result: Result | None = None,
    pipeline_results: list[Result] | None = None,
    progress_completed_weight: float | None = None,
    progress_step_weight: float | None = None,
    progress_total_weight: float | None = None,
    current_step: int | None = None,
    progress_sequence: int = 0,
    run_log_sink: ExperimentRunLogSink | None = None,
) -> ExperimentExecutionContext:
    return ExperimentExecutionContext.from_requested_event(
        event,
        execution_config,
    ).model_copy(
        update={
            "dataset_uri": dataset_uri,
            "inputs": inputs or {},
            "previous_result": previous_result,
            "pipeline_results": pipeline_results or [],
            "progress_completed_weight": progress_completed_weight,
            "progress_step_weight": progress_step_weight,
            "progress_total_weight": progress_total_weight,
            "current_step": current_step,
            "progress_sequence": progress_sequence,
            "run_log_sink": run_log_sink,
        }
    )


def _execute_pipeline(
    event: ExperimentRunRequestedEvent,
    execution_configs: tuple[ExperimentRunExecutionConfig, ...],
    registry: ExperimentRegistry,
    run_log_sink: ExperimentRunLogSink,
) -> _PipelineExecution:
    step_results: list[tuple[ExperimentRunExecutionConfig, Result]] = []
    progress_states = _progress_states(execution_configs)
    progress_sequence = 0
    last_published_progress: int | None = None
    current_step: int | None = None
    artifacts_by_step_and_name: dict[
        tuple[int, str],
        tuple[Artifact, ExperimentRunExecutionPlanOutput],
    ] = {}
    previous_result: Result | None = None

    for index, execution_config in enumerate(execution_configs):
        current_step = _current_step(execution_configs, index)
        progress_completed_weight, progress_step_weight, progress_total_weight = (
            progress_states[index]
        )
        inputs = _resolve_execution_inputs(
            execution_config=execution_config,
            artifacts_by_step_and_name=artifacts_by_step_and_name,
        )
        context = _build_execution_context(
            event=event,
            execution_config=execution_config,
            dataset_uri=_primary_input_uri(inputs),
            inputs=inputs,
            previous_result=previous_result,
            pipeline_results=[
                step_result
                for _, step_result in step_results
            ],
            progress_completed_weight=progress_completed_weight,
            progress_step_weight=progress_step_weight,
            progress_total_weight=progress_total_weight,
            current_step=current_step,
            progress_sequence=progress_sequence,
            run_log_sink=run_log_sink,
        )
        try:
            result = registry.execute(
                experiment_type=execution_config.experiment_type or "",
                context=context,
            )
        except Exception as exception:
            raise _PipelineExecutionError(
                exception,
                progress_sequence=context.progress_sequence,
                last_published_progress=context.last_published_progress,
                current_step=current_step,
            ) from exception
        progress_sequence = context.progress_sequence
        result = _record_step_artifacts(
            execution_config=execution_config,
            result=result,
            artifacts_by_step_and_name=artifacts_by_step_and_name,
        )
        last_published_progress = context.last_published_progress
        step_results.append((execution_config, result))
        previous_result = result

    return _PipelineExecution(
        step_results=step_results,
        progress_sequence=progress_sequence,
        last_published_progress=last_published_progress,
        current_step=current_step,
    )


def _resolve_execution_inputs(
    execution_config: ExperimentRunExecutionConfig,
    artifacts_by_step_and_name: dict[
        tuple[int, str],
        tuple[Artifact, ExperimentRunExecutionPlanOutput],
    ],
) -> dict[str, ExperimentInputContext]:
    inputs: dict[str, ExperimentInputContext] = {}

    for planned_input in execution_config.inputs:
        port_name = _required_value(planned_input.port_name, "executionPlan.steps.inputs.portName")
        if planned_input.input_type == "DATASET":
            inputs[port_name] = _dataset_input_context(planned_input)
            continue

        if planned_input.input_type == "ARTIFACT":
            inputs[port_name] = _artifact_input_context(
                planned_input=planned_input,
                artifacts_by_step_and_name=artifacts_by_step_and_name,
            )
            continue

        raise ValueError(
            f"Unsupported execution input type={planned_input.input_type} port_name={port_name}"
        )

    return inputs


def _dataset_input_context(
    planned_input: ExperimentRunExecutionPlanInput,
) -> ExperimentInputContext:
    return ExperimentInputContext(
        port_name=planned_input.port_name,
        input_type=planned_input.input_type,
        data_kind=planned_input.data_kind,
        format=planned_input.format,
        uri=_required_value(planned_input.dataset_uri, "executionPlan.steps.inputs.datasetUri"),
        dataset_version_uuid=planned_input.dataset_version_uuid,
    )


def _artifact_input_context(
    planned_input: ExperimentRunExecutionPlanInput,
    artifacts_by_step_and_name: dict[
        tuple[int, str],
        tuple[Artifact, ExperimentRunExecutionPlanOutput],
    ],
) -> ExperimentInputContext:
    source_step_count = _required_int(
        planned_input.source_step_count,
        "executionPlan.steps.inputs.sourceStepCount",
    )
    artifact_name = _required_value(
        planned_input.artifact_name,
        "executionPlan.steps.inputs.artifactName",
    )
    produced_artifact = artifacts_by_step_and_name.get((source_step_count, artifact_name))
    if produced_artifact is None:
        raise ValueError(
            f"Missing CONNECTABLE artifact. source_step_count={source_step_count} artifact_name={artifact_name}"
        )

    artifact, output = produced_artifact
    if planned_input.format and _normalize_value(planned_input.format) != _normalize_value(artifact.format):
        raise ValueError(
            f"Artifact input format mismatch. artifact_name={artifact_name} expected={planned_input.format} actual={artifact.format}"
        )
    if (
        planned_input.data_kind
        and output.data_kind
        and _normalize_value(planned_input.data_kind) != _normalize_value(output.data_kind)
    ):
        raise ValueError(
            f"Artifact input dataKind mismatch. artifact_name={artifact_name} expected={planned_input.data_kind} actual={output.data_kind}"
        )

    return ExperimentInputContext(
        port_name=planned_input.port_name,
        input_type=planned_input.input_type,
        data_kind=planned_input.data_kind,
        format=planned_input.format or artifact.format,
        uri=artifact.uri,
        source_step_count=source_step_count,
        artifact_name=artifact_name,
    )


def _record_step_artifacts(
    execution_config: ExperimentRunExecutionConfig,
    result: Result,
    artifacts_by_step_and_name: dict[
        tuple[int, str],
        tuple[Artifact, ExperimentRunExecutionPlanOutput],
    ],
) -> Result:
    planned_outputs = tuple(
        output for output in execution_config.outputs
        if output.name
    )
    if not planned_outputs:
        if result.artifact:
            raise ValueError(
                f"Step {execution_config.step_count} produced artifacts but executionPlan.outputs is empty"
            )
        return result

    artifacts = tuple(result.artifact or ())
    if len(planned_outputs) == 1 and len(artifacts) == 1:
        output = planned_outputs[0]
        normalized_artifact = _normalize_artifact_for_output(
            artifact=artifacts[0],
            output=output,
            step_count=execution_config.step_count,
        )
        _index_artifact_if_connectable(
            artifact=normalized_artifact,
            output=output,
            step_count=execution_config.step_count,
            artifacts_by_step_and_name=artifacts_by_step_and_name,
        )
        return result.model_copy(update={"artifact": [normalized_artifact]})

    artifacts_by_type = {artifact.type: artifact for artifact in artifacts}
    normalized_artifacts_by_type = {
        _normalize_artifact_name(artifact.type): artifact
        for artifact in artifacts
    }
    matched_artifact_types: set[str] = set()
    normalized_artifacts: list[Artifact] = []
    for output in planned_outputs:
        artifact = _artifact_for_output(
            output=output,
            artifacts_by_type=artifacts_by_type,
            normalized_artifacts_by_type=normalized_artifacts_by_type,
        )
        if artifact is None:
            if _is_required_for_run(output):
                raise ValueError(
                    f"Step {execution_config.step_count} did not produce planned output artifact={output.name}"
            )
            continue

        matched_artifact_types.add(artifact.type)
        normalized_artifact = _normalize_artifact_for_output(
            artifact=artifact,
            output=output,
            step_count=execution_config.step_count,
        )
        _index_artifact_if_connectable(
            artifact=normalized_artifact,
            output=output,
            step_count=execution_config.step_count,
            artifacts_by_step_and_name=artifacts_by_step_and_name,
        )
        normalized_artifacts.append(normalized_artifact)

    unexpected_artifact_types = [
        artifact.type
        for artifact in artifacts
        if artifact.type not in matched_artifact_types
    ]
    if unexpected_artifact_types:
        raise ValueError(
            f"Step {execution_config.step_count} produced unexpected artifacts={unexpected_artifact_types}"
        )

    return result.model_copy(update={"artifact": normalized_artifacts})


def _is_required_for_run(output: ExperimentRunExecutionPlanOutput) -> bool:
    return output.required_for_run is not False


def _normalize_artifact_for_output(
    artifact: Artifact,
    output: ExperimentRunExecutionPlanOutput,
    step_count: int | None,
) -> Artifact:
    output_name = _required_value(output.name, "executionPlan.steps.outputs.name")
    output_format = output.format or artifact.format

    if _normalize_value(artifact.format) != _normalize_value(output_format):
        raise ValueError(
            f"Step {step_count} artifact format mismatch. artifact={output_name} expected={output_format} actual={artifact.format}"
        )

    return artifact.model_copy(
        update={
            "type": output_name,
            "format": output_format,
            "step_count": step_count,
            "port_name": output_name,
        }
    )


def _artifact_for_output(
    output: ExperimentRunExecutionPlanOutput,
    artifacts_by_type: dict[str, Artifact],
    normalized_artifacts_by_type: dict[str, Artifact],
) -> Artifact | None:
    if output.name is None:
        return None

    artifact = artifacts_by_type.get(output.name)
    if artifact is not None:
        return artifact

    normalized_output_name = _normalize_artifact_name(output.name)
    artifact = normalized_artifacts_by_type.get(normalized_output_name)
    if artifact is not None:
        return artifact

    alias = _PLANNED_OUTPUT_ALIASES.get(normalized_output_name)
    if alias is None:
        return None

    return normalized_artifacts_by_type.get(alias)


_PLANNED_OUTPUT_ALIASES = {
    "cleanedtraindata": "cleaneddataset",
    "trainingreport": "cleaningreport",
}


def _normalize_artifact_name(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "", name.lower())


def _index_artifact_if_connectable(
    artifact: Artifact,
    output: ExperimentRunExecutionPlanOutput,
    step_count: int | None,
    artifacts_by_step_and_name: dict[
        tuple[int, str],
        tuple[Artifact, ExperimentRunExecutionPlanOutput],
    ],
) -> None:
    resolved_step_count = _required_int(step_count, "executionPlan.steps.stepCount")
    output_name = _required_value(output.name, "executionPlan.steps.outputs.name")
    down_stream_policy = _normalize_value(output.down_stream_policy)

    if down_stream_policy == CONNECTABLE_DOWN_STREAM_POLICY:
        artifacts_by_step_and_name[(resolved_step_count, output_name)] = (artifact, output)
        return

    if down_stream_policy == INTERNAL_DOWN_STREAM_POLICY:
        return

    if down_stream_policy:
        return

    raise ValueError(f"Step {step_count} output {output_name} is missing downStreamPolicy")


def _primary_input_uri(inputs: dict[str, ExperimentInputContext]) -> str | None:
    for input_context in inputs.values():
        if input_context.uri:
            return input_context.uri

    return None


def _merge_results(
    step_results: list[tuple[ExperimentRunExecutionConfig, Result]],
) -> Result | None:
    if not step_results:
        return None

    artifacts = [
        _with_experiment_type(
            artifact=artifact,
            experiment_type=execution_config.experiment_type,
        )
        for execution_config, result in step_results
        for artifact in _visible_artifacts(execution_config, result)
    ]
    metrics = [
        _with_metric_experiment_type(
            metric=metric,
            experiment_type=execution_config.experiment_type,
        )
        for execution_config, result in step_results
        for metric in _result_metrics(result)
    ]
    return Result(artifact=artifacts, metrics=metrics or None)


def _visible_artifacts(
    execution_config: ExperimentRunExecutionConfig,
    result: Result,
) -> list[Artifact]:
    policies_by_name = {
        output.name: _normalize_value(output.down_stream_policy)
        for output in execution_config.outputs
        if output.name
    }
    return [
        artifact for artifact in result.artifact
        if policies_by_name.get(artifact.type) != INTERNAL_DOWN_STREAM_POLICY
    ]


def _progress_states(
    execution_configs: tuple[ExperimentRunExecutionConfig, ...],
) -> tuple[tuple[float, float, float], ...]:
    weights = [
        _time_weight(execution_config)
        for execution_config in execution_configs
    ]
    total_weight = sum(weights)
    completed_weight = 0.0
    progress_states: list[tuple[float, float, float]] = []

    for weight in weights:
        progress_states.append((completed_weight, weight, total_weight))
        completed_weight += weight

    return tuple(progress_states)


def _time_weight(execution_config: ExperimentRunExecutionConfig) -> float:
    if execution_config.time_weight is None or execution_config.time_weight <= 0:
        return DEFAULT_TIME_WEIGHT

    return execution_config.time_weight


def _sort_execution_configs(
    execution_configs: tuple[ExperimentRunExecutionConfig, ...],
) -> tuple[ExperimentRunExecutionConfig, ...]:
    return tuple(
        sorted(
            execution_configs,
            key=lambda execution_config: execution_config.step_count or 0,
        )
    )


def _with_experiment_type(
    artifact: Artifact,
    experiment_type: str | None,
) -> Artifact:
    return artifact.model_copy(update={"experiment_type": experiment_type})


def _with_metric_experiment_type(
    metric: Metric,
    experiment_type: str | None,
) -> Metric:
    return metric.model_copy(update={"experiment_type": experiment_type})


def _result_metrics(result: Result) -> list[Metric]:
    if result.metrics is None:
        return []

    if isinstance(result.metrics, list):
        return result.metrics

    return [result.metrics]


def _experiment_types_label(
    execution_configs: tuple[ExperimentRunExecutionConfig, ...],
) -> str | None:
    experiment_types = [
        execution_config.experiment_type
        for execution_config in execution_configs
        if execution_config.experiment_type
    ]
    return ",".join(experiment_types) if experiment_types else None


def _required_value(value: str | None, field_name: str) -> str:
    if value is None or not value.strip():
        raise ValueError(f"{field_name} is required")

    return value


def _required_int(value: int | None, field_name: str) -> int:
    if value is None:
        raise ValueError(f"{field_name} is required")

    return value


def _normalize_value(value: str | None) -> str | None:
    if value is None:
        return None

    return value.strip().upper()
