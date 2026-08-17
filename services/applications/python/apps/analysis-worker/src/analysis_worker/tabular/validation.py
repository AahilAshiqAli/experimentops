# Purpose: Provide reusable binary-target and finite-numeric feature validation.

from __future__ import annotations

from collections.abc import Iterable

import numpy as np
import pandas as pd

from analysis_worker.tabular.csv_io import TabularDataError


def validate_binary_target(
    dataframe: pd.DataFrame,
    target_column: str,
    *,
    minimum_rows_per_class: int = 1,
) -> pd.Series:
    if target_column not in dataframe.columns:
        raise TabularDataError(f"Target column does not exist: {target_column}")

    target = dataframe[target_column]
    if target.isna().any():
        raise TabularDataError(
            f"Target column contains missing values: {target_column}"
        )

    try:
        numeric_target = pd.to_numeric(target, errors="raise")
    except (TypeError, ValueError) as error:
        raise TabularDataError(
            f"Target column must contain only integer class labels 0 and 1: {target_column}"
        ) from error

    if not np.isfinite(numeric_target.to_numpy(dtype=float)).all():
        raise TabularDataError(
            f"Target column contains non-finite values: {target_column}"
        )
    if not numeric_target.isin([0, 1]).all():
        raise TabularDataError(
            f"Target column must contain only integer class labels 0 and 1: {target_column}"
        )

    normalized = numeric_target.astype("int64")
    counts = normalized.value_counts().to_dict()
    if set(counts) != {0, 1}:
        raise TabularDataError(
            f"Target column must contain both classes 0 and 1: {target_column}"
        )
    undersized = {
        label: count
        for label, count in counts.items()
        if count < minimum_rows_per_class
    }
    if undersized:
        raise TabularDataError(
            f"Target classes have fewer than {minimum_rows_per_class} rows: {undersized}"
        )
    return normalized


def binary_class_counts(target: pd.Series) -> dict[str, int]:
    counts = target.astype("int64").value_counts().sort_index()
    return {str(int(label)): int(count) for label, count in counts.items()}


def validate_finite_numeric_features(
    dataframe: pd.DataFrame,
    feature_columns: Iterable[str],
    *,
    allow_missing: bool,
) -> pd.DataFrame:
    feature_names = list(feature_columns)
    missing_columns = [name for name in feature_names if name not in dataframe.columns]
    if missing_columns:
        raise TabularDataError(f"Feature columns do not exist: {missing_columns}")

    numeric = pd.DataFrame(index=dataframe.index)
    for feature_name in feature_names:
        try:
            numeric[feature_name] = pd.to_numeric(
                dataframe[feature_name],
                errors="raise",
            )
        except (TypeError, ValueError) as error:
            raise TabularDataError(
                f"Feature column must contain numeric values: {feature_name}"
            ) from error

    if not allow_missing and numeric.isna().any().any():
        missing = numeric.columns[numeric.isna().any()].tolist()
        raise TabularDataError(
            f"Processed feature columns contain missing values: {missing}"
        )

    finite_values = numeric.to_numpy(dtype=float)
    if allow_missing:
        finite_values = finite_values[~np.isnan(finite_values)]
    if not np.isfinite(finite_values).all():
        raise TabularDataError("Feature columns contain non-finite values")
    return numeric
