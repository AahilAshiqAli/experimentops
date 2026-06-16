from __future__ import annotations

import logging


logger = logging.getLogger(__name__)


class CsvProfileAnalysisExecutor:
    def execute(self, context: dict) -> dict:
        logger.info(
            "Running CSV profile analysis. experiment_run_uuid=%s dataset_uri=%s",
            context.get("experimentRunUuid"),
            context.get("datasetUri"),
        )

        return {
            "status": "SUCCEEDED",
            "rowsProcessed": 100,
        }
