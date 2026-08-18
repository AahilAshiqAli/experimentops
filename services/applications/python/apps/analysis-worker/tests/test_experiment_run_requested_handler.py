from __future__ import annotations

import json

from analysis_worker.handlers.experiment_run_progress_publisher import (
    ExperimentRunProgressPublisher,
)
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


class CsvReportAliasRegistry(FakeRegistry):
    def supported_types(self) -> list[str]:
        return ["CSV_PROFILE_ANALYSIS"]

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
                    type="CLEANED_DATASET",
                    uri="s3://bucket/cleaned.csv",
                    size=10,
                ),
                Artifact(
                    format="json",
                    type="CLEANING_REPORT",
                    uri="s3://bucket/report.json",
                    size=20,
                ),
            ],
            metrics=[FakeMetric(rows_processed=100)],
        )


class LoggingRegistry(FakeRegistry):
    def __init__(self, progress_publisher: ExperimentRunProgressPublisher) -> None:
        super().__init__()
        self.progress_publisher = progress_publisher

    def supported_types(self) -> list[str]:
        return ["CSV_CLEANING"]

    def execute(
        self,
        experiment_type: str,
        context: ExperimentExecutionContext,
    ) -> Result:
        self.contexts.append(context)
        assert context.run_log_sink is not None
        context.run_log_sink.info(experiment_type, "Loaded CSV data.")
        context.run_log_sink.warning(experiment_type, "Internal dtype warning.")
        self.progress_publisher.publish(context, 25)
        context.run_log_sink.info(experiment_type, "Finished cleaning CSV data.")
        return Result(
            artifact=[
                Artifact(
                    format="csv",
                    type="CLEANED_DATASET",
                    uri="s3://bucket/cleaned.csv",
                    size=10,
                )
            ],
            metrics=[],
        )


class FailingRegistry(FakeRegistry):
    def execute(
        self,
        experiment_type: str,
        context: ExperimentExecutionContext,
    ) -> Result:
        self.contexts.append(context)
        if experiment_type == "SECOND_ANALYSIS":
            raise RuntimeError("second step failed")
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


