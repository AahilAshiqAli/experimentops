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
    ExperimentRunCompletedEvent,
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

        for execution_config in event.execution_configs:
            if not _is_supported_execution_config(
                execution_config=execution_config,
                registry=registry,
                message=message,
            ):
                return

        try:
            results = [
                registry.execute(
                    experiment_type=execution_config.experiment_type or "",
                    context=_build_execution_context(event, execution_config),
                )
                for execution_config in event.execution_configs
            ]
        except Exception as exception:
            _handle_experiment_run_exception(
                event=event,
                exception=exception,
                producer=failure_producer,
                failure_producer_topic=failure_producer_topic,
                experiment_type=_experiment_types_label(event.execution_configs),
            )
            return

        completed_event = ExperimentRunCompletedEvent.from_requested_event(
            event=event,
            result=_merge_results(results),
            experiment_type=_experiment_types_label(event.execution_configs),
        )

        producer.produce_sync(
            topic=producer_topic,
            key=event.experiment_run_uuid,
            value=completed_event.to_payload(),
        )

        logger.info(
            "Experiment processing finished and completion event published. experiment_types=%s producer_topic=%s result_count=%s",
            _experiment_types_label(event.execution_configs),
            producer_topic,
            len(results),
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
) -> ExperimentExecutionContext:
    return ExperimentExecutionContext.from_requested_event(event, execution_config)


def _merge_results(results: list[Result]) -> Result | None:
    if not results:
        return None

    artifacts = [
        artifact
        for result in results
        for artifact in result.artifact
    ]
    metrics = next(
        (result.metrics for result in reversed(results) if result.metrics is not None),
        None,
    )
    return Result(artifact=artifacts, metrics=metrics)


def _experiment_types_label(
    execution_configs: tuple[ExperimentRunExecutionConfig, ...],
) -> str | None:
    experiment_types = [
        execution_config.experiment_type
        for execution_config in execution_configs
        if execution_config.experiment_type
    ]
    return ",".join(experiment_types) if experiment_types else None
