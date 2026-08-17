# Purpose: Verify reusable ML operations and the complete named-artifact worker pipeline.

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import pandas as pd

from analysis_worker.executors.binary_classification_evaluation.model import (
    BinaryClassificationEvaluationConfig,
)
from analysis_worker.executors.binary_classification_evaluation.operation import (
    evaluate_binary_classifier,
)
from analysis_worker.executors.logistic_regression_training.model import (
    LogisticRegressionTrainingConfig,
)
from analysis_worker.executors.logistic_regression_training.operation import (
    model_bundle,
    train_logistic_regression,
)
from analysis_worker.executors.tabular_preprocessing.contract import (
    CONTRACT as PREPROCESSING_CONTRACT,
)
from analysis_worker.executors.tabular_preprocessing.model import (
    TabularPreprocessingConfig,
)
from analysis_worker.executors.tabular_preprocessing.operation import (
    preprocess_datasets,
)
from analysis_worker.executors.tabular_train_test_split.model import (
    TabularTrainTestSplitConfig,
)
from analysis_worker.executors.tabular_train_test_split.operation import split_dataframe
from analysis_worker.handlers.experiment_run_progress_publisher import (
    ExperimentRunProgressPublisher,
)
from analysis_worker.handlers.experiment_run_requested_handler import (
    build_experiment_run_requested_handler,
)
from experiment_runtime.kafka import KafkaMessage
from experiment_runtime.registry import ExperimentRegistry


class FakeProducer:
    def __init__(self) -> None:
        self.produced: list[dict[str, Any]] = []

    def produce_sync(self, topic: str, key: str | None, value: Any) -> None:
        self.produced.append({"topic": topic, "key": key, "value": value})


def test_preprocessing_contract_can_generate_platform_manifest() -> None:
    manifest = PREPROCESSING_CONTRACT.to_manifest()

    assert [item["portName"] for item in manifest["inputs"]] == [
        "trainDataset",
        "testDataset",
    ]
    assert [item["name"] for item in manifest["outputs"]] == [
        "processedTrainDataset",
        "processedTestDataset",
        "preprocessorBundle",
        "preprocessingReport",
    ]


def test_binary_classification_operations_are_reusable_and_deterministic() -> None:
    source = _sample_dataframe()
    split_config = TabularTrainTestSplitConfig(
        target_column="survived",
        test_size=0.2,
        random_seed=42,
        stratify=True,
    )

    train_a, test_a, _ = split_dataframe(source, split_config)
    train_b, test_b, _ = split_dataframe(source, split_config)
    pd.testing.assert_frame_equal(train_a, train_b)
    pd.testing.assert_frame_equal(test_a, test_b)

    preprocessing_config = _preprocessing_config()
    processed_train, processed_test, _, preprocessing_metrics = preprocess_datasets(
        train_a,
        test_a,
        preprocessing_config,
    )
    assert "passenger_id" not in " ".join(preprocessing_metrics.output_feature_names)
    assert not processed_train.isna().any().any()
    assert list(processed_train.columns) == list(processed_test.columns)

    estimator, training_metrics = train_logistic_regression(
        processed_train,
        LogisticRegressionTrainingConfig(target_column="survived"),
    )
    bundle = model_bundle(estimator, training_metrics, "test-version")
    evaluation = evaluate_binary_classifier(
        bundle,
        processed_test,
        BinaryClassificationEvaluationConfig(decision_threshold=0.5),
    )

    assert evaluation.rows_evaluated == 20
    assert 0 <= evaluation.accuracy <= 1
    assert 0 <= evaluation.roc_auc <= 1
    assert (
        evaluation.true_negative
        + evaluation.false_positive
        + evaluation.false_negative
        + evaluation.true_positive
        == evaluation.rows_evaluated
    )