class FakeObjectStorage:
    def __init__(self) -> None:
        self.uploads: dict[str, bytes] = {}

    def upload_file(self, key: str, content: bytes) -> str:
        self.uploads[key] = content
        return f"s3://logs-bucket/{key}"

    def download_file(self, key: str) -> bytes:
        return self.uploads[key]

    def delete_file(self, key: str) -> None:
        del self.uploads[key]


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
            "type": "TABULAR_DATASET",
            "uri": "s3://bucket/first_analysis.csv",
            "size": 10,
            "experimentType": "FIRST_ANALYSIS",
            "stepCount": 1,
            "portName": "FIRST_ANALYSIS_ARTIFACT",
        },
        {
            "format": "CSV",
            "type": "REPORT",
            "uri": "s3://bucket/second_analysis.csv",
            "size": 10,
            "experimentType": "SECOND_ANALYSIS",
            "stepCount": 2,
            "portName": "SECOND_ANALYSIS_ARTIFACT",
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


def test_handler_uploads_run_log_file_and_publishes_sequence_logs(tmp_path) -> None:
    progress_producer = FakeProducer()
    progress_publisher = ExperimentRunProgressPublisher(
        producer=progress_producer,
        producer_topic="progress-topic",
    )
    registry = LoggingRegistry(progress_publisher)
    producer = FakeProducer()
    failure_producer = FakeProducer()
    object_storage = FakeObjectStorage()
    handler = build_experiment_run_requested_handler(
        registry=registry,
        producer=producer,
        producer_topic="completed-topic",
        failure_producer=failure_producer,
        failure_producer_topic="failure-topic",
        progress_publisher=progress_publisher,
        object_storage=object_storage,
        log_root_dir=tmp_path,
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
                                    "stepCount": 4,
                                    "experimentConfigUuid": "config-1",
                                    "experimentType": "CSV_CLEANING",
                                    "inputs": [],
                                    "outputs": [
                                        {
                                            "name": "CLEANED_DATASET",
                                            "dataKind": "TABULAR_DATASET",
                                            "formatStrategy": "FIXED",
                                            "format": "CSV",
                                            "downStreamPolicy": "TERMINAL",
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
    assert len(progress_producer.produced) == 2
    first_progress = progress_producer.produced[0]["value"]["payload"]
    final_progress = progress_producer.produced[1]["value"]["payload"]
    assert first_progress["currentStep"] == 4
    assert first_progress["sequence"] == 1
    assert first_progress["logs"][0]["message"] == "Loaded CSV data."
    assert [log["message"] for log in final_progress["logs"]] == [
        "Finished cleaning CSV data."
    ]
    assert final_progress["progress"] == "100"
    assert final_progress["sequence"] == 2

    completed_payload = producer.produced[0]["value"]["payload"]
    log_file_url = completed_payload["logFileUrl"]
    assert log_file_url == (
        "s3://logs-bucket/workspaces/workspace-1/projects/project-1/"
        "experiments/experiment-1/runs/run-1/logs/run.jsonl"
    )
    uploaded_log = next(iter(object_storage.uploads.values())).decode()
    uploaded_records = [
        json.loads(line)
        for line in uploaded_log.splitlines()
    ]
    assert [record["message"] for record in uploaded_records] == [
        "Loaded CSV data.",
        "Internal dtype warning.",
        "Finished cleaning CSV data.",
    ]


def test_handler_failure_log_uses_failing_step_experiment_type(tmp_path) -> None:
    progress_producer = FakeProducer()
    progress_publisher = ExperimentRunProgressPublisher(
        producer=progress_producer,
        producer_topic="progress-topic",
    )
    registry = FailingRegistry()
    producer = FakeProducer()
    failure_producer = FakeProducer()
    object_storage = FakeObjectStorage()
    handler = build_experiment_run_requested_handler(
        registry=registry,
        producer=producer,
        producer_topic="completed-topic",
        failure_producer=failure_producer,
        failure_producer_topic="failure-topic",
        progress_publisher=progress_publisher,
        object_storage=object_storage,
        log_root_dir=tmp_path,
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
                                "experimentType": "FIRST_ANALYSIS",
                                "inputs": [],
                                "outputs": [],
                            },
                            {
                                "stepCount": 2,
                                "experimentType": "SECOND_ANALYSIS",
                                "inputs": [],
                                "outputs": [],
                            },
                        ],
                    },
                },
            },
            headers={},
            timestamp_millis=None,
        )
    )

    assert producer.produced == []
    assert len(failure_producer.produced) == 1
    assert len(progress_producer.produced) == 1
    failure_log = progress_producer.produced[0]["value"]["payload"]["logs"][0]
    assert failure_log["experimentType"] == "SECOND_ANALYSIS"
    assert failure_log["message"] == "Experiment processing failed: second step failed"

    uploaded_log = next(iter(object_storage.uploads.values())).decode()
    uploaded_record = json.loads(uploaded_log)
    assert uploaded_record["experimentType"] == "SECOND_ANALYSIS"


def test_handler_maps_csv_report_artifact_to_training_report_output() -> None:
    registry = CsvReportAliasRegistry()
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
                                "experimentType": "CSV_PROFILE_ANALYSIS",
                                "inputs": [],
                                "outputs": [
                                    {
                                        "name": "cleanedTrainData",
                                        "dataKind": "TABULAR_DATASET",
                                        "type": {
                                            "type": "SAME_AS_INPUT",
                                            "format": None,
                                            "sourceInputPort": "trainData",
                                        },
                                        "required": False,
                                        "downStreamPolicy": "CONNECTABLE",
                                    },
                                    {
                                        "name": "trainingReport",
                                        "dataKind": "REPORT",
                                        "type": {
                                            "type": "FIXED",
                                            "format": "JSON",
                                            "sourceInputPort": None,
                                        },
                                        "required": True,
                                        "downStreamPolicy": "TERMINAL",
                                    },
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
    assert producer.produced[0]["value"]["payload"]["result"]["artifact"] == [
        {
            "format": "csv",
            "type": "TABULAR_DATASET",
            "uri": "s3://bucket/cleaned.csv",
            "size": 10,
            "experimentType": "CSV_PROFILE_ANALYSIS",
            "stepCount": 1,
            "portName": "cleanedTrainData",
        },
        {
            "format": "JSON",
            "type": "REPORT",
            "uri": "s3://bucket/report.json",
            "size": 20,
            "experimentType": "CSV_PROFILE_ANALYSIS",
            "stepCount": 1,
            "portName": "trainingReport",
        },
    ]


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
