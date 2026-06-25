from __future__ import annotations

import json
import re
from collections.abc import Callable
from pathlib import Path
from typing import Any
from urllib.parse import unquote, urlparse

import pandas as pd

from analysis_worker.executors.csv_profile_analysis.model import (
    CsvCleaningMetrics,
    CsvCleaningOutput,
    CsvCleaningReport,
    CsvProfileAnalysisContext,
)
from experiment_runtime.models.experiment_run_completed_event import Artifact
from experiment_runtime.storage import ObjectStorage

CSV_READ_ENCODINGS = ("utf-8", "utf-8-sig", "cp1252", "latin1")


class CsvCleaningError(Exception):
    """Raised when CSV cleaning cannot complete for the supplied context."""

    pass


def clean_context(
    context: CsvProfileAnalysisContext,
    publish_progress: Callable[[int], None],
    object_storage: ObjectStorage | None = None,
) -> CsvCleaningOutput:
    """Clean the requested CSV dataset, persist artifacts, and return the result."""

    output_dir = _resolve_output_dir(context)

    output_dir.mkdir(parents=True, exist_ok=True)

    input_file_path = _resolve_input_file_path(
        context=context,
        output_dir=output_dir,
        object_storage=object_storage,
    )

    if not input_file_path.exists():
        raise CsvCleaningError(f"Input CSV file does not exist: {input_file_path}")

    config = context.cleaning_config

    df = _read_csv(input_file_path)
    publish_progress(25)

    input_rows = len(df)
    input_columns = len(df.columns)
    missing_values_before = int(df.isna().sum().sum())

    unnamed_columns_removed = 0

    if config.remove_unnamed_columns:
        before_columns = len(df.columns)
        df = df.loc[:, ~df.columns.astype(str).str.startswith("Unnamed:")]
        unnamed_columns_removed = before_columns - len(df.columns)

    if config.normalize_column_names:
        normalized_columns = [
            _normalize_column_name(column)
            for column in df.columns
        ]

        df.columns = _deduplicate_column_names(normalized_columns)

    if config.trim_string_values:
        string_columns = df.select_dtypes(include=["object", "string"]).columns

        for column in string_columns:
            df[column] = df[column].map(
                lambda value: value.strip() if isinstance(value, str) else value
            )

    if config.empty_strings_as_null:
        df = df.replace(r"^\s*$", pd.NA, regex=True)

    rows_before_empty_drop = len(df)

    if config.drop_fully_empty_rows:
        df = df.dropna(how="all")

    fully_empty_rows_removed = rows_before_empty_drop - len(df)

    rows_before_required_drop = len(df)

    required_columns = [
        _normalize_column_name(column)
        if config.normalize_column_names
        else column
        for column in config.required_columns
    ]

    missing_required_columns = [
        column for column in required_columns
        if column not in df.columns
    ]

    if missing_required_columns:
        raise CsvCleaningError(
            f"Required columns are missing from CSV: {missing_required_columns}"
        )

    if required_columns:
        df = df.dropna(subset=required_columns)

    rows_removed_missing_required_values = (
        rows_before_required_drop - len(df)
    )

    rows_before_duplicate_drop = len(df)

    if config.drop_duplicate_rows:
        df = df.drop_duplicates()

    duplicate_rows_removed = rows_before_duplicate_drop - len(df)

    missing_values_after = int(df.isna().sum().sum())

    cleaned_file_path = output_dir / f"{input_file_path.stem}_cleaned.csv"
    report_file_path = output_dir / f"{input_file_path.stem}_cleaning_report.json"

    df.to_csv(cleaned_file_path, index=False)
    publish_progress(75)

    metrics = CsvCleaningMetrics(
        input_rows=input_rows,
        output_rows=len(df),
        input_columns=input_columns,
        output_columns=len(df.columns),
        missing_values_before=missing_values_before,
        missing_values_after=missing_values_after,
        unnamed_columns_removed=unnamed_columns_removed,
        fully_empty_rows_removed=fully_empty_rows_removed,
        rows_removed_missing_required_values=rows_removed_missing_required_values,
        duplicate_rows_removed=duplicate_rows_removed,
        final_columns=list(df.columns),
    )

    cleaned_dataset_uri = _upload_artifact(
        path=cleaned_file_path,
        context=context,
        object_storage=object_storage,
    )
    cleaning_report_uri = _to_file_uri(report_file_path)

    cleaned_output = _build_cleaning_output(
        cleaned_file_path=cleaned_file_path,
        cleaned_dataset_uri=cleaned_dataset_uri,
        report_file_path=report_file_path,
        cleaning_report_uri=cleaning_report_uri,
        metrics=metrics
    )

    cleaning_report_uri = _write_and_upload_report(
        report_file_path=report_file_path,
        cleaned_output=cleaned_output,
        metrics=metrics,
        context=context,
        object_storage=object_storage,
    )

    cleaned_output = _build_cleaning_output(
        cleaned_file_path=cleaned_file_path,
        cleaned_dataset_uri=cleaned_dataset_uri,
        report_file_path=report_file_path,
        cleaning_report_uri=cleaning_report_uri,
        metrics=metrics
    )

    for _ in range(5):
        expected_report_size = _artifact_size(cleaned_output, "CLEANING_REPORT")

        _write_report(
            report_file_path=report_file_path,
            cleaned_output=cleaned_output,
            metrics=metrics,
            experiment_run_uuid=context.experiment_run_uuid,
        )

        actual_report_size = report_file_path.stat().st_size

        if actual_report_size == expected_report_size:
            break

        cleaned_output = _build_cleaning_output(
            cleaned_file_path=cleaned_file_path,
            cleaned_dataset_uri=cleaned_dataset_uri,
            report_file_path=report_file_path,
            cleaning_report_uri=cleaning_report_uri,
            metrics=metrics
        )

    _upload_artifact(
        path=report_file_path,
        context=context,
        object_storage=object_storage,
    )

    return cleaned_output


