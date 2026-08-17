# Purpose: Materialize input artifacts and publish generated files for an experiment run.

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any
from urllib.parse import unquote, urlparse

from experiment_runtime.models import ExperimentExecutionContext, ExperimentInputContext
from experiment_runtime.models.experiment_run_completed_event import Artifact
from experiment_runtime.storage import ObjectStorage


class ArtifactStorageError(ValueError):
    """Raised when an input or output artifact cannot be materialized."""


class RunArtifactStore:
    """Run-scoped artifact IO shared by analysis-worker executors."""

    def __init__(self, object_storage: ObjectStorage | None) -> None:
        self._object_storage = object_storage

    def output_dir(
        self,
        context: ExperimentExecutionContext,
        experiment_type: str,
    ) -> Path:
        directory = (
            Path("/tmp/experimentops/runs")
            / _safe_part(context.experiment_run_uuid)
            / _safe_part(experiment_type.lower())
        )
        directory.mkdir(parents=True, exist_ok=True)
        return directory

    def materialize_input(
        self,
        input_context: ExperimentInputContext,
        destination_dir: Path,
    ) -> Path:
        uri = input_context.uri
        if uri is None or not uri.strip():
            raise ArtifactStorageError(
                f"Input port {input_context.port_name or '<unknown>'} has no URI"
            )

        parsed_uri = urlparse(uri)
        if parsed_uri.scheme == "file":
            path = Path(unquote(parsed_uri.path))
            self._require_existing_file(path)
            return path
        if parsed_uri.scheme in ("", None):
            path = Path(uri)
            self._require_existing_file(path)
            return path
        if parsed_uri.scheme != "s3":
            raise ArtifactStorageError(
                f"Unsupported artifact URI scheme: {parsed_uri.scheme}"
            )
        if self._object_storage is None:
            raise ArtifactStorageError(
                "Object storage is required for s3:// artifact inputs"
            )

        object_key = unquote(parsed_uri.path).lstrip("/")
        if not object_key:
            raise ArtifactStorageError(f"S3 artifact URI has no object key: {uri}")

        destination_dir.mkdir(parents=True, exist_ok=True)
        source_name = Path(parsed_uri.path).name or input_context.port_name or "input"
        destination = destination_dir / _safe_filename(source_name)
        destination.write_bytes(self._object_storage.download_file(object_key))
        return destination

    def write_json(self, path: Path, payload: Any) -> None:
        path.write_text(
            json.dumps(payload, indent=2, sort_keys=True),
            encoding="utf-8",
        )

    def publish(
        self,
        path: Path,
        context: ExperimentExecutionContext,
        experiment_type: str,
        artifact_type: str,
        artifact_format: str,
    ) -> Artifact:
        self._require_existing_file(path)
        uri = path.resolve().as_uri()
        if self._object_storage is not None:
            uri = self._object_storage.upload_file(
                key=_object_key(context, experiment_type, path.name),
                content=path.read_bytes(),
            )

        return Artifact(
            format=artifact_format.upper(),
            type=artifact_type,
            uri=uri,
            size=path.stat().st_size,
        )

    @staticmethod
    def _require_existing_file(path: Path) -> None:
        if not path.is_file():
            raise ArtifactStorageError(f"Artifact file does not exist: {path}")


def _object_key(
    context: ExperimentExecutionContext,
    experiment_type: str,
    filename: str,
) -> str:
    return (
        f"workspaces/{_safe_part(context.workspace_uuid)}/"
        f"projects/{_safe_part(context.project_uuid)}/"
        f"experiments/{_safe_part(context.experiment_uuid)}/"
        f"runs/{_safe_part(context.experiment_run_uuid)}/"
        f"artifacts/{_safe_part(experiment_type.lower())}/{_safe_filename(filename)}"
    )


def _safe_filename(value: str) -> str:
    return re.sub(r"[^A-Za-z0-9._-]", "_", value) or "artifact"


def _safe_part(value: str | None) -> str:
    return _safe_filename(value or "unknown")
