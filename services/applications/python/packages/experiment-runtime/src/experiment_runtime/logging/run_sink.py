from __future__ import annotations

import json
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from pydantic import Field

from experiment_runtime.base_model import ExperimentOpsModel

PROGRESS_LOG_LEVELS = frozenset({"INFO", "ERROR"})


class ExperimentRunUserLogRecord(ExperimentOpsModel):
    timestamp: int = Field(default_factory=lambda: _current_epoch_millis())
    level: str
    experiment_type: str | None = None
    message: str

    def to_payload(self) -> dict[str, Any]:
        return self.model_dump(by_alias=True, mode="json", exclude_none=True)


class ExperimentRunLogSink:
    """Append run-scoped user logs to JSONL and buffer progress-safe logs."""

    def __init__(self, log_file_path: Path) -> None:
        self._log_file_path = log_file_path
        self._pending_progress_logs: list[ExperimentRunUserLogRecord] = []
        self._log_file_path.parent.mkdir(parents=True, exist_ok=True)
        self._log_file_path.touch(exist_ok=True)

    @property
    def log_file_path(self) -> Path:
        return self._log_file_path

    def info(self, experiment_type: str | None, message: str, *args: object) -> None:
        self.log("INFO", experiment_type, message, *args)

    def error(self, experiment_type: str | None, message: str, *args: object) -> None:
        self.log("ERROR", experiment_type, message, *args)

    def warning(self, experiment_type: str | None, message: str, *args: object) -> None:
        self.log("WARNING", experiment_type, message, *args)

    def verbose(self, experiment_type: str | None, message: str, *args: object) -> None:
        self.log("VERBOSE", experiment_type, message, *args)

    def log(
        self,
        level: str,
        experiment_type: str | None,
        message: str,
        *args: object,
    ) -> None:
        normalized_level = level.strip().upper()
        rendered_message = message % args if args else message
        record = ExperimentRunUserLogRecord(
            level=normalized_level,
            experiment_type=experiment_type,
            message=rendered_message,
        )

        with self._log_file_path.open("a", encoding="utf-8") as log_file:
            log_file.write(json.dumps(record.to_payload(), separators=(",", ":")))
            log_file.write("\n")

        if normalized_level in PROGRESS_LOG_LEVELS:
            self._pending_progress_logs.append(record)

    def drain_progress_logs(self) -> list[ExperimentRunUserLogRecord]:
        logs = self._pending_progress_logs
        self._pending_progress_logs = []
        return logs

    def has_pending_progress_logs(self) -> bool:
        return bool(self._pending_progress_logs)

    def read_bytes_for_upload(self) -> bytes:
        return self._log_file_path.read_bytes()


def _current_epoch_millis() -> int:
    return int(datetime.now(UTC).timestamp() * 1000)
