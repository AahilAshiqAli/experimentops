from __future__ import annotations

from analysis_worker.handlers.experiment_run_requested_handler import (
    build_experiment_run_requested_handler,
)
from experiment_runtime.kafka import KafkaMessage
from experiment_runtime.models import (
    Artifact,
    ExperimentExecutionContext,
    Metric,
    Result,
)


class FakeMetric(Metric):
    rows_processed: int


class FakeRegistry:
    def __init__(self) -> None:
        self.contexts: list[ExperimentExecutionContext] = []

    def supported_types(self) -> list[str]:
        return ["FIRST_ANALYSIS", "SECOND_ANALYSIS"]

    def execute(
        self,
        experiment_type: str,
        context: ExperimentExecutionContext,
    ) -> Result:
        self.contexts.append(context)
        return Result(
            artifact=[
                Artifact(
                    format="csv",
                    type=f"{experiment_type}_ARTIFACT",
                    uri=f"s3://bucket/{experiment_type.lower()}.csv",
                    size=10,
                )
            ],
            metrics=[FakeMetric(rows_processed=len(self.contexts) * 100)],
        )


class FakeProducer:
    def __init__(self) -> None:
        self.produced: list[dict[str, object]] = []

    def produce_sync(
        self,
        topic: str,
        key: str | None,
        value: dict[str, object],
    ) -> None:
        self.produced.append({"topic": topic, "key": key, "value": value})


def test_handler_executes_configs_as_dataset_pipeline() -> None:
    registry = FakeRegistry()
    producer = FakeProducer()
    failure_producer = FakeProducer()
    handler = build_experiment_run_requested_handler(
        registry=registry,
        producer=producer,
        producer_topic="completed-topic",
        failure_producer=failure_producer,
        failure_producer_topic="failure-topic",
    )

    handler(
        KafkaMessage(
            topic="requested-topic",
            partition=0,
            offset=1,
            key="run-1",
            value={
                "metadata": {
                    "eventUuid": "event-1",
                    "traceUuid": "request-1",
                    "requesterUuid": "user-1",
                    "uuid": "run-1",
                    "workspaceUuid": "workspace-1",
                },
                "payload": {
                    "projectUuid": "project-1",
                    "experimentUuid": "experiment-1",
                    "datasetUri": "s3://bucket/input.csv",
                    "configJson": [
                        {
                            "stepCount": 2,
                            "experimentType": "SECOND_ANALYSIS",
                            "experimentConfigJson": {"second": True},
                            "timeWeight": 13,
                        },
                        {
                            "stepCount": 1,
                            "experimentType": "FIRST_ANALYSIS",
                            "experimentConfigJson": {"first": True},
                            "timeWeight": 5,
                        },
                    ],
                },
            },
            headers={},
            timestamp_millis=None,
        )
    )

    assert [context.experiment_type for context in registry.contexts] == [
        "FIRST_ANALYSIS",
        "SECOND_ANALYSIS",
    ]
    assert registry.contexts[0].dataset_uri == "s3://bucket/input.csv"
    assert registry.contexts[1].dataset_uri == "s3://bucket/first_analysis.csv"
    assert registry.contexts[1].previous_result is not None
    assert len(registry.contexts[1].pipeline_results) == 1
    assert registry.contexts[0].progress_completed_weight == 0
    assert registry.contexts[0].progress_step_weight == 5
    assert registry.contexts[0].progress_total_weight == 18
    assert registry.contexts[1].progress_completed_weight == 5
    assert registry.contexts[1].progress_step_weight == 13
    assert registry.contexts[1].progress_total_weight == 18

    assert failure_producer.produced == []
    assert len(producer.produced) == 1

    payload = producer.produced[0]["value"]["payload"]
    assert payload["experimentType"] == "FIRST_ANALYSIS,SECOND_ANALYSIS"
    assert payload["result"]["artifact"] == [
        {
            "format": "csv",
            "type": "FIRST_ANALYSIS_ARTIFACT",
            "uri": "s3://bucket/first_analysis.csv",
            "size": 10,
            "experimentType": "FIRST_ANALYSIS",
        },
        {
            "format": "csv",
            "type": "SECOND_ANALYSIS_ARTIFACT",
            "uri": "s3://bucket/second_analysis.csv",
            "size": 10,
            "experimentType": "SECOND_ANALYSIS",
        },
    ]
    assert payload["result"]["metrics"] == [
        {
            "experimentType": "FIRST_ANALYSIS",
            "metricsJson": '{"rowsProcessed":100}',
        },
        {
            "experimentType": "SECOND_ANALYSIS",
            "metricsJson": '{"rowsProcessed":200}',
        },
    ]
    assert payload["result"]["metricsJson"] == (
        '[{"experimentType":"FIRST_ANALYSIS","metricsJson":{"rowsProcessed":100}},'
        '{"experimentType":"SECOND_ANALYSIS","metricsJson":{"rowsProcessed":200}}]'
    )
