from __future__ import annotations

from typing import Any

from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroSerializer
from confluent_kafka.serialization import (
    MessageField,
    SerializationContext,
    StringSerializer,
)

from experiment_runtime.config import KafkaSettings


class AvroSerializationError(RuntimeError):
    pass


class ExperimentOpsAvroSerializer:
    """ Key is serialized as string. Key is experimentRUnUuid and Value is serialized with avroSerializer"""
    def __init__(
        self,
        settings: KafkaSettings,
        value_schema_str: str,
        auto_register_schemas: bool = True,
    ):
        self._schema_registry_client = SchemaRegistryClient(
            settings.schema_registry_config()
        )
        self._value_serializer = AvroSerializer(
            self._schema_registry_client,
            value_schema_str,
            conf={"auto.register.schemas": auto_register_schemas},
        )
        self._string_key_serializer = StringSerializer("utf_8")

    def serialize_value(self, topic: str, value: Any | None) -> bytes | None:
        if value is None:
            return None

        try:
            return self._value_serializer(
                value,
                SerializationContext(topic, MessageField.VALUE),
            )
        except Exception as exception:
            raise AvroSerializationError(
                f"Failed to serialize Avro value for topic '{topic}'"
            ) from exception

    def serialize_string_key(self, topic: str, key: str | None) -> bytes | None:
        if key is None:
            return None

        try:
            return self._string_key_serializer(
                key,
                SerializationContext(topic, MessageField.KEY),
            )
        except Exception as exception:
            raise AvroSerializationError(
                f"Failed to serialize Kafka key for topic '{topic}'"
            ) from exception