def test_complete_pipeline_executes_through_named_artifact_contracts(
    tmp_path: Path,
) -> None:
    source_path = tmp_path / "binary-classification.csv"
    _sample_dataframe().to_csv(source_path, index=False)
    completed_producer = FakeProducer()
    failure_producer = FakeProducer()
    progress_producer = FakeProducer()
    progress_publisher = ExperimentRunProgressPublisher(
        producer=progress_producer,
        producer_topic="progress-topic",
    )
    registry = ExperimentRegistry.discover_executors(
        "analysis_worker.executors",
        executor_dependencies={
            "object_storage": None,
            "progress_publisher": progress_publisher,
        },
    )
    handler = build_experiment_run_requested_handler(
        registry=registry,
        producer=completed_producer,
        producer_topic="completed-topic",
        failure_producer=failure_producer,
        failure_producer_topic="failure-topic",
        progress_publisher=progress_publisher,
        log_root_dir=tmp_path / "logs",
    )

    handler(
        KafkaMessage(
            topic="requested-topic",
            partition=0,
            offset=1,
            key="end-to-end-run",
            value=_pipeline_event(source_path),
            headers={},
            timestamp_millis=None,
        )
    )

    assert failure_producer.produced == []
    assert len(completed_producer.produced) == 1
    payload = completed_producer.produced[0]["value"]["payload"]
    assert payload["status"] == "SUCCEEDED"
    assert payload["experimentType"] == (
        "TABULAR_TRAIN_TEST_SPLIT,TABULAR_PREPROCESSING,"
        "LOGISTIC_REGRESSION_TRAINING,BINARY_CLASSIFICATION_EVALUATION"
    )
    artifact_types = {item["type"] for item in payload["result"]["artifact"]}
    assert artifact_types == {
        "trainDataset",
        "testDataset",
        "splitReport",
        "processedTrainDataset",
        "processedTestDataset",
        "preprocessorBundle",
        "preprocessingReport",
        "modelBundle",
        "trainingReport",
        "evaluationReport",
    }
    assert len(payload["result"]["metrics"]) == 4
    progress_payloads = [
        produced["value"]["payload"] for produced in progress_producer.produced
    ]
    assert progress_payloads[-1]["progress"] == "100"
    assert all(progress_payload["logs"] for progress_payload in progress_payloads)

    customer_logs = [
        log
        for progress_payload in progress_payloads
        for log in progress_payload["logs"]
    ]
    assert all(log["level"] == "INFO" for log in customer_logs)
    assert {log["experimentType"] for log in customer_logs} == {
        "TABULAR_TRAIN_TEST_SPLIT",
        "TABULAR_PREPROCESSING",
        "LOGISTIC_REGRESSION_TRAINING",
        "BINARY_CLASSIFICATION_EVALUATION",
    }
    customer_messages = [log["message"] for log in customer_logs]
    assert any("Loaded source dataset" in message for message in customer_messages)
    assert any(
        "Fitted preprocessing on training data only" in message
        for message in customer_messages
    )
    assert any(
        "fitted logistic regression" in message for message in customer_messages
    )
    assert any(
        "Validated the model bundle" in message for message in customer_messages
    )

    run_log_path = tmp_path / "logs" / "end-to-end-run" / "run.log.jsonl"
    persisted_logs = [
        json.loads(line)
        for line in run_log_path.read_text(encoding="utf-8").splitlines()
    ]
    assert len(persisted_logs) > len(customer_logs)
    assert any(log["level"] == "VERBOSE" for log in persisted_logs)


def _sample_dataframe() -> pd.DataFrame:
    rows: list[dict[str, object]] = []
    for index in range(100):
        rows.append(
            {
                "survived": index % 2,
                "age": None if index % 13 == 0 else 18 + index % 50,
                "fare": 7.5 + index * 0.8,
                "sex": "female" if index % 2 else "male",
                "embarked": ("S", "C", "Q")[index % 3],
                "passenger_id": f"passenger-{index}",
            }
        )
    return pd.DataFrame(rows)


def _preprocessing_config() -> TabularPreprocessingConfig:
    return TabularPreprocessingConfig(
        target_column="survived",
        numerical_columns=["age", "fare"],
        categorical_columns=["sex", "embarked"],
    )


