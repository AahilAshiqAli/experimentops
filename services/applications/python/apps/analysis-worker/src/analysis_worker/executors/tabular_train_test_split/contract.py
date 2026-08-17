# Purpose: Declare the named input and outputs for deterministic train/test splitting.

from analysis_worker.contracts import (
    ExperimentInputContract,
    ExperimentOutputContract,
    ExperimentTypeContract,
)


CONTRACT = ExperimentTypeContract(
    experiment_type="TABULAR_TRAIN_TEST_SPLIT",
    inputs=(
        ExperimentInputContract(
            port_name="dataset",
            data_kind="TABULAR_DATASET",
            accepted_formats=("CSV",),
        ),
    ),
    outputs=(
        ExperimentOutputContract(
            name="trainDataset",
            data_kind="TABULAR_DATASET",
            format="CSV",
            downstream_policy="CONNECTABLE",
        ),
        ExperimentOutputContract(
            name="testDataset",
            data_kind="TABULAR_DATASET",
            format="CSV",
            downstream_policy="CONNECTABLE",
        ),
        ExperimentOutputContract(
            name="splitReport",
            data_kind="REPORT",
            format="JSON",
            downstream_policy="TERMINAL",
        ),
    ),
)
