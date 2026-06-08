from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Mapping

# dataclass means it adds all getters setters and constructors.
@dataclass(frozen=True)
class KafkaMessage:
    topic: str
    partition: int
    offset: int
    key: str | None
    value: Mapping[str, Any] | None # Has the decoded Avro value
    headers: Mapping[str, str | bytes | None]
    timestamp_millis: int | None