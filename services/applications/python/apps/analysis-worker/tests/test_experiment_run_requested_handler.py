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


class MultiArtifactRegistry(FakeRegistry):
    def execute(
        self,
        experiment_type: str,
        context: ExperimentExecutionContext,
    ) -> Result:
        self.contexts.append(context)
        if experiment_type == "FIRST_ANALYSIS":
            return Result(
                artifact=[
                    Artifact(
                        format="csv",
                        type="UNUSED_ARTIFACT",
                        uri="s3://bucket/unused.csv",
                        size=10,
                    ),
                    Artifact(
                        format="csv",
                        type="SELECTED_ARTIFACT",
                        uri="s3://bucket/selected.csv",
                        size=10,
                    ),
                ],
                metrics=[],
            )

        return Result(
            artifact=[
                Artifact(
                    format="csv",
                    type="SECOND_ANALYSIS_ARTIFACT",
                    uri="s3://bucket/second_analysis.csv",
                    size=10,
                )
            ],
            metrics=[],
        )


class NoArtifactRegistry(FakeRegistry):
    def execute(
        self,
        experiment_type: str,
        context: ExperimentExecutionContext,
    ) -> Result:
        self.contexts.append(context)
        return Result(artifact=[], metrics=[])


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
                    "executionPlan": {
                        "schemaVersion": 1,
                        "steps": [
                            {
                                "stepCount": 2,
                                "experimentConfigUuid": "config-2",
                                "experimentType": "SECOND_ANALYSIS",
                                "experimentConfigJson": {"second": True},
                                "timeWeight": 13,
                                "inputs": [
                                    {
                                        "portName": "cleanedData",
                                        "inputType": "ARTIFACT",
                                        "dataKind": "TABULAR_DATASET",
                                        "format": "CSV",
                                        "sourceStepCount": 1,
                                        "artifactName": "FIRST_ANALYSIS_ARTIFACT",
                                    }
                                ],
                                "outputs": [
                                    {
                                        "name": "SECOND_ANALYSIS_ARTIFACT",
                                        "dataKind": "REPORT",
                                        "formatStrategy": "FIXED",
                                        "format": "CSV",
                                        "downStreamPolicy": "TERMINAL",
                                    }
                                ],
                            },
                            {
                                "stepCount": 1,
                                "experimentConfigUuid": "config-1",
                                "experimentType": "FIRST_ANALYSIS",
                                "experimentConfigJson": {"first": True},
                                "timeWeight": 5,
                                "inputs": [
                                    {
                                        "portName": "dataset",
                                        "inputType": "DATASET",
                                        "dataKind": "TABULAR_DATASET",
                                        "format": "CSV",
                                        "datasetVersionUuid": "dataset-version-1",
                                        "datasetUri": "s3://bucket/input.csv",
                                    }
                                ],
                                "outputs": [
                                    {
                                        "name": "FIRST_ANALYSIS_ARTIFACT",
                                        "dataKind": "TABULAR_DATASET",
                                        "formatStrategy": "SAME_AS_INPUT",
                                        "format": "CSV",
                                        "sourceInputPort": "dataset",
                                        "downStreamPolicy": "CONNECTABLE",
                                    }
                                ],
                            },
                        ],
                    },
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
    assert registry.contexts[0].inputs["dataset"].dataset_version_uuid == "dataset-version-1"
    assert registry.contexts[1].inputs["cleanedData"].artifact_name == "FIRST_ANALYSIS_ARTIFACT"
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
            "format": "CSV",
            "type": "FIRST_ANALYSIS_ARTIFACT",
            "uri": "s3://bucket/first_analysis.csv",
            "size": 10,
            "experimentType": "FIRST_ANALYSIS",
        },
        {
            "format": "CSV",
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


def test_handler_does_not_parse_config_json_without_execution_plan() -> None:
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
                            "stepCount": 1,
                            "experimentType": "FIRST_ANALYSIS",
                            "experimentConfigJson": {"first": True},
                        }
                    ],
                },
            },
            headers={},
            timestamp_millis=None,
        )
    )

    assert registry.contexts == []
    assert producer.produced == []
    assert failure_producer.produced == []


