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
)
from experiment_runtime.models.experiment_run_failure_event import (
    ExperimentRunFailureEvent,
)
from experiment_runtime.models.experiment_run_requested_event import (
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
            "Received experiment run request. experiment_type=%s topic=%s partition=%s offset=%s",
            event.experiment_type,
            message.topic,
            message.partition,
            message.offset,
        )

        if not event.experiment_type:
            logger.warning(
                "Skipping experiment run request because experiment_type is missing. topic=%s partition=%s offset=%s",
                message.topic,
                message.partition,
                message.offset,
            )
            return

        if event.experiment_type.strip().upper() not in registry.supported_types():
            logger.warning(
                "Skipping experiment run request because experiment_type is unsupported. experiment_type=%s supported_experiment_types=%s topic=%s partition=%s offset=%s",
                event.experiment_type,
                registry.supported_types(),
                message.topic,
                message.partition,
                message.offset,
            )
            return

        try:
            result = registry.execute(
                experiment_type=event.experiment_type,
                context=_build_execution_context(event)
            )
        except Exception as exception:
            _handle_experiment_run_exception(
                event=event,
                exception=exception,
                producer=failure_producer,
                failure_producer_topic=failure_producer_topic,
            )
            return

        completed_event = ExperimentRunCompletedEvent.from_requested_event(
            event=event,
            result=result,
        )

        producer.produce_sync(
            topic=producer_topic,
            key=event.experiment_run_uuid,
            value=completed_event.to_payload(),
        )

        logger.info(
            "Experiment processing finished and completion event published. experiment_type=%s producer_topic=%s result=%s",
            event.experiment_type,
            producer_topic,
            result,
        )

    return handle_experiment_run_requested


def _handle_experiment_run_exception(
    event: ExperimentRunRequestedEvent,
    exception: BaseException,
    producer: ExperimentOpsKafkaProducer,
    failure_producer_topic: str,
) -> None:
    logger.exception(
        "Experiment processing failed. experiment_type=%s",
        event.experiment_type,
    )

    _publish_failure_event(
        event=event,
        exception=exception,
        producer=producer,
        failure_producer_topic=failure_producer_topic,
    )


def _publish_failure_event(
    event: ExperimentRunRequestedEvent,
    exception: BaseException,
    producer: ExperimentOpsKafkaProducer,
    failure_producer_topic: str,
) -> None:
    failure_event = ExperimentRunFailureEvent.from_requested_event(
        event=event,
        exception=exception,
    )

    producer.produce_sync(
        topic=failure_producer_topic,
        key=event.experiment_run_uuid,
        value=failure_event.to_payload(),
    )

    logger.info(
        "Experiment processing failed and failure event published. experiment_type=%s producer_topic=%s error_type=%s",
        event.experiment_type,
        failure_producer_topic,
        exception.__class__.__name__,
    )


def _build_execution_context(event: ExperimentRunRequestedEvent) -> ExperimentExecutionContext:
    return ExperimentExecutionContext.from_requested_event(event)
