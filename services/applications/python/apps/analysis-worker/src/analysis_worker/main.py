from __future__ import annotations

import logging

from experiment_runtime import KafkaSettings
from experiment_runtime.kafka import (
    ExperimentOpsKafkaConsumer,
    ExperimentOpsKafkaProducer,
)
from experiment_runtime.logging.context import ExperimentOpsLogger
from experiment_runtime.registry import ExperimentRegistry

from analysis_worker.config import ObjectStorageConfig
from analysis_worker.handlers.experiment_run_requested_handler import (
    build_experiment_run_requested_handler,
)
from analysis_worker.handlers.experiment_run_progress_publisher import (
    ExperimentRunProgressPublisher,
)


logger = ExperimentOpsLogger.get_logger("analysis-worker")


def main() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s - %(message)s",
    )

    settings = KafkaSettings.from_env()
    object_storage = ObjectStorageConfig.from_env().create_object_storage()
    consumer_logger = ExperimentOpsLogger.get_logger("experimentOps-logger")
    producer_logger = ExperimentOpsLogger.get_logger("experimentOps-producer")
    consumer = ExperimentOpsKafkaConsumer(settings, consumer_logger)
    producer = ExperimentOpsKafkaProducer(
        settings=settings,
        logger=producer_logger,
        value_schema_path=settings.producer_value_schema_path,
        import_paths=settings.avro_import_paths,
    )
    failure_producer = ExperimentOpsKafkaProducer(
        settings=settings,
        logger=producer_logger,
        value_schema_path=settings.failure_producer_value_schema_path,
        import_paths=settings.avro_import_paths,
    )
    progress_producer = ExperimentOpsKafkaProducer(
        settings=settings,
        logger=producer_logger,
        value_schema_path=settings.progress_producer_value_schema_path,
        import_paths=settings.avro_import_paths,
    )
    progress_publisher = ExperimentRunProgressPublisher(
        producer=progress_producer,
        producer_topic=settings.progress_producer_topic,
    )
    registry = ExperimentRegistry.discover_executors(
        "analysis_worker.executors",
        executor_dependencies={
            "object_storage": object_storage,
            "progress_publisher": progress_publisher,
        },
    )

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
                failure_producer=failure_producer,
                failure_producer_topic=settings.failure_producer_topic,
            ),
            commit_on_handler_error=False,
            commit_on_deserialization_error=False,
        )
    finally:
        producer.close()
        failure_producer.close()
        progress_producer.close()


if __name__ == "__main__":
    main()
