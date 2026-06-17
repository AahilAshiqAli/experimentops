from __future__ import annotations

from collections.abc import Callable, Mapping
from typing import Any

from confluent_kafka import KafkaError, Message, Producer

from experiment_runtime.config import KafkaSettings
from experiment_runtime.logging.context import ExperimentOpsLogger
from experiment_runtime.schema import ExperimentOpsAvroSerializer, load_avro_schema

KafkaHeaders = dict[str, str | bytes | None] | list[tuple[str, str | bytes | None]]


class KafkaProduceError(RuntimeError):
    pass

# The schema needs to be supplied either through string or file path
class ExperimentOpsKafkaProducer:
    def __init__(
        self,
        settings: KafkaSettings,
        logger: ExperimentOpsLogger,
        value_schema_path: str | None = None,
        value_schema_str: str | None = None,
        import_paths: tuple[str, ...] = (),
        auto_register_schemas: bool = True,
    ):
        self._settings = settings
        self._logger = logger
        self._producer = Producer(settings.producer_config())
        if value_schema_str is None:
            if value_schema_path is None:
                raise ValueError("value_schema_path or value_schema_str is required")

            value_schema_str = load_avro_schema(value_schema_path, import_paths)

        self._serializer = ExperimentOpsAvroSerializer(
            settings,
            value_schema_str,
            auto_register_schemas=auto_register_schemas,
        )

    def produce(
        self,
        topic: str,
        value: Mapping[str, Any],
        key: str | None = None,
        headers: KafkaHeaders | None = None,
        callback: Callable[[KafkaError | None, Message], None] | None = None,
    ) -> None:
        """ Sync producer sending msg to queue of confluent async producer and then polling for response"""

        serialized_key = self._serializer.serialize_string_key(topic, key)
        serialized_value = self._serializer.serialize_value(topic, dict(value))

        self._producer.produce(
            topic,
            key=serialized_key,
            value=serialized_value,
            headers=headers,
            callback=callback or self._delivery_callback,
        )
        self._producer.poll(0)

    def produce_sync(
        self,
        topic: str,
        value: Mapping[str, Any],
        key: str | None = None,
        headers: KafkaHeaders | None = None,
        timeout_seconds: float | None = None,
    ) -> None:
        """ Waits for response from produce async method. If it has errors, then adds in delivery errors, else calls delivery callback for success logging"""
        delivery_errors: list[KafkaError] = []

        def delivery_callback(error: KafkaError | None, message: Message) -> None:
            if error is not None:
                delivery_errors.append(error)
                return

            self._delivery_callback(error, message)

        self.produce(
            topic=topic,
            value=value,
            key=key,
            headers=headers,
            callback=delivery_callback,
        )

        remaining_messages = self.flush(timeout_seconds)

        if remaining_messages:
            raise KafkaProduceError(
                f"Kafka producer flush timed out with {remaining_messages} undelivered message(s)"
            )

        if delivery_errors:
            self._logger.error(
                "Failed to produce Kafka message. topic=%s key=%s event_type=%s error=%s",
                topic,
                key,
                _event_type_from_value(value),
                delivery_errors[0],
            )
            raise KafkaProduceError(
                f"Failed to deliver Kafka message to topic '{topic}': {delivery_errors[0]}"
            )

        self._logger.info(
            "Produced Kafka message. topic=%s key=%s event_type=%s",
            topic,
            key,
            _event_type_from_value(value),
        )

    def flush(self, timeout_seconds: float | None = None) -> int:
        if timeout_seconds is None:
            return self._producer.flush()

        return self._producer.flush(timeout_seconds)

    def close(self) -> None:
        remaining_messages = self.flush()

        if remaining_messages:
            self._logger.warning(
                "Kafka producer closed with undelivered messages. remaining_messages=%s",
                remaining_messages,
            )

    def _delivery_callback(
        self,
        error: KafkaError | None,
        message: Message,
    ) -> None:
        """ Callback function for Kafka producer """
        if error is not None:
            self._logger.error(
                "Failed to deliver Kafka message. topic=%s error=%s",
                message.topic(),
                error,
            )



def _event_type_from_value(value: Mapping[str, Any]) -> str | None:
    """ Return event_type from Event """

    metadata = value.get("metadata")

    if not isinstance(metadata, Mapping):
        return None

    event_type = metadata.get("eventType")

    if event_type is None:
        return None

    return str(event_type)
