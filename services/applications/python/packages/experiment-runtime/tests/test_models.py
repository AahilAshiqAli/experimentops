from __future__ import annotations

from experiment_runtime.models import (
    Artifact,
    ExperimentExecutionContext,
    ExperimentRunExecutionConfig,
    ExperimentRunExecutionPlan,
    ExperimentRunExecutionPlanInput,
    ExperimentRunExecutionPlanOutput,
    ExperimentRunCompletedEvent,
    ExperimentRunFailureErrorEntry,
    ExperimentRunFailureEvent,
    ExperimentRunProgressEvent,
    ExperimentRunUserLogRecord,
    ExperimentRunRequestedEvent,
    Metric,
    Result,
)


class ExampleMetric(Metric):
    rows_processed: int


def test_models_dump_camel_case_aliases() -> None:
    event = ExperimentRunRequestedEvent(
        event_uuid="event-1",
        request_uuid="request-1",
        requester_uuid="user-1",
        workspace_uuid="workspace-1",
        project_uuid="project-1",
        experiment_uuid="experiment-1",
        experiment_run_uuid="run-1",
        experiment_type="CSV_PROFILE_ANALYSIS",
        dataset_uri="s3://bucket/input.csv",
        config_json={"sampleSize": 100},
        request_timestamp=123,
        user_role="OWNER",
    )

    assert event.model_dump(by_alias=True)["experimentRunUuid"] == "run-1"
    assert event.model_dump(by_alias=True)["datasetUri"] == "s3://bucket/input.csv"


def test_completed_event_payload_keeps_event_envelope_shape() -> None:
    requested_event = ExperimentRunRequestedEvent.from_payload(
        {
            "metadata": {
                "eventUuid": "event-1",
                "traceUuid": "request-1",
                "requesterUuid": "user-1",
                "uuid": "run-1",
                "workspaceUuid": "workspace-1",
                "requestTimestamp": 123,
                "userRole": "OWNER",
            },
            "payload": {
                "projectUuid": "project-1",
                "experimentUuid": "experiment-1",
                "experimentType": "CSV_PROFILE_ANALYSIS",
                "datasetUri": "s3://bucket/input.csv",
                "configJson": {"sampleSize": 100},
            },
        }
    )

    payload = ExperimentRunCompletedEvent.from_requested_event(
        event=requested_event,
        result=Result(
            artifact=[
                Artifact(
                    format="csv",
                    type="CLEANED_DATASET",
                    uri="s3://bucket/output.csv",
                    size=10,
                    step_count=1,
                    port_name="CLEANED_DATASET",
                )
            ],
            metrics=[ExampleMetric(rows_processed=100)],
        ),
        log_file_url="s3://bucket/run.log",
    ).to_payload()

    assert payload["metadata"]["traceUuid"] == "request-1"
    assert payload["metadata"]["uuid"] == "run-1"
    assert payload["payload"]["experimentType"] == "CSV_PROFILE_ANALYSIS"
    assert payload["payload"]["logFileUrl"] == "s3://bucket/run.log"
    assert payload["payload"]["result"] == {
        "artifact": [
            {
                "format": "csv",
                "type": "CLEANED_DATASET",
                "uri": "s3://bucket/output.csv",
                "size": 10,
                "stepCount": 1,
                "portName": "CLEANED_DATASET",
            }
        ],
        "metrics": [
            {
                "experimentType": None,
                "metricsJson": '{"rowsProcessed":100}',
            }
        ],
        "metricsJson": (
            '[{"experimentType":null,"metricsJson":{"rowsProcessed":100}}]'
        ),
    }


def test_execution_context_builds_from_requested_event() -> None:
    requested_event = ExperimentRunRequestedEvent.from_payload(
        {
            "metadata": {
                "eventUuid": "event-1",
                "traceUuid": "request-1",
                "uuid": "run-1",
                "workspaceUuid": "workspace-1",
            },
            "payload": {
                "projectUuid": "project-1",
                "experimentUuid": "experiment-1",
                "experimentType": "CSV_PROFILE_ANALYSIS",
                "datasetUri": "s3://bucket/input.csv",
                "configJson": {"sampleSize": 100},
            },
        }
    )

    context = ExperimentExecutionContext.from_requested_event(requested_event)

    assert context.model_dump(by_alias=True) == {
        "eventUuid": "event-1",
        "requestUuid": "request-1",
        "workspaceUuid": "workspace-1",
        "projectUuid": "project-1",
        "experimentUuid": "experiment-1",
        "experimentRunUuid": "run-1",
        "experimentType": "CSV_PROFILE_ANALYSIS",
        "datasetUri": "s3://bucket/input.csv",
        "configJson": {"sampleSize": 100},
        "inputs": {},
    }


