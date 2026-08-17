# Purpose: Declare the named inputs and outputs for binary-classification evaluation.

from analysis_worker.contracts import (
    ExperimentInputContract,
    ExperimentOutputContract,
    ExperimentTypeContract,
)


CONTRACT = ExperimentTypeContract(
    experiment_type="BINARY_CLASSIFICATION_EVALUATION",
    inputs=(
        ExperimentInputContract(
            port_name="model",
            data_kind="MODEL",
            accepted_formats=("PICKLE",),
            accepted_input_types=("ARTIFACT",),
        ),
        ExperimentInputContract(
            port_name="testDataset",
            data_kind="TABULAR_DATASET",
            accepted_formats=("CSV",),
            accepted_input_types=("ARTIFACT",),
        ),
    ),
    outputs=(
        ExperimentOutputContract(
            name="evaluationReport",
            data_kind="REPORT",
            format="JSON",
            downstream_policy="TERMINAL",
        ),
    ),
)
