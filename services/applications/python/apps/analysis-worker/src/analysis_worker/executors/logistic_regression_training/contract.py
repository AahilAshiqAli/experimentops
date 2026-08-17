# Purpose: Declare the named inputs and outputs for logistic-regression training.

from analysis_worker.contracts import (
    ExperimentInputContract,
    ExperimentOutputContract,
    ExperimentTypeContract,
)


CONTRACT = ExperimentTypeContract(
    experiment_type="LOGISTIC_REGRESSION_TRAINING",
    inputs=(
        ExperimentInputContract(
            port_name="trainDataset",
            data_kind="TABULAR_DATASET",
            accepted_formats=("CSV",),
            accepted_input_types=("ARTIFACT",),
        ),
    ),
    outputs=(
        ExperimentOutputContract(
            name="modelBundle",
            data_kind="MODEL",
            format="PICKLE",
            downstream_policy="CONNECTABLE",
        ),
        ExperimentOutputContract(
            name="trainingReport",
            data_kind="REPORT",
            format="JSON",
            downstream_policy="TERMINAL",
        ),
    ),
)