def test_run_requested_event_parses_execution_configs_from_execution_plan() -> None:
    requested_event = ExperimentRunRequestedEvent.from_payload(
        {
            "metadata": {
                "eventUuid": "event-1",
                "traceUuid": "request-1",
                "uuid": "run-1",
                "workspaceUuid": "workspace-1",
            },
            "payload": {
                "projectUuid": "project-1",
                "experimentUuid": "experiment-1",
                "datasetUri": "s3://bucket/input.csv",
                "configJson": [
                    {
                        "stepCount": 99,
                        "experimentType": "IGNORED_LEGACY_CONFIG",
                    }
                ],
                "executionPlan": {
                    "schemaVersion": 1,
                    "steps": [
                        {
                            "stepCount": 1,
                            "experimentConfigUuid": "config-1",
                            "experimentType": "CSV_PROFILE_ANALYSIS",
                            "experimentConfigJson": {"sampleSize": 100},
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
                                    "name": "CLEANED_DATASET",
                                    "dataKind": "TABULAR_DATASET",
                                    "formatStrategy": "SAME_AS_INPUT",
                                    "format": "CSV",
                                    "sourceInputPort": "dataset",
                                    "requiredForRun": True,
                                    "downStreamPolicy": "CONNECTABLE",
                                }
                            ],
                        }
                    ],
                },
            },
        }
    )

    assert requested_event.experiment_type is None
    assert requested_event.execution_plan == ExperimentRunExecutionPlan(
        schema_version=1,
        steps=requested_event.execution_configs,
    )
    assert requested_event.execution_configs == (
        ExperimentRunExecutionConfig(
            step_count=1,
            experiment_config_uuid="config-1",
            experiment_type="CSV_PROFILE_ANALYSIS",
            experiment_config_json={"sampleSize": 100},
            time_weight=5,
            inputs=(
                ExperimentRunExecutionPlanInput(
                    port_name="dataset",
                    input_type="DATASET",
                    data_kind="TABULAR_DATASET",
                    format="CSV",
                    dataset_version_uuid="dataset-version-1",
                    dataset_uri="s3://bucket/input.csv",
                ),
            ),
            outputs=(
                ExperimentRunExecutionPlanOutput(
                    name="CLEANED_DATASET",
                    data_kind="TABULAR_DATASET",
                    format_strategy="SAME_AS_INPUT",
                    format="CSV",
                    source_input_port="dataset",
                    required_for_run=True,
                    down_stream_policy="CONNECTABLE",
                ),
            ),
        ),
    )

    context = ExperimentExecutionContext.from_requested_event(
        requested_event,
        requested_event.execution_configs[0],
    )

    assert context.experiment_type == "CSV_PROFILE_ANALYSIS"
    assert context.config_json == {"sampleSize": 100}


def test_run_requested_event_decodes_execution_plan_config_json_string() -> None:
    requested_event = ExperimentRunRequestedEvent.from_payload(
        {
            "metadata": {"uuid": "run-1"},
            "payload": {
                "executionPlan": {
                    "schemaVersion": 1,
                    "steps": [
                        {
                            "stepCount": 1,
                            "experimentType": "TABULAR_TRAIN_TEST_SPLIT",
                            "experimentConfigJson": '{"testSize": 0.2, "shuffle": true}',
                        }
                    ],
                }
            },
        }
    )

    context = ExperimentExecutionContext.from_requested_event(
        requested_event,
        requested_event.execution_configs[0],
    )

    assert context.config_json == {"testSize": 0.2, "shuffle": True}


