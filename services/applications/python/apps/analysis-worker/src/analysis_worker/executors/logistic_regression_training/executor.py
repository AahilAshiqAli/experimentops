# Purpose: Train logistic regression and publish its reusable model bundle and report.

from __future__ import annotations

import joblib
import sklearn

from analysis_worker.artifacts import RunArtifactStore
from analysis_worker.contracts import bind_execution
from analysis_worker.executors.logistic_regression_training.contract import CONTRACT
from analysis_worker.executors.logistic_regression_training.model import (
    LogisticRegressionTrainingConfig,
    LogisticRegressionTrainingReport,
)
from analysis_worker.executors.logistic_regression_training.operation import (
    model_bundle,
    train_logistic_regression,
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


class LogisticRegressionTrainingExecutor:
    def __init__(
        self,
        progress_publisher: ExperimentRunProgressPublisher,
        object_storage: ObjectStorage | None = None,
    ) -> None:
        self._progress_publisher = progress_publisher
        self._artifacts = RunArtifactStore(object_storage)

    def execute(self, context: ExperimentExecutionContext) -> Result:
        bound = bind_execution(context, CONTRACT, LogisticRegressionTrainingConfig)
        run_info(
            context,
            "Starting logistic regression training for target '%s'.",
            bound.config.target_column,
        )
        run_verbose(
            context,
            "Training configuration: C=%s, max_iter=%s, class_weight=%s, "
            "random_seed=%s.",
            bound.config.c,
            bound.config.max_iter,
            bound.config.class_weight,
            bound.config.random_seed,
        )
        output_dir = self._artifacts.output_dir(context, CONTRACT.experiment_type)
        training_path = self._artifacts.materialize_input(
            bound.inputs["trainDataset"],
            output_dir / "inputs",
        )
        training_data = read_csv(
            training_path,
            max_columns=MAX_PROCESSED_FEATURES + 1,
        )
        run_info(
            context,
            "Loaded processed training dataset with %s rows and %s candidate features.",
            len(training_data),
            max(len(training_data.columns) - 1, 0),
        )
        self._publish(context, 20)

        estimator, metrics = train_logistic_regression(training_data, bound.config)
        run_info(
            context,
            "Validated numeric features and fitted logistic regression: "
            "rows=%s, features=%s, iterations=%s.",
            metrics.training_rows,
            metrics.feature_count,
            metrics.iterations,
        )
        run_verbose(
            context,
            "Trained model classes=%s and feature_names=%s.",
            metrics.classes,
            metrics.feature_names,
        )
        self._publish(context, 70)

        model_path = output_dir / "model.joblib"
        report_path = output_dir / "training-report.json"
        joblib.dump(
            model_bundle(estimator, metrics, sklearn.__version__),
            model_path,
        )
        self._artifacts.write_json(
            report_path,
            LogisticRegressionTrainingReport(metrics=metrics).model_dump(
                by_alias=True,
                mode="json",
            ),
        )
        run_info(context, "Created the model bundle and training report files.")
        self._publish(context, 85)

        artifacts = [
            self._artifacts.publish(
                model_path,
                context,
                CONTRACT.experiment_type,
                "modelBundle",
                "PICKLE",
            ),
            self._artifacts.publish(
                report_path,
                context,
                CONTRACT.experiment_type,
                "trainingReport",
                "JSON",
            ),
        ]
        run_info(context, "Published modelBundle and trainingReport artifacts.")
        run_info(context, "Logistic regression training completed successfully.")
        self._publish(context, 100)
        logger.info(
            "Trained logistic regression. rows=%s features=%s",
            metrics.training_rows,
            metrics.feature_count,
        )
        return Result(artifact=artifacts, metrics=[metrics])

    def _publish(self, context: ExperimentExecutionContext, progress: int) -> None:
        self._progress_publisher.publish(context, progress)
