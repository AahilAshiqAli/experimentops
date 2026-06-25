from __future__ import annotations

import json
from pathlib import Path

from analysis_worker.executors.csv_profile_analysis.cleaner import clean_context
from analysis_worker.executors.csv_profile_analysis.model import (
    CsvProfileAnalysisContext,
)


class FakeObjectStorage:
    def __init__(self) -> None:
        self.uploads: dict[str, bytes] = {}
        self.downloads: dict[str, bytes] = {
            "workspaces/workspace-1/projects/project-1/datasets/dataset-1/raw/input.csv": (
                b"Name,Age\n Alice ,30\nAlice,30\n,\n"
            )
        }

    def upload_file(self, key: str, content: bytes) -> str:
        self.uploads[key] = content
        return f"s3://test-bucket/{key}"

    def download_file(self, key: str) -> bytes:
        return self.downloads[key]

    def delete_file(self, key: str) -> None:
        return None


def test_clean_context_downloads_input_and_uploads_artifacts(tmp_path: Path) -> None:
    object_storage = FakeObjectStorage()

    result = clean_context(
        context=CsvProfileAnalysisContext(
            workspace_uuid="workspace-1",
            project_uuid="project-1",
            experiment_uuid="experiment-1",
            experiment_run_uuid="run-1",
            dataset_uri=(
                "s3://test-bucket/workspaces/workspace-1/projects/project-1/"
                "datasets/dataset-1/raw/input.csv"
            ),
            output_dir=str(tmp_path),
        ),
        object_storage=object_storage,
    )

    assert result.cleaned_dataset_uri == (
        "s3://test-bucket/workspaces/workspace-1/projects/project-1/"
        "experiments/experiment-1/runs/run-1/artifacts/csv-profile-analysis/"
        "input_cleaned.csv"
    )
    assert result.cleaning_report_uri == (
        "s3://test-bucket/workspaces/workspace-1/projects/project-1/"
        "experiments/experiment-1/runs/run-1/artifacts/csv-profile-analysis/"
        "input_cleaning_report.json"
    )
    assert result.rows_processed == 1
    assert result.model_dump(by_alias=True) == {
        "artifact": [
            {
                "format": "csv",
                "type": "CLEANED_DATASET",
                "uri": result.cleaned_dataset_uri,
                "size": len(object_storage.uploads[
                    "workspaces/workspace-1/projects/project-1/"
                    "experiments/experiment-1/runs/run-1/artifacts/"
                    "csv-profile-analysis/input_cleaned.csv"
                ]),
            },
            {
                "format": "json",
                "type": "CLEANING_REPORT",
                "uri": result.cleaning_report_uri,
                "size": len(object_storage.uploads[
                    "workspaces/workspace-1/projects/project-1/"
                    "experiments/experiment-1/runs/run-1/artifacts/"
                    "csv-profile-analysis/input_cleaning_report.json"
                ]),
            },
        ],
        "metrics": result.metrics.model_dump(by_alias=True),
    }
    assert len(object_storage.uploads) == 2

    report_key = (
        "workspaces/workspace-1/projects/project-1/experiments/experiment-1/"
        "runs/run-1/artifacts/csv-profile-analysis/input_cleaning_report.json"
    )
    report = json.loads(object_storage.uploads[report_key])

    assert report["context"]["artifact"][1]["uri"] == result.cleaning_report_uri
    assert report["context"]["metrics"]["outputRows"] == result.rows_processed
