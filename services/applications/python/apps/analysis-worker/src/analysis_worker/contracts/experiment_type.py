# Purpose: Model reusable experiment input/output contracts and generate platform manifests.

from __future__ import annotations

from dataclasses import dataclass


def _normalize(value: str) -> str:
    return value.strip().upper()


@dataclass(frozen=True)
class ExperimentInputContract:
    port_name: str
    data_kind: str
    accepted_formats: tuple[str, ...]
    accepted_input_types: tuple[str, ...] = ("DATASET", "ARTIFACT")
    required: bool = True

    def __post_init__(self) -> None:
        object.__setattr__(self, "data_kind", _normalize(self.data_kind))
        object.__setattr__(
            self,
            "accepted_formats",
            tuple(_normalize(value) for value in self.accepted_formats),
        )
        object.__setattr__(
            self,
            "accepted_input_types",
            tuple(_normalize(value) for value in self.accepted_input_types),
        )


@dataclass(frozen=True)
class ExperimentOutputContract:
    name: str
    data_kind: str
    format: str
    downstream_policy: str
    required: bool = True

    def __post_init__(self) -> None:
        object.__setattr__(self, "data_kind", _normalize(self.data_kind))
        object.__setattr__(self, "format", _normalize(self.format))
        object.__setattr__(
            self,
            "downstream_policy",
            _normalize(self.downstream_policy),
        )


@dataclass(frozen=True)
class ExperimentTypeContract:
    experiment_type: str
    inputs: tuple[ExperimentInputContract, ...]
    outputs: tuple[ExperimentOutputContract, ...]

    def __post_init__(self) -> None:
        object.__setattr__(
            self,
            "experiment_type",
            _normalize(self.experiment_type),
        )

        input_names = [item.port_name for item in self.inputs]
        output_names = [item.name for item in self.outputs]
        if len(input_names) != len(set(input_names)):
            raise ValueError("Experiment contract input port names must be unique")
        if len(output_names) != len(set(output_names)):
            raise ValueError("Experiment contract output names must be unique")

    def to_manifest(self) -> dict[str, object]:
        """Return the platform-compatible port manifest for future registration."""

        return {
            "inputs": [
                {
                    "portName": item.port_name,
                    "required": item.required,
                    "cardinality": "ONE",
                    "contract": {
                        "dataKind": item.data_kind,
                        "acceptedFormats": list(item.accepted_formats),
                    },
                }
                for item in self.inputs
            ],
            "outputs": [
                {
                    "name": item.name,
                    "required": item.required,
                    "dataKind": item.data_kind,
                    "type": {"type": "FIXED", "format": item.format},
                    "downStreamPolicy": item.downstream_policy,
                }
                for item in self.outputs
            ],
        }
