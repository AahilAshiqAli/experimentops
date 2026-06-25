from __future__ import annotations

from experiment_runtime.kafka import ExperimentOpsKafkaProducer
from experiment_runtime.models import (
    ExperimentExecutionContext,
    ExperimentRunProgressEvent,
)


class ExperimentRunProgressPublisher:
    """Publishes experiment run progress updates for an active execution."""

    def __init__(
        self,
        producer: ExperimentOpsKafkaProducer,
        producer_topic: str,
    ) -> None:
        self._producer = producer
        self._producer_topic = producer_topic

    def publish(
        self,
        context: ExperimentExecutionContext,
        progress: int,
    ) -> None:
        """Publish a single progress update for an execution context."""

        progress_event = ExperimentRunProgressEvent.from_execution_context(
            context=context,
            progress=progress,
        )

        self._producer.produce_sync(
            topic=self._producer_topic,
            key=context.experiment_run_uuid,
            value=progress_event.to_payload(),
        )
