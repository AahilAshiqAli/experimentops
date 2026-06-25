from __future__ import annotations

from typing import Any, Mapping

from experiment_runtime.base_model import ExperimentOpsModel


class KafkaMessage(ExperimentOpsModel):
    topic: str
    partition: int
    offset: int
    key: str | None
    value: Mapping[str, Any] | None # Has the decoded Avro value
    headers: Mapping[str, str | bytes | None]
    timestamp_millis: int | None
