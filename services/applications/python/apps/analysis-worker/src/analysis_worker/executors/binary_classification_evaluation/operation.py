# Purpose: Validate a model bundle and compute binary-classification metrics on test data.

from __future__ import annotations

from collections.abc import Mapping
from typing import Any

import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    accuracy_score,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)

from analysis_worker.executors.binary_classification_evaluation.model import (
    BinaryClassificationEvaluationConfig,
    BinaryClassificationEvaluationMetrics,
)
from analysis_worker.tabular.csv_io import TabularDataError
from analysis_worker.tabular.validation import (
    validate_binary_target,
    validate_finite_numeric_features,
)


def evaluate_binary_classifier(
    bundle: Any,
    test_data: pd.DataFrame,
    config: BinaryClassificationEvaluationConfig,
) -> BinaryClassificationEvaluationMetrics:
    validated_bundle = _validate_model_bundle(bundle)
    estimator = validated_bundle["estimator"]
    feature_names = validated_bundle["feature_names"]
    target_column = validated_bundle["target_column"]

    target = validate_binary_target(test_data, target_column)
    actual_feature_names = [name for name in test_data.columns if name != target_column]
    if actual_feature_names != feature_names:
        raise TabularDataError(
            "Evaluation feature schema does not match the model bundle. "
            f"expected={feature_names} actual={actual_feature_names}"
        )
    features = validate_finite_numeric_features(
        test_data,
        feature_names,
        allow_missing=False,
    )

    positive_class_index = list(estimator.classes_).index(1)
    probabilities = estimator.predict_proba(features)[:, positive_class_index]
    predictions = (probabilities >= config.decision_threshold).astype("int64")
    tn, fp, fn, tp = confusion_matrix(target, predictions, labels=[0, 1]).ravel()

    return BinaryClassificationEvaluationMetrics(
        target_column=target_column,
        rows_evaluated=len(test_data),
        decision_threshold=config.decision_threshold,
        accuracy=float(accuracy_score(target, predictions)),
        precision=float(precision_score(target, predictions, zero_division=0)),
        recall=float(recall_score(target, predictions, zero_division=0)),
        f1=float(f1_score(target, predictions, zero_division=0)),
        roc_auc=float(roc_auc_score(target, probabilities)),
        true_negative=int(tn),
        false_positive=int(fp),
        false_negative=int(fn),
        true_positive=int(tp),
    )


def _validate_model_bundle(bundle: Any) -> dict[str, Any]:
    if not isinstance(bundle, Mapping):
        raise TabularDataError("Model artifact is not an ExperimentOps model bundle")
    if bundle.get("schema_version") != 1:
        raise TabularDataError("Model bundle schemaVersion is not supported")
    if bundle.get("bundle_type") != "EXPERIMENTOPS_MODEL":
        raise TabularDataError("Model artifact has an unsupported bundleType")
    if bundle.get("algorithm") != "LOGISTIC_REGRESSION":
        raise TabularDataError("Evaluation only supports LOGISTIC_REGRESSION bundles")
    if bundle.get("problem_type") != "BINARY_CLASSIFICATION":
        raise TabularDataError("Evaluation only supports BINARY_CLASSIFICATION bundles")

    estimator = bundle.get("estimator")
    if not isinstance(estimator, LogisticRegression):
        raise TabularDataError(
            "Model bundle does not contain a LogisticRegression estimator"
        )
    if [int(value) for value in estimator.classes_.tolist()] != [0, 1]:
        raise TabularDataError("Model estimator classes must be exactly [0, 1]")

    feature_names = bundle.get("feature_names")
    if (
        not isinstance(feature_names, list)
        or not feature_names
        or any(not isinstance(value, str) or not value for value in feature_names)
    ):
        raise TabularDataError("Model bundle featureNames are invalid")
    if len(feature_names) != len(set(feature_names)):
        raise TabularDataError("Model bundle featureNames must be unique")

    target_column = bundle.get("target_column")
    if not isinstance(target_column, str) or not target_column:
        raise TabularDataError("Model bundle targetColumn is invalid")

    return {
        "estimator": estimator,
        "feature_names": feature_names,
        "target_column": target_column,
    }
