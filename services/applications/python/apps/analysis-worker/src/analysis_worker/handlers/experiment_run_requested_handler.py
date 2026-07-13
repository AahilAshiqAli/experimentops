from __future__ import annotations

from experiment_runtime.kafka import (
    ExperimentOpsKafkaProducer,
    KafkaMessage,
)
from experiment_runtime.logging.context import ExperimentOpsLogger
from experiment_runtime.models.experiment_execution_context import (
    ExperimentExecutionContext,
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
    ExperimentRunRequestedEvent,
)
from experiment_runtime.registry import ExperimentRegistry


logger = ExperimentOpsLogger.get_logger("analysis-worker")
DEFAULT_TIME_WEIGHT = 1.0


def build_experiment_run_requested_handler(
    registry: ExperimentRegistry,
    producer: ExperimentOpsKafkaProducer,
    producer_topic: str,
    failure_producer: ExperimentOpsKafkaProducer,
    failure_producer_topic: str,
):
    def handle_experiment_run_requested(message: KafkaMessage) -> None:
        event = ExperimentRunRequestedEvent.from_kafka_message(message)

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
            step_results = _execute_pipeline(
                event=event,
                execution_configs=execution_configs,
                registry=registry,
            )
        except Exception as exception:
            _handle_experiment_run_exception(
                event=event,
                exception=exception,
                producer=failure_producer,
                failure_producer_topic=failure_producer_topic,
                experiment_type=_experiment_types_label(execution_configs),
            )
            return

        completed_event = ExperimentRunCompletedEvent.from_requested_event(
            event=event,
            result=_merge_results(step_results),
            experiment_type=_experiment_types_label(execution_configs),
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
            len(step_results),
        )

    return handle_experiment_run_requested


def _handle_experiment_run_exception(
    event: ExperimentRunRequestedEvent,
    exception: BaseException,
    producer: ExperimentOpsKafkaProducer,
    failure_producer_topic: str,
    experiment_type: str | None = None,
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
    )


def _publish_failure_event(
    event: ExperimentRunRequestedEvent,
    exception: BaseException,
    producer: ExperimentOpsKafkaProducer,
    failure_producer_topic: str,
    experiment_type: str | None = None,
) -> None:
    failure_event = ExperimentRunFailureEvent.from_requested_event(
        event=event,
        exception=exception,
        experiment_type=experiment_type,
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
    previous_result: Result | None = None,
    pipeline_results: list[Result] | None = None,
    progress_completed_weight: float | None = None,
    progress_step_weight: float | None = None,
    progress_total_weight: float | None = None,
) -> ExperimentExecutionContext:
    return ExperimentExecutionContext.from_requested_event(
        event,
        execution_config,
    ).model_copy(
        update={
            "dataset_uri": dataset_uri or event.dataset_uri,
            "previous_result": previous_result,
            "pipeline_results": pipeline_results or [],
            "progress_completed_weight": progress_completed_weight,
            "progress_step_weight": progress_step_weight,
            "progress_total_weight": progress_total_weight,
        }
    )


def _execute_pipeline(
    event: ExperimentRunRequestedEvent,
    execution_configs: tuple[ExperimentRunExecutionConfig, ...],
    registry: ExperimentRegistry,
) -> list[tuple[ExperimentRunExecutionConfig, Result]]:
    step_results: list[tuple[ExperimentRunExecutionConfig, Result]] = []
    progress_states = _progress_states(execution_configs)
    current_dataset_uri = event.dataset_uri
    previous_result: Result | None = None

    for index, execution_config in enumerate(execution_configs):
        progress_completed_weight, progress_step_weight, progress_total_weight = (
            progress_states[index]
        )
        result = registry.execute(
            experiment_type=execution_config.experiment_type or "",
            context=_build_execution_context(
                event=event,
                execution_config=execution_config,
                dataset_uri=current_dataset_uri,
                previous_result=previous_result,
                pipeline_results=[
                    step_result
                    for _, step_result in step_results
                ],
                progress_completed_weight=progress_completed_weight,
                progress_step_weight=progress_step_weight,
                progress_total_weight=progress_total_weight,
            ),
        )
        step_results.append((execution_config, result))
        previous_result = result

        if index < len(execution_configs) - 1:
            current_dataset_uri = _resultant_artifact_uri(result)

    return step_results


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
        for artifact in result.artifact
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


def _resultant_artifact_uri(result: Result) -> str:
    if not result.artifact:
        raise ValueError("Pipeline step did not produce an artifact for the next step")

    return result.artifact[0].uri


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
