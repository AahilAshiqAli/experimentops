from __future__ import annotations

import logging

from experiment_runtime import KafkaSettings
from experiment_runtime.kafka import ExperimentOpsKafkaConsumer, KafkaMessage
from experiment_runtime.logging.context import ExperimentOpsLogger
from experiment_runtime.models.events import ExperimentRunRequestedEvent


logger = ExperimentOpsLogger.get_logger("analysis-worker")


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

    #
    # For now, keep it boring and prove Java -> Kafka -> Python works.
    logger.info(
        "Dummy experiment processing finished. experiment_run_uuid=%s",
        event.experiment_run_uuid,
    )


def main() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s - %(message)s",
    )

    settings = KafkaSettings.from_env()
    consumer = ExperimentOpsKafkaConsumer(settings)

    consumer.run_forever(
        handler=handle_experiment_run_requested,
        commit_on_handler_error=False,
        commit_on_deserialization_error=False,
    )


if __name__ == "__main__":
    main()
