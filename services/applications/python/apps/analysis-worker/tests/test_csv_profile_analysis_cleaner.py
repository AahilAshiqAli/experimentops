from __future__ import annotations

import json
from pathlib import Path

from analysis_worker.executors.csv_profile_analysis.cleaner import clean_context
from analysis_worker.executors.csv_profile_analysis.model import (
    CsvProfileAnalysisContext,
)
from experiment_runtime.models.experiment_run_completed_event import Artifact


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
    progress_updates: list[int] = []

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
        publish_progress=progress_updates.append,
        object_storage=object_storage,
    )

    assert progress_updates == [25, 75, 100]

    cleaned_dataset_artifact = _artifact(result.artifact, "CLEANED_DATASET")
    cleaning_report_artifact = _artifact(result.artifact, "CLEANING_REPORT")

    assert cleaned_dataset_artifact.uri == (
        "s3://test-bucket/workspaces/workspace-1/projects/project-1/"
        "experiments/experiment-1/runs/run-1/artifacts/csv-profile-analysis/"
        "input_cleaned.csv"
    )
    assert cleaning_report_artifact.uri == (
        "s3://test-bucket/workspaces/workspace-1/projects/project-1/"
        "experiments/experiment-1/runs/run-1/artifacts/csv-profile-analysis/"
        "input_cleaning_report.json"
    )
    assert result.metrics.output_rows == 1
    assert result.model_dump(by_alias=True) == {
        "artifact": [
            {
                "format": "csv",
                "type": "CLEANED_DATASET",
                "uri": cleaned_dataset_artifact.uri,
                "size": len(object_storage.uploads[
                    "workspaces/workspace-1/projects/project-1/"
                    "experiments/experiment-1/runs/run-1/artifacts/"
                    "csv-profile-analysis/input_cleaned.csv"
                ]),
                "stepCount": None,
                "portName": None,
            },
            {
                "format": "json",
                "type": "CLEANING_REPORT",
                "uri": cleaning_report_artifact.uri,
                "size": len(object_storage.uploads[
                    "workspaces/workspace-1/projects/project-1/"
                    "experiments/experiment-1/runs/run-1/artifacts/"
                    "csv-profile-analysis/input_cleaning_report.json"
                ]),
                "stepCount": None,
                "portName": None,
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

    assert report["context"]["artifact"][1]["uri"] == cleaning_report_artifact.uri
    assert report["context"]["metrics"]["outputRows"] == result.metrics.output_rows


def test_clean_context_reads_windows_1252_csv(tmp_path: Path) -> None:
    input_file_path = tmp_path / "input.csv"
    progress_updates: list[int] = []

    input_file_path.write_bytes(b"Name,Note\nAlice,pre\x96post\n")

    result = clean_context(
        context=CsvProfileAnalysisContext(
            experiment_run_uuid="run-1",
            dataset_uri=input_file_path.as_uri(),
            output_dir=str(tmp_path / "output"),
        ),
        publish_progress=progress_updates.append,
    )

    assert progress_updates == [25, 75, 100]
    assert result.metrics.output_rows == 1


def _artifact(artifacts: list[Artifact], artifact_type: str) -> Artifact:
    for artifact in artifacts:
        if artifact.type == artifact_type:
            return artifact

    raise AssertionError(f"Missing artifact with type={artifact_type}")
