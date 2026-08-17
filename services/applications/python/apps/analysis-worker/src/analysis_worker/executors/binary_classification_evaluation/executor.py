# Purpose: Load a model bundle, evaluate test data, and publish the evaluation report.

from __future__ import annotations

import joblib

from analysis_worker.artifacts import RunArtifactStore
from analysis_worker.contracts import bind_execution
from analysis_worker.executors.binary_classification_evaluation.contract import CONTRACT
from analysis_worker.executors.binary_classification_evaluation.model import (
    BinaryClassificationEvaluationConfig,
    BinaryClassificationEvaluationReport,
)
from analysis_worker.executors.binary_classification_evaluation.operation import (
    evaluate_binary_classifier,
)
from analysis_worker.handlers.experiment_run_progress_publisher import (
    ExperimentRunProgressPublisher,
)
from analysis_worker.run_logs import run_info, run_verbose
from analysis_worker.tabular import MAX_PROCESSED_FEATURES, read_csv
from experiment_runtime.logging.context import ExperimentOpsLogger
from experiment_runtime.models import ExperimentExecutionContext
from experiment_runtime.models.experiment_run_completed_event import Result
from experiment_runtime.storage import ObjectStorage


logger = ExperimentOpsLogger.get_logger("analysis-worker")


class BinaryClassificationEvaluationExecutor:
    def __init__(
        self,
        progress_publisher: ExperimentRunProgressPublisher,
        object_storage: ObjectStorage | None = None,
    ) -> None:
        self._progress_publisher = progress_publisher
        self._artifacts = RunArtifactStore(object_storage)

    def execute(self, context: ExperimentExecutionContext) -> Result:
        bound = bind_execution(context, CONTRACT, BinaryClassificationEvaluationConfig)
        run_info(
            context,
            "Starting binary classification evaluation with decision threshold %s.",
            bound.config.decision_threshold,
        )
        output_dir = self._artifacts.output_dir(context, CONTRACT.experiment_type)
        model_path = self._artifacts.materialize_input(
            bound.inputs["model"],
            output_dir / "inputs" / "model",
        )
        test_path = self._artifacts.materialize_input(
            bound.inputs["testDataset"],
            output_dir / "inputs" / "test",
        )
        test_data = read_csv(test_path, max_columns=MAX_PROCESSED_FEATURES + 1)
        run_info(
            context,
            "Materialized the model input and loaded the test dataset: "
            "test_rows=%s, columns=%s.",
            len(test_data),
            len(test_data.columns),
        )
        self._publish(context, 25)

        try:
            bundle = joblib.load(model_path)  # loads the pickle file into memory
        except Exception as error:
            raise ValueError("Unable to load ExperimentOps model bundle") from error
        metrics = evaluate_binary_classifier(bundle, test_data, bound.config)
        run_info(
            context,
            "Validated the model bundle and evaluated %s test rows: "
            "accuracy=%.4f, precision=%.4f, recall=%.4f, f1=%.4f, roc_auc=%.4f.",
            metrics.rows_evaluated,
            metrics.accuracy,
            metrics.precision,
            metrics.recall,
            metrics.f1,
            metrics.roc_auc,
        )
        run_verbose(
            context,
            "Confusion matrix: true_negative=%s, false_positive=%s, "
            "false_negative=%s, true_positive=%s.",
            metrics.true_negative,
            metrics.false_positive,
            metrics.false_negative,
            metrics.true_positive,
        )
        self._publish(context, 75)

        report_path = output_dir / "evaluation-report.json"
        self._artifacts.write_json(
            report_path,
            BinaryClassificationEvaluationReport(metrics=metrics).model_dump(
                by_alias=True,
                mode="json",
            ),
        )
        artifact = self._artifacts.publish(
            report_path,
            context,
            CONTRACT.experiment_type,
            "evaluationReport",
            "JSON",
        )
        run_info(context, "Created and published the evaluationReport artifact.")
        run_info(context, "Binary classification evaluation completed successfully.")
        self._publish(context, 100)
        logger.info(
            "Evaluated binary classifier. rows=%s accuracy=%s",
            metrics.rows_evaluated,
            metrics.accuracy,
        )
        return Result(artifact=[artifact], metrics=[metrics])

    def _publish(self, context: ExperimentExecutionContext, progress: int) -> None:
        self._progress_publisher.publish(context, progress)
