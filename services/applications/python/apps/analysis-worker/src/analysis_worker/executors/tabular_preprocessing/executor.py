# Purpose: Fit preprocessing on training data and publish transformed datasets and metadata.

from __future__ import annotations

import joblib
import sklearn

from analysis_worker.artifacts import RunArtifactStore
from analysis_worker.contracts import bind_execution
from analysis_worker.executors.tabular_preprocessing.contract import CONTRACT
from analysis_worker.executors.tabular_preprocessing.model import (
    TabularPreprocessingConfig,
    TabularPreprocessingReport,
)
from analysis_worker.executors.tabular_preprocessing.operation import (
    preprocess_datasets,
    preprocessor_bundle,
)
from analysis_worker.handlers.experiment_run_progress_publisher import (
    ExperimentRunProgressPublisher,
)
from analysis_worker.run_logs import run_info, run_verbose
from analysis_worker.tabular import read_csv
from experiment_runtime.logging.context import ExperimentOpsLogger
from experiment_runtime.models import ExperimentExecutionContext
from experiment_runtime.models.experiment_run_completed_event import Result
from experiment_runtime.storage import ObjectStorage


logger = ExperimentOpsLogger.get_logger("analysis-worker")


class TabularPreprocessingExecutor:
    def __init__(
        self,
        progress_publisher: ExperimentRunProgressPublisher,
        object_storage: ObjectStorage | None = None,
    ) -> None:
        self._progress_publisher = progress_publisher
        self._artifacts = RunArtifactStore(object_storage)

    def execute(self, context: ExperimentExecutionContext) -> Result:
        bound = bind_execution(context, CONTRACT, TabularPreprocessingConfig)
        run_info(
            context,
            "Starting tabular preprocessing with %s numerical and %s "
            "categorical features.",
            len(bound.config.numerical_columns),
            len(bound.config.categorical_columns),
        )
        run_verbose(
            context,
            "Preprocessing configuration: target=%s, numerical_columns=%s, "
            "categorical_columns=%s, numerical_imputation=%s, "
            "categorical_imputation=%s, numerical_scaling=%s, "
            "categorical_encoding=%s, unknown_category_handling=%s.",
            bound.config.target_column,
            bound.config.numerical_columns,
            bound.config.categorical_columns,
            bound.config.numerical_imputation,
            bound.config.categorical_imputation,
            bound.config.numerical_scaling,
            bound.config.categorical_encoding,
            bound.config.unknown_category_handling,
        )
        output_dir = self._artifacts.output_dir(context, CONTRACT.experiment_type)
        train_path = self._artifacts.materialize_input(
            bound.inputs["trainDataset"],
            output_dir / "inputs" / "train",
        )
        test_path = self._artifacts.materialize_input(
            bound.inputs["testDataset"],
            output_dir / "inputs" / "test",
        )
        train_data = read_csv(train_path)
        test_data = read_csv(test_path)
        run_info(
            context,
            "Loaded train and test datasets: train_rows=%s, test_rows=%s.",
            len(train_data),
            len(test_data),
        )
        self._publish(context, 20)

        processed_train, processed_test, transformer, metrics = preprocess_datasets(
            train_data,
            test_data,
            bound.config,
        )
        run_info(
            context,
            "Fitted preprocessing on training data only and transformed both "
            "partitions into %s model features.",
            metrics.output_feature_count,
        )
        run_verbose(
            context,
            "Generated model feature names: %s.",
            metrics.output_feature_names,
        )
        self._publish(context, 65)

        processed_train_path = output_dir / "processed-train.csv"
        processed_test_path = output_dir / "processed-test.csv"
        bundle_path = output_dir / "preprocessor.joblib"
        report_path = output_dir / "preprocessing-report.json"
        processed_train.to_csv(processed_train_path, index=False)
        processed_test.to_csv(processed_test_path, index=False)
        joblib.dump(
            preprocessor_bundle(
                transformer,
                bound.config,
                metrics.output_feature_names,
                sklearn.__version__,
            ),
            bundle_path,
        )
        self._artifacts.write_json(
            report_path,
            TabularPreprocessingReport(metrics=metrics).model_dump(
                by_alias=True,
                mode="json",
            ),
        )
        run_info(
            context,
            "Created processed datasets, preprocessor bundle, and preprocessing report.",
        )
        self._publish(context, 85)

        artifacts = [
            self._artifacts.publish(
                processed_train_path,
                context,
                CONTRACT.experiment_type,
                "processedTrainDataset",
                "CSV",
            ),
            self._artifacts.publish(
                processed_test_path,
                context,
                CONTRACT.experiment_type,
                "processedTestDataset",
                "CSV",
            ),
            self._artifacts.publish(
                bundle_path,
                context,
                CONTRACT.experiment_type,
                "preprocessorBundle",
                "PICKLE",
            ),
            self._artifacts.publish(
                report_path,
                context,
                CONTRACT.experiment_type,
                "preprocessingReport",
                "JSON",
            ),
        ]
        run_info(context, "Published 4 tabular preprocessing artifacts.")
        run_info(context, "Tabular preprocessing completed successfully.")
        self._publish(context, 100)
        logger.info(
            "Preprocessed tabular datasets. output_features=%s",
            metrics.output_feature_count,
        )
        return Result(artifact=artifacts, metrics=[metrics])

    def _publish(self, context: ExperimentExecutionContext, progress: int) -> None:
        self._progress_publisher.publish(context, progress)
