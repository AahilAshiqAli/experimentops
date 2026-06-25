from __future__ import annotations

import importlib
import importlib.util
import inspect
import pkgutil
from collections.abc import Callable, Mapping
from types import ModuleType
from typing import Protocol, cast

from experiment_runtime.models import ExperimentExecutionContext
from experiment_runtime.models.experiment_run_completed_event import Result


EXPERIMENT_TYPE_EMPTY_ERROR = "experiment_type cannot be empty"
ExecutorDependencies = Mapping[str, object]


class ExperimentExecutor(Protocol):
    """
    Any experiment executor must follow this shape.
    For now, every executor should have an execute() method.
    """

    def execute(self, context: ExperimentExecutionContext) -> Result:
        ...

class ExperimentRegistry:
    """
    Registry maps experiment_type -> executor.

    Example:
        CSV_PROFILE_ANALYSIS -> CsvProfileAnalysisExecutor
        TEXT_CLASSIFICATION -> TextClassificationExecutor
    """

    def __init__(self) -> None:
        self._executors: dict[str, ExperimentExecutor] = {}

    @classmethod
    def discover_executors(
        cls,
        package_name: str,
        executor_dependencies: ExecutorDependencies | None = None,
    ) -> "ExperimentRegistry":
        registry = cls()
        registry.load_executors(
            package_name=package_name,
            executor_dependencies=executor_dependencies,
        )
        return registry

    def load_executors(
        self,
        package_name: str,
        executor_dependencies: ExecutorDependencies | None = None,
    ) -> None:
        package = importlib.import_module(package_name)
        package_paths = getattr(package, "__path__", None)
        dependencies = executor_dependencies or {}

        if package_paths is None:
            raise ValueError(f"Executor package must be a package: {package_name}")

        for module_info in pkgutil.iter_modules(
            package_paths,
            prefix=f"{package.__name__}.",
        ):
            if not module_info.ispkg:
                continue

            executor_module_name = f"{module_info.name}.executor"

            if importlib.util.find_spec(executor_module_name) is None:
                continue

            experiment_type = _experiment_type_from_module_name(
                module_info.name.rsplit(".", maxsplit=1)[-1]
            )
            module = importlib.import_module(executor_module_name)
            executor = _create_executor_from_module(module, dependencies)
            self.register(experiment_type=experiment_type, executor=executor)

    def register(self, experiment_type: str, executor: ExperimentExecutor) -> None:
        normalized_experiment_type = experiment_type.strip().upper()

        if not normalized_experiment_type:
            raise ValueError(EXPERIMENT_TYPE_EMPTY_ERROR)

        if executor is None:
            raise ValueError("executor cannot be None")

        if normalized_experiment_type in self._executors:
            raise ValueError(
                f"Executor already registered for experiment_type={normalized_experiment_type}"
            )

        self._executors[normalized_experiment_type] = executor


    def get_executor(self, experiment_type: str) -> ExperimentExecutor:
        if not experiment_type:
            raise ValueError(EXPERIMENT_TYPE_EMPTY_ERROR)

        normalized_experiment_type = experiment_type.strip().upper()

        if not normalized_experiment_type:
            raise ValueError(EXPERIMENT_TYPE_EMPTY_ERROR)

        executor = self._executors.get(normalized_experiment_type)

        if executor is None:
            raise ValueError(f"No executor registered for experiment_type={experiment_type}")

        return executor

    def execute(self, experiment_type: str, context: ExperimentExecutionContext) -> Result:
        executor = self.get_executor(experiment_type)
        return executor.execute(context)

    def supported_types(self) -> list[str]:
        return sorted(self._executors.keys())


def _experiment_type_from_module_name(module_name: str) -> str:
    return module_name.upper()


def _create_executor_from_module(
    module: ModuleType,
    dependencies: ExecutorDependencies,
) -> ExperimentExecutor:
    factory = getattr(module, "create_executor", None)

    if factory is not None:
        if not callable(factory):
            raise ValueError(f"{module.__name__}.create_executor must be callable")

        return _validate_executor(
            _call_with_supported_dependencies(
                factory=cast(Callable[..., object], factory),
                dependencies=dependencies,
            ),
            module.__name__,
        )

    executor_class = getattr(module, "EXECUTOR_CLASS", None)

    if executor_class is not None:
        if not callable(executor_class):
            raise ValueError(f"{module.__name__}.EXECUTOR_CLASS must be callable")

        return _instantiate_executor(
            cast(Callable[..., object], executor_class),
            module.__name__,
            dependencies,
        )

    executor_classes: list[Callable[..., object]] = [
        value
        for _, value in inspect.getmembers(module, inspect.isclass)
        if value.__module__ == module.__name__ and value.__name__.endswith("Executor")
    ]

    if len(executor_classes) != 1:
        raise ValueError(
            f"Expected exactly one executor class in {module.__name__}; "
            f"found {len(executor_classes)}"
        )

    return _instantiate_executor(
        executor_classes[0],
        module.__name__,
        dependencies,
    )


def _instantiate_executor(
    executor_class: Callable[..., object],
    module_name: str,
    dependencies: ExecutorDependencies,
) -> ExperimentExecutor:
    return _validate_executor(
        _call_with_supported_dependencies(
            factory=executor_class,
            dependencies=dependencies,
        ),
        module_name,
    )


def _call_with_supported_dependencies(
    factory: Callable[..., object],
    dependencies: ExecutorDependencies,
) -> object:
    signature = inspect.signature(factory)
    supported_dependencies = {
        name: value
        for name, value in dependencies.items()
        if name in signature.parameters
    }

    return factory(**supported_dependencies)


def _validate_executor(
    executor: object,
    module_name: str,
) -> ExperimentExecutor:
    if executor is None:
        raise ValueError(f"{module_name} did not create an executor")

    execute = getattr(executor, "execute", None)

    if not callable(execute):
        raise ValueError(f"Executor in {module_name} must define execute(context)")

    return cast(ExperimentExecutor, executor)
