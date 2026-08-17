# Purpose: Read CSV inputs safely while enforcing worker resource and schema limits.

from __future__ import annotations

from pathlib import Path

import pandas as pd

from analysis_worker.tabular.limits import (
    MAX_FILE_BYTES,
    MAX_SOURCE_COLUMNS,
    MAX_SOURCE_ROWS,
)


CSV_READ_ENCODINGS = ("utf-8", "utf-8-sig", "cp1252", "latin1")


class TabularDataError(ValueError):
    """Raised when a tabular input violates the worker's bounded v1 contract."""


def read_csv(
    path: Path,
    *,
    max_columns: int = MAX_SOURCE_COLUMNS,
    max_rows: int = MAX_SOURCE_ROWS,
    max_file_bytes: int = MAX_FILE_BYTES,
) -> pd.DataFrame:
    if not path.is_file():
        raise TabularDataError(f"CSV input does not exist: {path}")
    if path.stat().st_size > max_file_bytes:
        raise TabularDataError(
            f"CSV input exceeds the {max_file_bytes}-byte v1 limit: {path.stat().st_size}"
        )

    dataframe = _read_with_supported_encoding(path)
    if len(dataframe) > max_rows:
        raise TabularDataError(
            f"CSV input exceeds the {max_rows}-row v1 limit: {len(dataframe)}"
        )
    if len(dataframe.columns) > max_columns:
        raise TabularDataError(
            f"CSV input exceeds the {max_columns}-column v1 limit: {len(dataframe.columns)}"
        )

    columns = [str(value).strip() for value in dataframe.columns]
    if any(not value for value in columns):
        raise TabularDataError("CSV column names must be non-empty")
    if len(columns) != len(set(columns)):
        raise TabularDataError("CSV column names must be unique")
    dataframe.columns = columns
    return dataframe


def _read_with_supported_encoding(path: Path) -> pd.DataFrame:
    last_decode_error: UnicodeDecodeError | None = None
    for encoding in CSV_READ_ENCODINGS:
        try:
            return pd.read_csv(path, encoding=encoding)
        except UnicodeDecodeError as error:
            last_decode_error = error
        except (pd.errors.EmptyDataError, pd.errors.ParserError) as error:
            raise TabularDataError(f"Invalid CSV input: {error}") from error

    raise TabularDataError(
        f"Unable to decode CSV with supported encodings: {CSV_READ_ENCODINGS}"
    ) from last_decode_error
