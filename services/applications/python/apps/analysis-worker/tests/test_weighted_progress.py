from __future__ import annotations

from analysis_worker.handlers.experiment_run_progress_publisher import (
    ExperimentRunProgressPublisher,
)
from analysis_worker.handlers.weighted_progress import WeightedProgressCalculator
from experiment_runtime.logging.run_sink import ExperimentRunLogSink
from experiment_runtime.models import ExperimentExecutionContext


class FakeProducer:
    def __init__(self) -> None:
        self.values: list[dict[str, object]] = []

    def produce_sync(
        self,
        topic: str,
        key: str | None,
        value: dict[str, object],
    ) -> None:
        self.values.append(value)


def test_weighted_progress_calculator_converts_local_to_global_progress() -> None:
    first_step = WeightedProgressCalculator(
        completed_weight=0,
        current_step_weight=5,
        total_weight=18,
    )
    second_step = WeightedProgressCalculator(
        completed_weight=5,
        current_step_weight=13,
        total_weight=18,
    )

    assert first_step.calculate(50) == 13.89
    assert first_step.calculate(100) == 27.78
    assert second_step.calculate(50) == 63.89
    assert second_step.calculate(100) == 100


def test_weighted_progress_calculator_clamps_global_progress() -> None:
    calculator = WeightedProgressCalculator(
        completed_weight=0,
        current_step_weight=5,
        total_weight=18,
    )

    assert calculator.calculate(-10) == 0
    assert calculator.calculate(150) == 27.78


def test_progress_publisher_publishes_weighted_global_progress(tmp_path) -> None:
    producer = FakeProducer()
    publisher = ExperimentRunProgressPublisher(
        producer=producer,
        producer_topic="progress-topic",
    )
    run_log_sink = ExperimentRunLogSink(tmp_path / "run.log.jsonl")
    run_log_sink.info("BINARY_CLASSIFICATION", "Prepared model inputs.")
    run_log_sink.warning("BINARY_CLASSIFICATION", "Debug-only warning.")
    context = ExperimentExecutionContext(
        event_uuid="event-1",
        request_uuid="request-1",
        workspace_uuid="workspace-1",
        project_uuid="project-1",
        experiment_uuid="experiment-1",
        experiment_run_uuid="run-1",
        experiment_type="BINARY_CLASSIFICATION",
        dataset_uri="s3://bucket/cleaned.csv",
        config_json=None,
        progress_completed_weight=5,
        progress_step_weight=13,
        progress_total_weight=18,
        current_step=2,
        run_log_sink=run_log_sink,
    )

    publisher.publish(context, 50)

    assert producer.values[0]["payload"]["progress"] == "64"
    assert producer.values[0]["payload"]["currentStep"] == 2
    assert producer.values[0]["payload"]["sequence"] == 1
    assert producer.values[0]["payload"]["logs"] == [
        {
            "timestamp": producer.values[0]["payload"]["logs"][0]["timestamp"],
            "level": "INFO",
            "experimentType": "BINARY_CLASSIFICATION",
            "message": "Prepared model inputs.",
        }
    ]

    publisher.publish(context, 100)

    assert producer.values[1]["payload"]["sequence"] == 2
    assert producer.values[1]["payload"]["logs"] == []