def test_handler_resolves_named_artifact_instead_of_first_previous_artifact() -> None:
    registry = MultiArtifactRegistry()
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
                    "executionPlan": {
                        "schemaVersion": 1,
                        "steps": [
                            {
                                "stepCount": 1,
                                "experimentConfigUuid": "config-1",
                                "experimentType": "FIRST_ANALYSIS",
                                "experimentConfigJson": {"first": True},
                                "inputs": [
                                    {
                                        "portName": "dataset",
                                        "inputType": "DATASET",
                                        "dataKind": "TABULAR_DATASET",
                                        "format": "CSV",
                                        "datasetVersionUuid": "dataset-version-1",
                                        "datasetUri": "s3://bucket/input.csv",
                                    }
                                ],
                                "outputs": [
                                    {
                                        "name": "UNUSED_ARTIFACT",
                                        "dataKind": "REPORT",
                                        "formatStrategy": "FIXED",
                                        "format": "CSV",
                                        "downStreamPolicy": "CONNECTABLE",
                                    },
                                    {
                                        "name": "SELECTED_ARTIFACT",
                                        "dataKind": "TABULAR_DATASET",
                                        "formatStrategy": "FIXED",
                                        "format": "CSV",
                                        "downStreamPolicy": "CONNECTABLE",
                                    },
                                ],
                            },
                            {
                                "stepCount": 2,
                                "experimentConfigUuid": "config-2",
                                "experimentType": "SECOND_ANALYSIS",
                                "experimentConfigJson": {"second": True},
                                "inputs": [
                                    {
                                        "portName": "selectedData",
                                        "inputType": "ARTIFACT",
                                        "dataKind": "TABULAR_DATASET",
                                        "format": "CSV",
                                        "sourceStepCount": 1,
                                        "artifactName": "SELECTED_ARTIFACT",
                                    }
                                ],
                                "outputs": [
                                    {
                                        "name": "SECOND_ANALYSIS_ARTIFACT",
                                        "dataKind": "REPORT",
                                        "formatStrategy": "FIXED",
                                        "format": "CSV",
                                        "downStreamPolicy": "TERMINAL",
                                    }
                                ],
                            },
                        ],
                    },
                },
            },
            headers={},
            timestamp_millis=None,
        )
    )

    assert failure_producer.produced == []
    assert len(producer.produced) == 1
    assert registry.contexts[1].dataset_uri == "s3://bucket/selected.csv"
    assert registry.contexts[1].inputs["selectedData"].uri == "s3://bucket/selected.csv"


def test_handler_allows_missing_optional_output() -> None:
    registry = NoArtifactRegistry()
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
                    "executionPlan": {
                        "schemaVersion": 1,
                        "steps": [
                            {
                                "stepCount": 1,
                                "experimentConfigUuid": "config-1",
                                "experimentType": "FIRST_ANALYSIS",
                                "inputs": [
                                    {
                                        "portName": "dataset",
                                        "inputType": "DATASET",
                                        "dataKind": "TABULAR_DATASET",
                                        "format": "CSV",
                                        "datasetVersionUuid": "dataset-version-1",
                                        "datasetUri": "s3://bucket/input.csv",
                                    }
                                ],
                                "outputs": [
                                    {
                                        "name": "optionalDebugData",
                                        "dataKind": "TABULAR_DATASET",
                                        "formatStrategy": "SAME_AS_INPUT",
                                        "format": "CSV",
                                        "sourceInputPort": "dataset",
                                        "requiredForRun": False,
                                        "downStreamPolicy": "CONNECTABLE",
                                    }
                                ],
                            }
                        ],
                    },
                },
            },
            headers={},
            timestamp_millis=None,
        )
    )

    assert failure_producer.produced == []
    assert len(producer.produced) == 1
    assert producer.produced[0]["value"]["payload"]["result"]["artifact"] == []
