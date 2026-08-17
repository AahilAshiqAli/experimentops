# Purpose: Bind generic execution contexts to strict experiment configs and named input ports.

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Generic, Mapping, TypeVar

from pydantic import BaseModel, ValidationError

from analysis_worker.contracts.errors import ExperimentContractError
from analysis_worker.contracts.experiment_type import ExperimentTypeContract
from experiment_runtime.models import ExperimentExecutionContext, ExperimentInputContext


ConfigT = TypeVar("ConfigT", bound=BaseModel)


@dataclass(frozen=True)
class BoundExperimentExecution(Generic[ConfigT]):
    config: ConfigT
    inputs: dict[str, ExperimentInputContext]


def bind_execution(
    context: ExperimentExecutionContext,
    contract: ExperimentTypeContract,
    config_model: type[ConfigT],
) -> BoundExperimentExecution[ConfigT]:
    """Validate config and named inputs before experiment-specific execution."""

    config_payload = _config_payload(context.config_json)
    try:
        config = config_model.model_validate(config_payload)
    except ValidationError as error:
        raise ExperimentContractError(
            f"Invalid {contract.experiment_type} config: {error}"
        ) from error

    inputs_by_contract = {item.port_name: item for item in contract.inputs}
    unexpected_ports = sorted(set(context.inputs) - set(inputs_by_contract))
    if unexpected_ports:
        raise ExperimentContractError(
            f"Unexpected input ports for {contract.experiment_type}: {unexpected_ports}"
        )

    for port_name, input_contract in inputs_by_contract.items():
        input_context = context.inputs.get(port_name)
        if input_context is None:
            if input_contract.required:
                raise ExperimentContractError(
                    f"Missing required input port for {contract.experiment_type}: {port_name}"
                )
            continue

        _validate_input(contract, input_contract, input_context)

    return BoundExperimentExecution(config=config, inputs=dict(context.inputs))


def _config_payload(value: Any) -> Mapping[str, Any]:
    if value is None:
        return {}
    if not isinstance(value, Mapping):
        raise ExperimentContractError("experimentConfigJson must be a JSON object")
    return value


def _validate_input(
    contract: ExperimentTypeContract,
    input_contract: Any,
    input_context: ExperimentInputContext,
) -> None:
    input_type = _normalized(input_context.input_type)
    if input_type not in input_contract.accepted_input_types:
        raise ExperimentContractError(
            f"Input port {input_contract.port_name} for {contract.experiment_type} "
            f"does not accept inputType={input_context.input_type}"
        )

    data_kind = _normalized(input_context.data_kind)
    if data_kind != input_contract.data_kind:
        raise ExperimentContractError(
            f"Input port {input_contract.port_name} for {contract.experiment_type} "
            f"expects dataKind={input_contract.data_kind}, actual={input_context.data_kind}"
        )

    input_format = _normalized(input_context.format)
    if input_format not in input_contract.accepted_formats:
        raise ExperimentContractError(
            f"Input port {input_contract.port_name} for {contract.experiment_type} "
            f"does not accept format={input_context.format}"
        )

    if input_context.uri is None or not input_context.uri.strip():
        raise ExperimentContractError(
            f"Input port {input_contract.port_name} for {contract.experiment_type} has no URI"
        )


def _normalized(value: str | None) -> str:
    return value.strip().upper() if value else ""
