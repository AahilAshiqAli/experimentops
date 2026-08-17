# Purpose: Declare the named inputs and outputs for reusable tabular preprocessing.

from analysis_worker.contracts import (
    ExperimentInputContract,
    ExperimentOutputContract,
    ExperimentTypeContract,
)


CONTRACT = ExperimentTypeContract(
    experiment_type="TABULAR_PREPROCESSING",
    inputs=(
        ExperimentInputContract(
            port_name="trainDataset",
            data_kind="TABULAR_DATASET",
            accepted_formats=("CSV",),
        ),
        ExperimentInputContract(
            port_name="testDataset",
            data_kind="TABULAR_DATASET",
            accepted_formats=("CSV",),
        ),
    ),
    outputs=(
        ExperimentOutputContract(
            name="processedTrainDataset",
            data_kind="TABULAR_DATASET",
            format="CSV",
            downstream_policy="CONNECTABLE",
        ),
        ExperimentOutputContract(
            name="processedTestDataset",
            data_kind="TABULAR_DATASET",
            format="CSV",
            downstream_policy="CONNECTABLE",
        ),
        ExperimentOutputContract(
            name="preprocessorBundle",
            data_kind="FILE",
            format="PICKLE",
            downstream_policy="CONNECTABLE",
        ),
        ExperimentOutputContract(
            name="preprocessingReport",
            data_kind="REPORT",
            format="JSON",
            downstream_policy="TERMINAL",
        ),
    ),
)