def _resolve_input_file_path(
    context: CsvProfileAnalysisContext,
    output_dir: Path,
    object_storage: ObjectStorage | None,
) -> Path:
    """Resolve a local input CSV path from a file, plain path, or storage URI."""

    dataset_uri = context.dataset_uri

    parsed_uri = urlparse(dataset_uri)

    if parsed_uri.scheme == "file":
        return Path(unquote(parsed_uri.path))

    if parsed_uri.scheme in ("", None):
        return Path(dataset_uri)

    if parsed_uri.scheme == "s3":
        if object_storage is None:
            raise CsvCleaningError("Object storage is required for s3:// datasetUri")

        input_file_path = output_dir / _safe_filename(Path(parsed_uri.path).name)
        input_file_path.write_bytes(
            object_storage.download_file(_object_key_from_storage_uri(dataset_uri))
        )

        return input_file_path

    raise CsvCleaningError(f"Unsupported datasetUri scheme: {parsed_uri.scheme}")


def _read_csv(input_file_path: Path) -> pd.DataFrame:
    """Read CSV files that may use UTF-8 or common legacy encodings."""

    last_error: UnicodeDecodeError | None = None

    for encoding in CSV_READ_ENCODINGS:
        try:
            return pd.read_csv(input_file_path, encoding=encoding)
        except UnicodeDecodeError as error:
            last_error = error

    raise CsvCleaningError(
        f"Unable to decode CSV file with supported encodings: {CSV_READ_ENCODINGS}"
    ) from last_error


def _resolve_output_dir(context: CsvProfileAnalysisContext) -> Path:
    """Return the configured output directory or the default run-scoped path."""

    if context.output_dir:
        return Path(context.output_dir)

    return Path("/tmp/experimentops/runs") / context.experiment_run_uuid / "csv-profile-analysis"


def _normalize_column_name(column: Any) -> str:
    """Convert a raw CSV column name into a stable snake_case identifier."""

    name = str(column).strip().lower()
    name = re.sub(r"[^a-z0-9]+", "_", name)
    name = re.sub(r"_+", "_", name)
    name = name.strip("_")

    return name or "column"


def _deduplicate_column_names(columns: list[str]) -> list[str]:
    """Append numeric suffixes to repeated column names while preserving order."""

    seen: dict[str, int] = {}
    result: list[str] = []

    for column in columns:
        if column not in seen:
            seen[column] = 0
            result.append(column)
        else:
            seen[column] += 1
            result.append(f"{column}_{seen[column]}")

    return result


def _to_file_uri(path: Path) -> str:
    """Convert a local filesystem path into an absolute file URI."""

    return path.resolve().as_uri()


