from __future__ import annotations

from experiment_runtime.kafka import ExperimentOpsKafkaProducer
from experiment_runtime.models import (
    ExperimentExecutionContext,
    ExperimentRunProgressEvent,
)

from analysis_worker.handlers.weighted_progress import WeightedProgressCalculator


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
        progress: int | float,
    ) -> None:
        """Publish a single progress update for an execution context."""

        global_progress = _global_progress(context, progress)
        progress_event = ExperimentRunProgressEvent.from_execution_context(
            context=context,
            progress=global_progress,
        )

        self._producer.produce_sync(
            topic=self._producer_topic,
            key=context.experiment_run_uuid,
            value=progress_event.to_payload(),
        )


def _global_progress(
    context: ExperimentExecutionContext,
    local_progress: int | float,
) -> float:
    if (
        context.progress_completed_weight is None
        or context.progress_step_weight is None
        or context.progress_total_weight is None
    ):
        return float(local_progress)

    return WeightedProgressCalculator(
        completed_weight=context.progress_completed_weight,
        current_step_weight=context.progress_step_weight,
        total_weight=context.progress_total_weight,
    ).calculate(local_progress)
