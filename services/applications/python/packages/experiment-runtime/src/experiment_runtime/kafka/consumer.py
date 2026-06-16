from __future__ import annotations

import logging
from collections.abc import Callable

from confluent_kafka import Consumer, KafkaError, KafkaException, Message

from experiment_runtime.config import KafkaSettings
from experiment_runtime.kafka.message import KafkaMessage
from experiment_runtime.logging.context import (
    ExperimentOpsLogger,
    kafka_message_logging_context,
)
from experiment_runtime.schema.avro import (
    AvroDeserializationError,
    ExperimentOpsAvroDeserializer,
)

# A listener
KafkaMessageHandler = Callable[[KafkaMessage], None]


class ExperimentOpsKafkaConsumer:
    def __init__(
        self,
        settings: KafkaSettings,
        logger: ExperimentOpsLogger
    ):
        """Create the Kafka consumer, deserializer, and logger from runtime settings."""
        self._settings = settings
        self._logger = ExperimentOpsLogger.get_logger(self.__class__)
        self._consumer = Consumer(settings.consumer_config())
        self._deserializer = ExperimentOpsAvroDeserializer(settings)
        self._running = False

    # object level function so multiple consumer object instances can use this function and each can subscribe to a topic
    def run_forever(
        self,
        handler: KafkaMessageHandler,
        commit_on_handler_error: bool = False,
        commit_on_deserialization_error: bool = False,
    ) -> None:
        """Poll Kafka forever, deserialize messages, call the handler, and commit offsets."""
        self._running = True
        self._consumer.subscribe(list(self._settings.topics)) # the consumer listens to these topics

        self._logger.info(
            "ExperimentOps Kafka consumer started. topics=%s group_id=%s",
            self._settings.topics,
            self._settings.group_id,
        )

        try:
            while self._running:
                raw_message = self._consumer.poll(self._settings.poll_timeout_seconds)

                if raw_message is None:
                    continue

                if raw_message.error() is not None:
                    self._handle_kafka_error(raw_message)
                    continue

                message = self._deserialize_or_handle_error(
                    raw_message,
                    commit_on_deserialization_error,
                )
                if message is None:
                    continue

                self._handle_message(raw_message, message, handler, commit_on_handler_error)

        finally:
            self.close()

    def stop(self) -> None:
        """Ask the polling loop to stop on its next iteration."""
        self._running = False

    def close(self) -> None:
        """Stop the consumer and close the underlying Kafka connection."""
        self._running = False
        self._consumer.close()
        self._logger.info("ExperimentOps Kafka consumer stopped.")

    def _deserialize_or_handle_error(
        self,
        raw_message: Message,
        commit_on_deserialization_error: bool,
    ) -> KafkaMessage | None:
        """Deserialize a raw Kafka message or apply the configured deserialization error policy."""
        try:
            return self._to_kafka_message(raw_message)
        except (AvroDeserializationError, TypeError):
            self._logger.exception(
                "Failed to deserialize Kafka message. topic=%s partition=%s offset=%s",
                raw_message.topic(),
                raw_message.partition(),
                raw_message.offset(),
            )

            if commit_on_deserialization_error:
                self._commit(raw_message)
                return None

            raise

    def _handle_message(
        self,
        raw_message: Message,
        message: KafkaMessage,
        handler: KafkaMessageHandler,
        commit_on_handler_error: bool,
    ) -> None:
        """Run business logic for one message and commit the offset when processing succeeds."""
        with kafka_message_logging_context(message):
            try:
                handler(message)
                self._commit(raw_message)
            except Exception:  # noqa: BLE001 - handlers are user callbacks and may raise any exception.
                self._logger.exception(
                    "Kafka message handler failed. topic=%s partition=%s offset=%s",
                    raw_message.topic(),
                    raw_message.partition(),
                    raw_message.offset(),
                )

                if commit_on_handler_error:
                    self._commit(raw_message)
                    return

                raise

    def _to_kafka_message(self, raw_message: Message) -> KafkaMessage:
        """Convert a Confluent Kafka message into the project-level KafkaMessage type."""
        topic = raw_message.topic()
        if not topic:
            self._logger.error("topic not set")
            return KafkaMessage(
                topic="",
                partition=0,
                offset=0,
                key=None,
                value=None,
                headers={},
                timestamp_millis=None,
            )
        key = self._deserializer.deserialize_string_key(topic, raw_message.key())
        value = self._deserializer.deserialize_value(topic, raw_message.value())

        if value is not None and not isinstance(value, dict):
            raise TypeError(
                f"Expected Avro value to deserialize into dict, got {type(value).__name__}"
            )

        return KafkaMessage(
            topic=topic,
            partition=self._required_int(raw_message.partition(), "partition"),
            offset=self._required_int(raw_message.offset(), "offset"),
            key=key,
            value=value,
            headers=self._headers_to_mapping(raw_message.headers()),
            timestamp_millis=self._timestamp_millis(raw_message),
        )

    def _commit(self, raw_message: Message) -> None:
        """Synchronously commit the Kafka offset for a processed message."""
        self._consumer.commit(message=raw_message, asynchronous=False)

        self._logger.debug(
            "Committed Kafka offset. topic=%s partition=%s offset=%s",
            raw_message.topic(),
            raw_message.partition(),
            raw_message.offset(),
        )

    def _handle_kafka_error(self, raw_message: Message) -> None:
        """Ignore partition EOF notifications and raise real Kafka errors."""
        error = raw_message.error()

        if error is None:
            return

        if error.code() == KafkaError._PARTITION_EOF:
            self._logger.debug(
                "Reached end of Kafka partition. topic=%s partition=%s offset=%s",
                raw_message.topic(),
                raw_message.partition(),
                raw_message.offset(),
            )
            return

        if error.code() == KafkaError.UNKNOWN_TOPIC_OR_PART:
            self._logger.warning(
                "Subscribed topic is not available yet. topic=%s error=%s",
                raw_message.topic(),
                error,
            )
            return

        raise KafkaException(error)

    @staticmethod
    def _required_int(value: int | None, field_name: str) -> int:
        """Return a required Kafka integer field or fail if the client did not provide it."""
        if value is None:
            raise TypeError(f"Kafka message {field_name} is not set")

        return value

    @staticmethod
    def _headers_to_mapping(
        headers: (
            dict[str, str | bytes | None]
            | list[tuple[str, str | bytes | None]]
            | None
        ),
    ) -> dict[str, str | bytes | None]:
        """Normalize Kafka headers into a dictionary and decode UTF-8 byte values."""
        if not headers:
            return {}

        result: dict[str, str | bytes | None] = {}
        items = headers.items() if isinstance(headers, dict) else headers

        for key, value in items:
            if value is None:
                result[key] = None
                continue

            if isinstance(value, str):
                result[key] = value
                continue

            try:
                result[key] = value.decode("utf-8")
            except UnicodeDecodeError:
                result[key] = value

        return result

    @staticmethod
    def _timestamp_millis(raw_message: Message) -> int | None:
        """Return the Kafka message timestamp in milliseconds when it is available."""
        _, timestamp_millis = raw_message.timestamp()

        if timestamp_millis is None or timestamp_millis < 0:
            return None

        return timestamp_millis
