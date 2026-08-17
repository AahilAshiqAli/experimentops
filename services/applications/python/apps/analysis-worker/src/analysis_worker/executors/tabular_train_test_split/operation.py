# Purpose: Validate and reproducibly divide a binary-classification dataset.

from __future__ import annotations

import pandas as pd
from sklearn.model_selection import train_test_split

from analysis_worker.executors.tabular_train_test_split.model import (
    TabularTrainTestSplitConfig,
    TabularTrainTestSplitMetrics,
)
from analysis_worker.tabular.csv_io import TabularDataError
from analysis_worker.tabular.limits import MIN_CLASS_ROWS, MIN_SOURCE_ROWS
from analysis_worker.tabular.validation import (
    binary_class_counts,
    validate_binary_target,
)


def split_dataframe(
    dataframe: pd.DataFrame,
    config: TabularTrainTestSplitConfig,
) -> tuple[pd.DataFrame, pd.DataFrame, TabularTrainTestSplitMetrics]:
    if len(dataframe) < MIN_SOURCE_ROWS:
        raise TabularDataError(
            f"Dataset must contain at least {MIN_SOURCE_ROWS} rows; actual={len(dataframe)}"
        )

    target = validate_binary_target(
        dataframe,
        config.target_column,
        minimum_rows_per_class=MIN_CLASS_ROWS,
    )
    normalized = dataframe.copy()
    normalized[config.target_column] = target

    stratify_target = target if config.stratify else None
    try:
        train_data, test_data = train_test_split(
            normalized,
            test_size=config.test_size,
            random_state=config.random_seed,
            shuffle=True,
            stratify=stratify_target,
        )
    except ValueError as error:
        raise TabularDataError(f"Unable to split dataset: {error}") from error

    train_data = train_data.sort_index().reset_index(drop=True)
    test_data = test_data.sort_index().reset_index(drop=True)
    train_target = validate_binary_target(train_data, config.target_column)
    test_target = validate_binary_target(test_data, config.target_column)

    metrics = TabularTrainTestSplitMetrics(
        source_rows=len(normalized),
        train_rows=len(train_data),
        test_rows=len(test_data),
        target_column=config.target_column,
        test_size=config.test_size,
        random_seed=config.random_seed,
        stratified=config.stratify,
        class_distribution={
            "source": binary_class_counts(target),
            "train": binary_class_counts(train_target),
            "test": binary_class_counts(test_target),
        },
    )
    return train_data, test_data, metrics
