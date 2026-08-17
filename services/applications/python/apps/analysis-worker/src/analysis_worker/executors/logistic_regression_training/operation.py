# Purpose: Validate processed training data and fit a deterministic logistic-regression model.

from __future__ import annotations

import warnings
from typing import Any

import pandas as pd
from sklearn.exceptions import ConvergenceWarning
from sklearn.linear_model import LogisticRegression

from analysis_worker.executors.logistic_regression_training.model import (
    LogisticRegressionTrainingConfig,
    LogisticRegressionTrainingMetrics,
)
from analysis_worker.tabular.csv_io import TabularDataError
from analysis_worker.tabular.limits import MAX_PROCESSED_FEATURES, MIN_CLASS_ROWS
from analysis_worker.tabular.validation import (
    validate_binary_target,
    validate_finite_numeric_features,
)


def train_logistic_regression(
    training_data: pd.DataFrame,
    config: LogisticRegressionTrainingConfig,
) -> tuple[LogisticRegression, LogisticRegressionTrainingMetrics]:
    target = validate_binary_target(
        training_data,
        config.target_column,
        minimum_rows_per_class=MIN_CLASS_ROWS,
    )
    feature_names = [
        name for name in training_data.columns if name != config.target_column
    ]
    if not feature_names:
        raise TabularDataError("Training dataset does not contain any feature columns")
    if len(feature_names) > MAX_PROCESSED_FEATURES:
        raise TabularDataError(
            f"Training feature count exceeds the v1 limit of {MAX_PROCESSED_FEATURES}"
        )

    features = validate_finite_numeric_features(
        training_data,
        feature_names,
        allow_missing=False,
    )
    estimator = LogisticRegression(
        C=config.c,
        max_iter=config.max_iter,
        class_weight=None if config.class_weight == "NONE" else "balanced",
        random_state=config.random_seed,
        solver="liblinear",
    )
    with warnings.catch_warnings(record=True) as captured_warnings:
        warnings.simplefilter("always", ConvergenceWarning)
        estimator.fit(features, target)

    if any(issubclass(item.category, ConvergenceWarning) for item in captured_warnings):
        raise TabularDataError(
            "Logistic regression did not converge; increase maxIter or revise the features"
        )

    hyperparameters: dict[str, object] = {
        "c": config.c,
        "maxIter": config.max_iter,
        "classWeight": config.class_weight,
        "randomSeed": config.random_seed,
        "solver": "liblinear",
    }
    metrics = LogisticRegressionTrainingMetrics(
        target_column=config.target_column,
        training_rows=len(training_data),
        feature_count=len(feature_names),
        feature_names=feature_names,
        classes=[int(value) for value in estimator.classes_.tolist()],
        iterations=int(estimator.n_iter_.max()),
        hyperparameters=hyperparameters,
    )
    return estimator, metrics


def model_bundle(
    estimator: LogisticRegression,
    metrics: LogisticRegressionTrainingMetrics,
    sklearn_version: str,
) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "bundle_type": "EXPERIMENTOPS_MODEL",
        "algorithm": metrics.algorithm,
        "problem_type": metrics.problem_type,
        "estimator": estimator,
        "feature_names": list(metrics.feature_names),
        "target_column": metrics.target_column,
        "class_labels": list(metrics.classes),
        "training_rows": metrics.training_rows,
        "hyperparameters": dict(metrics.hyperparameters),
        "sklearn_version": sklearn_version,
    }