def test_run_requested_event_parses_nested_execution_plan_contracts() -> None:
    requested_event = ExperimentRunRequestedEvent.from_payload(
        {
            "metadata": {
                "eventUuid": "event-1",
                "traceUuid": "request-1",
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
                            "experimentType": "CSV_PROFILE_ANALYSIS",
                            "inputs": [
                                {
                                    "portName": "trainData",
                                    "required": True,
                                    "contract": {
                                        "dataKind": "TABULAR_DATASET",
                                        "acceptedFormats": ["CSV", "PARQUET"],
                                    },
                                }
                            ],
                            "outputs": [
                                {
                                    "name": "cleanedTrainData",
                                    "type": {
                                        "type": "SAME_AS_INPUT",
                                        "format": None,
                                        "sourceInputPort": "trainData",
                                    },
                                    "dataKind": "TABULAR_DATASET",
                                    "required": False,
                                    "downStreamPolicy": "CONNECTABLE",
                                },
                                {
                                    "name": "trainingReport",
                                    "type": {
                                        "type": "FIXED",
                                        "format": "JSON",
                                        "sourceInputPort": None,
                                    },
                                    "dataKind": "REPORT",
                                    "required": True,
                                    "downStreamPolicy": "TERMINAL",
                                },
                            ],
                        }
                    ],
                },
            },
        }
    )

    step = requested_event.execution_configs[0]

    assert step.inputs[0].data_kind == "TABULAR_DATASET"
    assert step.inputs[0].format == "CSV"
    assert step.outputs[0].format_strategy == "SAME_AS_INPUT"
    assert step.outputs[0].source_input_port == "trainData"
    assert step.outputs[0].required_for_run is False
    assert step.outputs[1].format_strategy == "FIXED"
    assert step.outputs[1].format == "JSON"
    assert step.outputs[1].required_for_run is True


def test_progress_event_payload_keeps_event_envelope_shape() -> None:
    context = ExperimentExecutionContext(
        event_uuid="event-1",
        request_uuid="request-1",
        workspace_uuid="workspace-1",
        project_uuid="project-1",
        experiment_uuid="experiment-1",
        experiment_run_uuid="run-1",
        experiment_type="CSV_PROFILE_ANALYSIS",
        dataset_uri="s3://bucket/input.csv",
        config_json=None,
    )

    payload = ExperimentRunProgressEvent.from_execution_context(
        context=context,
        progress=75,
        current_step=2,
        sequence=3,
        logs=[
            ExperimentRunUserLogRecord(
                timestamp=123456,
                level="INFO",
                experiment_type="CSV_PROFILE_ANALYSIS",
                message="Loaded CSV.",
            )
        ],
    ).to_payload()

    assert payload["metadata"]["traceUuid"] == "request-1"
    assert payload["metadata"]["eventType"] == "EXPERIMENT_RUN_PROGRESS"
    assert payload["metadata"]["uuid"] == "run-1"
    assert payload["metadata"]["workspaceUuid"] == "workspace-1"
    assert payload["payload"] == {
        "experimentUuid": "experiment-1",
        "experimentRunUuid": "run-1",
        "progress": "75",
        "currentStep": 2,
        "sequence": 3,
        "logs": [
            {
                "timestamp": 123456,
                "level": "INFO",
                "experimentType": "CSV_PROFILE_ANALYSIS",
                "message": "Loaded CSV.",
            }
        ],
    }

    fractional_payload = ExperimentRunProgressEvent.from_execution_context(
        context=context,
        progress=63.89,
    ).to_payload()

    assert fractional_payload["payload"]["progress"] == "64"


def test_failure_error_entry_payload_uses_camel_case_aliases() -> None:
    error = ExperimentRunFailureErrorEntry(
        error_type="RuntimeError",
        error_message="failed",
        stack_trace="trace",
    )

    assert error.to_payload() == {
        "errorType": "RuntimeError",
        "errorMessage": "failed",
        "stackTrace": "trace",
    }


def test_failure_event_payload_includes_log_file_url() -> None:
    requested_event = ExperimentRunRequestedEvent.from_payload(
        {
            "metadata": {
                "eventUuid": "event-1",
                "traceUuid": "request-1",
                "requesterUuid": "user-1",
                "uuid": "run-1",
                "workspaceUuid": "workspace-1",
                "requestTimestamp": 123,
                "userRole": "OWNER",
            },
            "payload": {
                "projectUuid": "project-1",
                "experimentUuid": "experiment-1",
                "experimentType": "CSV_PROFILE_ANALYSIS",
                "datasetUri": "s3://bucket/input.csv",
                "configJson": {"sampleSize": 100},
            },
        }
    )

    payload = ExperimentRunFailureEvent.from_requested_event(
        event=requested_event,
        exception=RuntimeError("failed"),
        log_file_url="s3://bucket/run.log",
    ).to_payload()

    assert payload["payload"]["logFileUrl"] == "s3://bucket/run.log"
