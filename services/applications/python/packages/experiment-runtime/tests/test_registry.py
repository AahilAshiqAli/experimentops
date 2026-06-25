from __future__ import annotations

import sys
from pathlib import Path

import pytest

from experiment_runtime.registry import ExperimentRegistry


def test_discovers_package_executor(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    package_root = _write_package(tmp_path)
    _write_file(
        package_root / "executors" / "csv_profile_analysis" / "__init__.py",
        "",
    )
    _write_file(
        package_root / "executors" / "csv_profile_analysis" / "executor.py",
        """
class CsvProfileAnalysisExecutor:
    def execute(self, context: dict) -> dict:
        return {"context": context}
""",
    )

    monkeypatch.syspath_prepend(str(tmp_path))

    registry = ExperimentRegistry.discover_executors("sample_worker.executors")

    assert registry.supported_types() == ["CSV_PROFILE_ANALYSIS"]
    assert registry.execute("csv_profile_analysis", {"datasetUri": "s3://bucket/key.csv"}) == {
        "context": {"datasetUri": "s3://bucket/key.csv"},
    }


def test_ignores_legacy_executor_module(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    package_root = _write_package(tmp_path)
    _write_file(
        package_root / "executors" / "csv_profile_analysis_executor.py",
        """
class CsvProfileAnalysisExecutor:
    def execute(self, context: dict) -> dict:
        return {"source": "legacy"}
""",
    )

    monkeypatch.syspath_prepend(str(tmp_path))

    registry = ExperimentRegistry.discover_executors("sample_worker.executors")

    assert registry.supported_types() == []


def test_injects_supported_executor_constructor_dependencies(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    package_root = _write_package(tmp_path)
    _write_file(
        package_root / "executors" / "csv_profile_analysis" / "__init__.py",
        "",
    )
    _write_file(
        package_root / "executors" / "csv_profile_analysis" / "executor.py",
        """
class CsvProfileAnalysisExecutor:
    def __init__(self, object_storage=None):
        self.object_storage = object_storage

    def execute(self, context: dict) -> dict:
        return {"objectStorage": self.object_storage}
""",
    )

    monkeypatch.syspath_prepend(str(tmp_path))

    registry = ExperimentRegistry.discover_executors(
        "sample_worker.executors",
        executor_dependencies={"object_storage": "storage", "unused": "ignored"},
    )

    assert registry.execute("CSV_PROFILE_ANALYSIS", {}) == {"objectStorage": "storage"}


def _write_package(tmp_path: Path) -> Path:
    package_root = tmp_path / "sample_worker"
    _write_file(package_root / "__init__.py", "")
    _write_file(package_root / "executors" / "__init__.py", "")
    _clear_sample_worker_modules()
    return package_root


def _write_file(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.lstrip(), encoding="utf-8")


def _clear_sample_worker_modules() -> None:
    for module_name in list(sys.modules):
        if module_name == "sample_worker" or module_name.startswith("sample_worker."):
            del sys.modules[module_name]
