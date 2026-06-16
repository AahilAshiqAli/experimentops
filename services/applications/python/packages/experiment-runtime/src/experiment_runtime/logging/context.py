from __future__ import annotations

import logging
from collections.abc import Iterator, Mapping
from contextlib import contextmanager
from contextvars import ContextVar, Token
from dataclasses import dataclass
from typing import TYPE_CHECKING, Any, Self

if TYPE_CHECKING:
    from experiment_runtime.kafka.message import KafkaMessage

NA = "N/A"

@dataclass(frozen=True)
class ExperimentOpsHeaders:
    """Request metadata that should be printed with application log messages."""

    request_uuid: str = NA
    workspace_uuid: str = NA
    user_uuid: str = NA
    user_role: str = NA
    project_uuid: str = NA
    experiment_uuid: str = NA
    experiment_run_uuid: str = NA

    @classmethod
    def from_mapping(cls, payload: Mapping[str, Any] | None) -> Self:
        """Build log headers from Kafka event metadata."""
        if not payload:
            return cls()

        return cls(
            request_uuid=str(payload.get("requestUuid") or NA),
            workspace_uuid=str(payload.get("workspaceUuid") or NA),
            user_uuid=str(payload.get("requesterUuid") or NA),
            user_role=str(payload.get("userRole") or NA),
        )

    @classmethod
    def from_kafka_message(cls, message: KafkaMessage) -> Self:
        """Extract log headers from the event metadata inside a Kafka message value."""
        value = message.value

        if not isinstance(value, Mapping):
            return cls()

        event = value.get("payload") or value
        if not isinstance(event, Mapping):
            return cls()

        metadata = event.get("metadata")

        if isinstance(metadata, Mapping):
            return cls.from_mapping(metadata)

        return cls.from_mapping(event)


_current_headers: ContextVar[ExperimentOpsHeaders | None] = ContextVar(
    "experiment_ops_headers",
    default=None,
)


def get_current_headers() -> ExperimentOpsHeaders | None:
    """Return headers currently attached to this execution context."""
    return _current_headers.get()


def set_current_headers(headers: ExperimentOpsHeaders | None) -> Token[ExperimentOpsHeaders | None]:
    """Attach headers to logs emitted in the current execution context."""
    return _current_headers.set(headers)


def reset_current_headers(token: Token[ExperimentOpsHeaders | None]) -> None:
    """Restore the previous logging headers context."""
    _current_headers.reset(token)


@contextmanager
def logging_context(headers: ExperimentOpsHeaders | None) -> Iterator[None]:
    """Temporarily attach headers to logs emitted inside the context block."""
    token = set_current_headers(headers)

    try:
        yield
    finally:
        reset_current_headers(token)


@contextmanager
def kafka_message_logging_context(message: KafkaMessage) -> Iterator[None]:
    """Temporarily attach headers extracted from a Kafka message event payload."""
    with logging_context(ExperimentOpsHeaders.from_kafka_message(message)):
        yield


class ExperimentOpsLogger:
    """Logger wrapper that prefixes messages with ExperimentOps request headers."""

    def __init__(self, delegate: logging.Logger):
        self._delegate = delegate

    @classmethod
    def get_logger(cls, name: str | type[Any]) -> Self:
        """Create a logger from a logger name or class."""
        logger_name = name if isinstance(name, str) else name.__name__
        return cls(logging.getLogger(logger_name))

    def debug(self, *args: Any, **kwargs: Any) -> None:
        """Log a DEBUG message with explicit or contextual headers."""
        self._log(logging.DEBUG, *args, **kwargs)

    def info(self, *args: Any, **kwargs: Any) -> None:
        """Log an INFO message with explicit or contextual headers."""
        self._log(logging.INFO, *args, **kwargs)

    def warning(self, *args: Any, **kwargs: Any) -> None:
        """Log a WARNING message with explicit or contextual headers."""
        self._log(logging.WARNING, *args, **kwargs)

    def warn(self, *args: Any, **kwargs: Any) -> None:
        """Log a WARNING message with the Java-style warn method name."""
        self.warning(*args, **kwargs)

    def error(self, *args: Any, **kwargs: Any) -> None:
        """Log an ERROR message with explicit or contextual headers."""
        self._log(logging.ERROR, *args, **kwargs)

    def exception(self, *args: Any, **kwargs: Any) -> None:
        """Log an ERROR message with exception details and request headers."""
        kwargs["exc_info"] = True
        self._log(logging.ERROR, *args, **kwargs)

    def _log(self, level: int, *args: Any, **kwargs: Any) -> None:
        headers, message, message_args = self._parse_args(args)
        self._delegate.log(
            level,
            self._format(headers, message),
            *message_args,
            **kwargs,
        )

    def _parse_args(
        self,
        args: tuple[Any, ...],
    ) -> tuple[ExperimentOpsHeaders | None, str, tuple[Any, ...]]:
        if not args:
            raise TypeError("Logger message is required")

        first = args[0]
        if isinstance(first, ExperimentOpsHeaders):
            if len(args) < 2:
                raise TypeError("Logger message is required after headers")

            return first, str(args[1]), args[2:]

        return get_current_headers(), str(first), args[1:]

    @staticmethod
    def _format(headers: ExperimentOpsHeaders | None, message: str) -> str:
        if headers is None:
            return message

        parts = [
            f"| requestUuid : {headers.request_uuid or NA}",
            f"| userUuid : {headers.user_uuid or NA}",
            f"| workspaceUuid : {headers.workspace_uuid or NA}",
        ]

        return f"{' '.join(parts)} | {message}"
