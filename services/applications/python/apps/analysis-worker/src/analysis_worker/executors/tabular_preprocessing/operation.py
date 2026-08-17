# Purpose: Validate feature selections and transform train/test data without test-data leakage.

from __future__ import annotations

from typing import Any

import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

from analysis_worker.executors.tabular_preprocessing.model import (
    TabularPreprocessingConfig,
    TabularPreprocessingMetrics,
)
from analysis_worker.tabular.csv_io import TabularDataError
from analysis_worker.tabular.limits import (
    MAX_CATEGORICAL_CARDINALITY,
    MAX_CONFIGURED_FEATURES,
    MAX_PROCESSED_FEATURES,
)
from analysis_worker.tabular.validation import (
    validate_binary_target,
    validate_finite_numeric_features,
)


def preprocess_datasets(
    train_data: pd.DataFrame,
    test_data: pd.DataFrame,
    config: TabularPreprocessingConfig,
) -> tuple[pd.DataFrame, pd.DataFrame, ColumnTransformer, TabularPreprocessingMetrics]:
    feature_columns = config.numerical_columns + config.categorical_columns
    if len(feature_columns) > MAX_CONFIGURED_FEATURES:
        raise TabularDataError(
            f"Configured feature count exceeds the v1 limit of {MAX_CONFIGURED_FEATURES}"
        )

    _validate_selected_columns(train_data, test_data, config, feature_columns)
    train_target = validate_binary_target(train_data, config.target_column)
    test_target = validate_binary_target(test_data, config.target_column)

    train_features = _normalized_features(train_data, config)
    test_features = _normalized_features(test_data, config)
    transformer = _build_transformer(config)
    try:
        processed_train_values = transformer.fit_transform(train_features)
        processed_test_values = transformer.transform(test_features)
    except ValueError as error:
        raise TabularDataError(f"Unable to preprocess tabular data: {error}") from error

    feature_names = [str(value) for value in transformer.get_feature_names_out()]
    if not feature_names:
        raise TabularDataError("Preprocessing did not produce any usable features")
    if len(feature_names) > MAX_PROCESSED_FEATURES:
        raise TabularDataError(
            f"Processed feature count exceeds the v1 limit of {MAX_PROCESSED_FEATURES}: "
            f"{len(feature_names)}"
        )

    processed_train = pd.DataFrame(processed_train_values, columns=feature_names)
    processed_test = pd.DataFrame(processed_test_values, columns=feature_names)
    processed_train[config.target_column] = train_target.to_numpy()
    processed_test[config.target_column] = test_target.to_numpy()

    metrics = TabularPreprocessingMetrics(
        train_rows=len(processed_train),
        test_rows=len(processed_test),
        input_feature_count=len(feature_columns),
        output_feature_count=len(feature_names),
        target_column=config.target_column,
        numerical_columns=list(config.numerical_columns),
        categorical_columns=list(config.categorical_columns),
        output_feature_names=feature_names,
    )
    return processed_train, processed_test, transformer, metrics


def preprocessor_bundle(
    transformer: ColumnTransformer,
    config: TabularPreprocessingConfig,
    output_feature_names: list[str],
    sklearn_version: str,
) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "bundle_type": "TABULAR_PREPROCESSOR",
        "transformer": transformer,
        "target_column": config.target_column,
        "numerical_columns": list(config.numerical_columns),
        "categorical_columns": list(config.categorical_columns),
        "output_feature_names": list(output_feature_names),
        "sklearn_version": sklearn_version,
    }


def _validate_selected_columns(
    train_data: pd.DataFrame,
    test_data: pd.DataFrame,
    config: TabularPreprocessingConfig,
    feature_columns: list[str],
) -> None:
    required_columns = feature_columns + [config.target_column]
    for label, dataframe in (("train", train_data), ("test", test_data)):
        missing = [name for name in required_columns if name not in dataframe.columns]
        if missing:
            raise TabularDataError(
                f"{label} dataset is missing configured columns: {missing}"
            )

    for column in config.categorical_columns:
        cardinality = int(train_data[column].nunique(dropna=True))
        if cardinality > MAX_CATEGORICAL_CARDINALITY:
            raise TabularDataError(
                f"Categorical column {column} exceeds the v1 cardinality limit of "
                f"{MAX_CATEGORICAL_CARDINALITY}: {cardinality}"
            )


def _normalized_features(
    dataframe: pd.DataFrame,
    config: TabularPreprocessingConfig,
) -> pd.DataFrame:
    features = pd.DataFrame(index=dataframe.index)
    if config.numerical_columns:
        numeric = validate_finite_numeric_features(
            dataframe,
            config.numerical_columns,
            allow_missing=True,
        )
        for column in config.numerical_columns:
            features[column] = numeric[column].astype("float64")

    for column in config.categorical_columns:
        values = dataframe[column].astype("object")
        features[column] = values.where(values.notna(), np.nan)
    return features


def _build_transformer(config: TabularPreprocessingConfig) -> ColumnTransformer:
    transformers: list[tuple[str, Pipeline, list[str]]] = []
    if config.numerical_columns:
        transformers.append(
            (
                "numerical",
                Pipeline(
                    steps=[
                        ("imputer", SimpleImputer(strategy="median")),
                        ("scaler", StandardScaler()),
                    ]
                ),
                config.numerical_columns,
            )
        )
    if config.categorical_columns:
        transformers.append(
            (
                "categorical",
                Pipeline(
                    steps=[
                        ("imputer", SimpleImputer(strategy="most_frequent")),
                        (
                            "encoder",
                            OneHotEncoder(handle_unknown="ignore", sparse_output=False),
                        ),
                    ]
                ),
                config.categorical_columns,
            )
        )
    return ColumnTransformer(transformers=transformers, remainder="drop")
