from __future__ import annotations

import logging

from experiment_runtime import KafkaSettings
from experiment_runtime.kafka import (
    ExperimentOpsKafkaConsumer,
    ExperimentOpsKafkaProducer,
    KafkaMessage,
)
from experiment_runtime.logging.context import ExperimentOpsLogger
from experiment_runtime.models.experiment_run_completed_event import (
    ExperimentRunCompletedEvent,
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
):
    def handle_experiment_run_requested(message: KafkaMessage) -> None:
        event = ExperimentRunRequestedEvent.from_kafka_message(message)

        logger.info(
            "Received experiment run request. request_uuid=%s workspace_uuid=%s project_uuid=%s experiment_run_uuid=%s experiment_type=%s topic=%s partition=%s offset=%s",
            event.request_uuid,
            event.workspace_uuid,
            event.project_uuid,
            event.experiment_run_uuid,
            event.experiment_type,
            message.topic,
            message.partition,
            message.offset,
        )

        if not event.experiment_type:
            logger.warning(
                "Skipping experiment run request because experiment_type is missing. request_uuid=%s workspace_uuid=%s project_uuid=%s experiment_run_uuid=%s topic=%s partition=%s offset=%s",
                event.request_uuid,
                event.workspace_uuid,
                event.project_uuid,
                event.experiment_run_uuid,
                message.topic,
                message.partition,
                message.offset,
            )
            return

        if event.experiment_type.strip().upper() not in registry.supported_types():
            logger.warning(
                "Skipping experiment run request because experiment_type is unsupported. experiment_type=%s supported_experiment_types=%s request_uuid=%s workspace_uuid=%s project_uuid=%s experiment_run_uuid=%s topic=%s partition=%s offset=%s",
                event.experiment_type,
                registry.supported_types(),
                event.request_uuid,
                event.workspace_uuid,
                event.project_uuid,
                event.experiment_run_uuid,
                message.topic,
                message.partition,
                message.offset,
            )
            return

        result = registry.execute(
            experiment_type=event.experiment_type,
            context=_build_execution_context(event),
        )

        # Get experimentRunCompletedEvent and make producer call
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
            "Experiment processing finished and completion event published. experiment_run_uuid=%s experiment_type=%s producer_topic=%s result=%s",
            event.experiment_run_uuid,
            event.experiment_type,
            producer_topic,
            result,
        )

    return handle_experiment_run_requested


def _build_execution_context(event: ExperimentRunRequestedEvent) -> dict:
    return {
        "eventUuid": event.event_uuid,
        "requestUuid": event.request_uuid,
        "workspaceUuid": event.workspace_uuid,
        "projectUuid": event.project_uuid,
        "experimentUuid": event.experiment_uuid,
        "experimentRunUuid": event.experiment_run_uuid,
        "experimentType": event.experiment_type,
        "datasetUri": event.dataset_uri,
        "configJson": event.config_json
    }


def main() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s - %(message)s",
    )

    settings = KafkaSettings.from_env()
    consumer_logger = ExperimentOpsLogger.get_logger("experimentOps-logger")
    producer_logger = ExperimentOpsLogger.get_logger("experimentOps-producer")
    consumer = ExperimentOpsKafkaConsumer(settings, consumer_logger)
    producer = ExperimentOpsKafkaProducer(
        settings=settings,
        logger=producer_logger,
        value_schema_path=settings.producer_value_schema_path,
        import_paths=settings.avro_import_paths,
    )
    registry = ExperimentRegistry.discover_executors("analysis_worker.executors")

    logger.info(
        "Loaded experiment executors. supported_experiment_types=%s",
        registry.supported_types(),
    )

    try:
        consumer.run_forever(
            handler=build_experiment_run_requested_handler(
                registry=registry,
                producer=producer,
                producer_topic=settings.producer_topic,
            ),
            commit_on_handler_error=False,
            commit_on_deserialization_error=False,
        )
    finally:
        producer.close()


if __name__ == "__main__":
    main()
