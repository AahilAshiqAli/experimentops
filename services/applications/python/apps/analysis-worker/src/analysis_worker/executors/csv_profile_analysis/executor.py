from __future__ import annotations

from analysis_worker.executors.csv_profile_analysis.cleaner import clean_context
from analysis_worker.executors.csv_profile_analysis.model import (
    CsvProfileAnalysisContext,
)
from experiment_runtime.logging.context import ExperimentOpsLogger
from experiment_runtime.models import ExperimentExecutionContext
from experiment_runtime.models.experiment_run_completed_event import Result
from experiment_runtime.storage import ObjectStorage

logger = ExperimentOpsLogger.get_logger("analysis-worker")


class CsvProfileAnalysisExecutor:
    def __init__(self, object_storage: ObjectStorage | None = None) -> None:
        self._object_storage = object_storage

    def execute(self, context: ExperimentExecutionContext) -> Result:
        execution_context = CsvProfileAnalysisContext.model_validate(context)
        cleaned_context = clean_context(
            context=execution_context,
            object_storage=self._object_storage,
        )

        logger.info(
            "Running CSV profile analysis. dataset_uri=%s",
            execution_context.dataset_uri,
        )

        return cleaned_context
