from __future__ import annotations

from typing import Any

from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroDeserializer
from confluent_kafka.serialization import MessageField, SerializationContext, StringDeserializer

from experiment_runtime.config import KafkaSettings


class AvroDeserializationError(RuntimeError):
    pass


class ExperimentOpsAvroDeserializer:
    def __init__(self, settings: KafkaSettings):
        self._schema_registry_client = SchemaRegistryClient(
            settings.schema_registry_config()
        )
        # This object knows how to read Confluent Avro messages and fetch schemas using Schema Registry
        self._value_deserializer = AvroDeserializer(self._schema_registry_client)
        self._string_key_deserializer = StringDeserializer("utf_8")

    def deserialize_value(self, topic: str, value: bytes | None) -> Any:
        if value is None:
            return None

        try:
            # It takes the raw Kafka bytes, uses the schema id inside the message, asks Schema Registry for the schema, and converts the Avro bytes into a Python object
            return self._value_deserializer(
                value,
                SerializationContext(topic, MessageField.VALUE),
            )
        except Exception as exception:
            raise AvroDeserializationError(
                f"Failed to deserialize Avro value from topic '{topic}'"
            ) from exception

    def deserialize_string_key(self, topic: str, key: bytes | None) -> str | None:
        if key is None:
            return None

        try:
            return self._string_key_deserializer(
                key,
                SerializationContext(topic, MessageField.KEY),
            )
        except Exception as exception:
            raise AvroDeserializationError(
                f"Failed to deserialize Kafka key from topic '{topic}'"
            ) from exception