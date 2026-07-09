from __future__ import annotations

import json

from experiment_runtime.models import (
    Artifact,
    ExperimentExecutionContext,
    ExperimentRunExecutionConfig,
    ExperimentRunCompletedEvent,
    ExperimentRunFailureErrorEntry,
    ExperimentRunProgressEvent,
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
                )
            ],
            metrics=ExampleMetric(rows_processed=100),
        ),
    ).to_payload()

    assert payload["metadata"]["traceUuid"] == "request-1"
    assert payload["metadata"]["uuid"] == "run-1"
    assert payload["payload"]["experimentType"] == "CSV_PROFILE_ANALYSIS"
    assert payload["payload"]["result"] == {
        "artifact": [
            {
                "format": "csv",
                "type": "CLEANED_DATASET",
                "uri": "s3://bucket/output.csv",
                "size": 10,
            }
        ],
        "metricsJson": '{"rowsProcessed":100}',
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
    }


def test_run_requested_event_parses_execution_configs_from_config_json() -> None:
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
                "configJson": json.dumps(
                    [
                        {
                            "stepCount": 1,
                            "experimentType": "CSV_PROFILE_ANALYSIS",
                            "experimentConfigJson": {"sampleSize": 100},
                        }
                    ]
                ),
            },
        }
    )

    assert requested_event.experiment_type is None
    assert requested_event.execution_configs == (
        ExperimentRunExecutionConfig(
            step_count=1,
            experiment_type="CSV_PROFILE_ANALYSIS",
            experiment_config_json={"sampleSize": 100},
        ),
    )

    context = ExperimentExecutionContext.from_requested_event(
        requested_event,
        requested_event.execution_configs[0],
    )

    assert context.experiment_type == "CSV_PROFILE_ANALYSIS"
    assert context.config_json == {"sampleSize": 100}


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
    ).to_payload()

    assert payload["metadata"]["traceUuid"] == "request-1"
    assert payload["metadata"]["eventType"] == "EXPERIMENT_RUN_PROGRESS"
    assert payload["metadata"]["uuid"] == "run-1"
    assert payload["metadata"]["workspaceUuid"] == "workspace-1"
    assert payload["payload"] == {
        "experimentUuid": "experiment-1",
        "experimentRunUuid": "run-1",
        "progress": "75",
    }


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
