# Purpose: Split a bounded CSV dataset and publish train, test, and report artifacts.

from __future__ import annotations

from analysis_worker.artifacts import RunArtifactStore
from analysis_worker.contracts import bind_execution
from analysis_worker.executors.tabular_train_test_split.contract import CONTRACT
from analysis_worker.executors.tabular_train_test_split.model import (
    TabularTrainTestSplitConfig,
    TabularTrainTestSplitReport,
)
from analysis_worker.executors.tabular_train_test_split.operation import split_dataframe
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


class TabularTrainTestSplitExecutor:
    def __init__(
        self,
        progress_publisher: ExperimentRunProgressPublisher,
        object_storage: ObjectStorage | None = None,
    ) -> None:
        self._progress_publisher = progress_publisher
        self._artifacts = RunArtifactStore(object_storage)

    def execute(self, context: ExperimentExecutionContext) -> Result:
        bound = bind_execution(context, CONTRACT, TabularTrainTestSplitConfig)
        run_info(
            context,
            "Starting train/test split for target '%s'.",
            bound.config.target_column,
        )
        run_verbose(
            context,
            "Split configuration: test_size=%s, random_seed=%s, stratify=%s.",
            bound.config.test_size,
            bound.config.random_seed,
            bound.config.stratify,
        )
        output_dir = self._artifacts.output_dir(context, CONTRACT.experiment_type)
        input_path = self._artifacts.materialize_input(
            bound.inputs["dataset"],
            output_dir / "inputs",
        )
        dataframe = read_csv(input_path)
        run_info(
            context,
            "Loaded source dataset with %s rows and %s columns.",
            len(dataframe),
            len(dataframe.columns),
        )
        self._publish(context, 20)

        train_data, test_data, metrics = split_dataframe(dataframe, bound.config)
        run_info(
            context,
            "Validated the binary target and created partitions: "
            "train_rows=%s, test_rows=%s.",
            metrics.train_rows,
            metrics.test_rows,
        )
        run_verbose(
            context,
            "Class distribution by partition: %s.",
            metrics.class_distribution,
        )
        self._publish(context, 55)

        train_path = output_dir / "train.csv"
        test_path = output_dir / "test.csv"
        report_path = output_dir / "split-report.json"
        train_data.to_csv(train_path, index=False)
        test_data.to_csv(test_path, index=False)
        self._artifacts.write_json(
            report_path,
            TabularTrainTestSplitReport(metrics=metrics).model_dump(
                by_alias=True,
                mode="json",
            ),
        )
        run_info(
            context,
            "Created trainDataset, testDataset, and splitReport output files.",
        )
        self._publish(context, 80)

        artifacts = [
            self._artifacts.publish(
                train_path,
                context,
                CONTRACT.experiment_type,
                "trainDataset",
                "CSV",
            ),
            self._artifacts.publish(
                test_path,
                context,
                CONTRACT.experiment_type,
                "testDataset",
                "CSV",
            ),
            self._artifacts.publish(
                report_path,
                context,
                CONTRACT.experiment_type,
                "splitReport",
                "JSON",
            ),
        ]
        run_info(context, "Published 3 train/test split artifacts.")
        run_info(context, "Train/test split completed successfully.")
        self._publish(context, 100)
        logger.info(
            "Split tabular dataset. train_rows=%s test_rows=%s",
            metrics.train_rows,
            metrics.test_rows,
        )
        return Result(artifact=artifacts, metrics=[metrics])

    def _publish(self, context: ExperimentExecutionContext, progress: int) -> None:
        self._progress_publisher.publish(context, progress)