def _upload_artifact(
    path: Path,
    context: CsvProfileAnalysisContext,
    object_storage: ObjectStorage | None,
) -> str:
    """Upload an artifact to object storage or return a local file URI."""

    if object_storage is None:
        return _to_file_uri(path)

    return object_storage.upload_file(
        key=_artifact_object_key(context, path.name),
        content=path.read_bytes(),
    )


def _write_and_upload_report(
    report_file_path: Path,
    cleaned_output: CsvCleaningOutput,
    metrics: CsvCleaningMetrics,
    context: CsvProfileAnalysisContext,
    object_storage: ObjectStorage | None,
) -> str:
    """Write the cleaning report and publish it as an artifact."""

    _write_report(
        report_file_path=report_file_path,
        cleaned_output=cleaned_output,
        metrics=metrics,
        experiment_run_uuid=context.experiment_run_uuid,
    )

    return _upload_artifact(
        path=report_file_path,
        context=context,
        object_storage=object_storage,
    )


def _build_cleaning_output(
    cleaned_file_path: Path,
    cleaned_dataset_uri: str,
    report_file_path: Path,
    cleaning_report_uri: str,
    metrics: CsvCleaningMetrics
) -> CsvCleaningOutput:
    """Build the serialized cleaning result from artifact paths and metrics."""

    return CsvCleaningOutput(
        artifact=[
            _build_artifact(
                artifact_type="CLEANED_DATASET",
                artifact_format="csv",
                uri=cleaned_dataset_uri,
                path=cleaned_file_path,
            ),
            _build_artifact(
                artifact_type="CLEANING_REPORT",
                artifact_format="json",
                uri=cleaning_report_uri,
                path=report_file_path,
            ),
        ],
        metrics=metrics,
    )


def _build_artifact(
    artifact_type: str,
    artifact_format: str,
    uri: str,
    path: Path,
) -> Artifact:
    """Create artifact metadata for a generated output file."""

    size = path.stat().st_size if path.exists() else 0

    return Artifact(
        format=artifact_format,
        type=artifact_type,
        uri=uri,
        size=size,
    )


def _artifact_size(output: CsvCleaningOutput, artifact_type: str) -> int:
    """Return the recorded size for an artifact in a cleaning result."""

    for artifact in output.artifact:
        if artifact.type == artifact_type:
            return artifact.size

    raise ValueError(f"Missing artifact with type={artifact_type}")


def _write_report(
    report_file_path: Path,
    cleaned_output: CsvCleaningOutput,
    metrics: CsvCleaningMetrics,
    experiment_run_uuid: str,
) -> None:
    """Write the JSON report that captures the cleaning result context."""

    report = CsvCleaningReport(
        status="SUCCEEDED",
        experiment_run_uuid=experiment_run_uuid,
        metrics=metrics,
        context=cleaned_output.model_dump(by_alias=True),
    )

    report_file_path.write_text(
        json.dumps(report.model_dump(by_alias=True), indent=2),
        encoding="utf-8",
    )


def _artifact_object_key(context: CsvProfileAnalysisContext, filename: str) -> str:
    """Build the object-storage key for a run artifact."""

    return (
        f"workspaces/{_key_part(context.workspace_uuid)}/"
        f"projects/{_key_part(context.project_uuid)}/"
        f"experiments/{_key_part(context.experiment_uuid)}/"
        f"runs/{_key_part(context.experiment_run_uuid)}/"
        f"artifacts/csv-profile-analysis/{_safe_filename(filename)}"
    )


def _object_key_from_storage_uri(storage_uri: str) -> str:
    """Extract an object key from a s3 storage URI."""

    parsed_uri = urlparse(storage_uri)

    if parsed_uri.scheme != "s3":
        raise CsvCleaningError(f"Expected s3:// storage URI, got: {storage_uri}")

    object_key = unquote(parsed_uri.path).lstrip("/")

    if not object_key:
        raise CsvCleaningError(f"S3 storage URI must include an object key: {storage_uri}")

    return object_key


def _safe_filename(filename: str) -> str:
    """Replace unsafe object-key filename characters with underscores."""

    return re.sub(r"[^a-zA-Z0-9._-]", "_", filename) or "artifact"


def _key_part(value: str | None) -> str:
    """Sanitize a path segment used in object-storage keys."""

    return _safe_filename(value or "unknown")