def _pipeline_event(source_path: Path) -> dict[str, object]:
    return {
        "metadata": {
            "eventUuid": "event-1",
            "traceUuid": "request-1",
            "requesterUuid": "user-1",
            "uuid": "end-to-end-run",
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
                        "experimentConfigUuid": "split-config",
                        "experimentType": "TABULAR_TRAIN_TEST_SPLIT",
                        "experimentConfigJson": {
                            "targetColumn": "survived",
                            "testSize": 0.2,
                            "randomSeed": 42,
                            "stratify": True,
                        },
                        "timeWeight": 2,
                        "inputs": [
                            {
                                "portName": "dataset",
                                "inputType": "DATASET",
                                "dataKind": "TABULAR_DATASET",
                                "format": "CSV",
                                "datasetVersionUuid": "dataset-version-1",
                                "datasetUri": source_path.as_uri(),
                            }
                        ],
                        "outputs": _outputs(
                            ("trainDataset", "TABULAR_DATASET", "CSV", "CONNECTABLE"),
                            ("testDataset", "TABULAR_DATASET", "CSV", "CONNECTABLE"),
                            ("splitReport", "REPORT", "JSON", "TERMINAL"),
                        ),
                    },
                    {
                        "stepCount": 2,
                        "experimentConfigUuid": "preprocessing-config",
                        "experimentType": "TABULAR_PREPROCESSING",
                        "experimentConfigJson": {
                            "targetColumn": "survived",
                            "numericalColumns": ["age", "fare"],
                            "categoricalColumns": ["sex", "embarked"],
                            "numericalImputation": "MEDIAN",
                            "categoricalImputation": "MOST_FREQUENT",
                            "numericalScaling": "STANDARD",
                            "categoricalEncoding": "ONE_HOT",
                            "unknownCategoryHandling": "IGNORE",
                        },
                        "timeWeight": 3,
                        "inputs": [
                            _artifact_input(
                                "trainDataset",
                                1,
                                "trainDataset",
                                "TABULAR_DATASET",
                                "CSV",
                            ),
                            _artifact_input(
                                "testDataset",
                                1,
                                "testDataset",
                                "TABULAR_DATASET",
                                "CSV",
                            ),
                        ],
                        "outputs": _outputs(
                            (
                                "processedTrainDataset",
                                "TABULAR_DATASET",
                                "CSV",
                                "CONNECTABLE",
                            ),
                            (
                                "processedTestDataset",
                                "TABULAR_DATASET",
                                "CSV",
                                "CONNECTABLE",
                            ),
                            ("preprocessorBundle", "FILE", "PICKLE", "CONNECTABLE"),
                            ("preprocessingReport", "REPORT", "JSON", "TERMINAL"),
                        ),
                    },
                    {
                        "stepCount": 3,
                        "experimentConfigUuid": "training-config",
                        "experimentType": "LOGISTIC_REGRESSION_TRAINING",
                        "experimentConfigJson": {
                            "targetColumn": "survived",
                            "c": 1.0,
                            "maxIter": 1000,
                            "classWeight": "NONE",
                            "randomSeed": 42,
                        },
                        "timeWeight": 5,
                        "inputs": [
                            _artifact_input(
                                "trainDataset",
                                2,
                                "processedTrainDataset",
                                "TABULAR_DATASET",
                                "CSV",
                            )
                        ],
                        "outputs": _outputs(
                            ("modelBundle", "MODEL", "PICKLE", "CONNECTABLE"),
                            ("trainingReport", "REPORT", "JSON", "TERMINAL"),
                        ),
                    },
                    {
                        "stepCount": 4,
                        "experimentConfigUuid": "evaluation-config",
                        "experimentType": "BINARY_CLASSIFICATION_EVALUATION",
                        "experimentConfigJson": {"decisionThreshold": 0.5},
                        "timeWeight": 2,
                        "inputs": [
                            _artifact_input(
                                "model", 3, "modelBundle", "MODEL", "PICKLE"
                            ),
                            _artifact_input(
                                "testDataset",
                                2,
                                "processedTestDataset",
                                "TABULAR_DATASET",
                                "CSV",
                            ),
                        ],
                        "outputs": _outputs(
                            ("evaluationReport", "REPORT", "JSON", "TERMINAL"),
                        ),
                    },
                ],
            },
        },
    }


def _artifact_input(
    port_name: str,
    source_step: int,
    artifact_name: str,
    data_kind: str,
    artifact_format: str,
) -> dict[str, object]:
    return {
        "portName": port_name,
        "inputType": "ARTIFACT",
        "dataKind": data_kind,
        "format": artifact_format,
        "sourceStepCount": source_step,
        "artifactName": artifact_name,
    }


def _outputs(
    *values: tuple[str, str, str, str],
) -> list[dict[str, object]]:
    return [
        {
            "name": name,
            "dataKind": data_kind,
            "formatStrategy": "FIXED",
            "format": artifact_format,
            "requiredForRun": True,
            "downStreamPolicy": policy,
        }
        for name, data_kind, artifact_format, policy in values
    ]
