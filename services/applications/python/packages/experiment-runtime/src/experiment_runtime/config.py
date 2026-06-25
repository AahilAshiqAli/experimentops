from __future__ import annotations

import os
from collections.abc import Callable
from pathlib import Path

from experiment_runtime.base_model import ExperimentOpsModel


def _get_optional_env(name: str, default: str | None = None) -> str | None:
    value = os.getenv(name, default)

    if value is None or value.strip() == "":
        return default

    return value


def _get_required_env(name: str, default: str | None = None) -> str:
    value = os.getenv(name, default)

    if value is None or value.strip() == "":
        raise RuntimeError(f"Missing required environment variable: {name}")

    return value


def _get_required_env_with_default_factory(
    name: str,
    default_factory: Callable[[], str],
) -> str:
    value = os.getenv(name)

    if value is not None and value.strip() != "":
        return value

    default = default_factory()

    if default.strip() == "":
        raise RuntimeError(f"Missing required environment variable: {name}")

    return default


def _get_csv_env_with_default_factory(
    name: str,
    default_factory: Callable[[], str],
) -> tuple[str, ...]:
    value = os.getenv(name)

    if value is None or value.strip() == "":
        value = default_factory()

    return tuple(item.strip() for item in value.split(",") if item.strip())


def _get_int_env(name: str, default: int) -> int:
    value = os.getenv(name)

    if value is None or value.strip() == "":
        return default

    return int(value)


def _get_float_env(name: str, default: float) -> float:
    value = os.getenv(name)

    if value is None or value.strip() == "":
        return default

    return float(value)


def _get_bool_env(name: str, default: bool) -> bool:
    value = os.getenv(name)

    if value is None or value.strip() == "":
        return default

    return value.strip().lower() in {"true", "1", "yes", "y"}


def _get_csv_env(name: str, default: str) -> tuple[str, ...]:
    value = os.getenv(name, default)

    return tuple(item.strip() for item in value.split(",") if item.strip())


# Avro Schema Parent path joined in other schema to get full url
def _runtime_avro_schema_dir() -> Path:
    return Path(__file__).resolve().parent / "schema" / "avro"


def _default_producer_value_schema_path() -> str:
    return str(_runtime_avro_schema_dir() / "experiment-run-completed-event.avsc")


def _default_failure_producer_value_schema_path() -> str:
    return str(_runtime_avro_schema_dir() / "experiment-run-failure-event.avsc")


def _default_avro_import_paths() -> str:
    return str(_runtime_avro_schema_dir() / "event-metadata.avsc")

class KafkaSettings(ExperimentOpsModel):
    bootstrap_servers: str
    schema_registry_url: str
    topics: tuple[str, ...]
    group_id: str
    client_id: str
    auto_offset_reset: str # tells Kafka where to start reading msgs if this group has no saved offset
    enable_auto_commit: bool # would be false since we only want to commit that msg is completed when experiment is completed.
    max_poll_interval_ms: int # Max time kafka allows the worker to complete the msg before thinking it is stuck
    session_timeout_ms: int # is how long Kafka waits before deciding the consumer is dead if it stops sending heartbeats
    poll_timeout_seconds: float # controls how long the worker waits each time it asks Kafka for a new message
    producer_topic: str
    producer_value_schema_path: str
    failure_producer_topic: str
    failure_producer_value_schema_path: str
    avro_import_paths: tuple[str, ...]
    schema_registry_basic_auth_user_info: str | None = None
    kafka_security_protocol: str | None = None
    kafka_sasl_mechanism: str | None = None
    kafka_sasl_username: str | None = None
    kafka_sasl_password: str | None = None

    # A method in which cls (Class is send as reference). Used here to make alternative constructor. it is doing like KafkaSettings(injecting all data to store in class attributes)
    @classmethod
    def from_env(cls) -> "KafkaSettings":
        return cls(
            bootstrap_servers=_get_required_env(
                "EXPERIMENTOPS_KAFKA_BOOTSTRAP_SERVERS",
                "localhost:9092",
            ),
            schema_registry_url=_get_required_env(
                "EXPERIMENTOPS_SCHEMA_REGISTRY_URL",
                "http://localhost:8081",
            ),
            topics=_get_csv_env(
                "EXPERIMENTOPS_KAFKA_TOPICS",
                "experiment-run-requested-topic",
            ),
            group_id=_get_required_env(
                "EXPERIMENTOPS_KAFKA_CONSUMER_GROUP_ID",
                "experimentops-analysis-worker",
            ),
            client_id=_get_required_env(
                "EXPERIMENTOPS_KAFKA_CLIENT_ID",
                "experimentops-analysis-worker",
            ),
            auto_offset_reset=_get_required_env(
                "EXPERIMENTOPS_KAFKA_AUTO_OFFSET_RESET",
                "earliest",
            ),
            enable_auto_commit=_get_bool_env(
                "EXPERIMENTOPS_KAFKA_ENABLE_AUTO_COMMIT",
                False,
            ),
            max_poll_interval_ms=_get_int_env(
                "EXPERIMENTOPS_KAFKA_MAX_POLL_INTERVAL_MS",
                900_000,
            ),
            session_timeout_ms=_get_int_env(
                "EXPERIMENTOPS_KAFKA_SESSION_TIMEOUT_MS",
                45_000,
            ),
            poll_timeout_seconds=_get_float_env(
                "EXPERIMENTOPS_KAFKA_POLL_TIMEOUT_SECONDS",
                1.0,
            ),
            # List all producer topics here
            producer_topic=_get_required_env(
                "EXPERIMENTOPS_KAFKA_PRODUCER_TOPIC",
                "experiment-run-completed-topic",
            ),
            producer_value_schema_path=_get_required_env_with_default_factory(
                "EXPERIMENTOPS_KAFKA_PRODUCER_VALUE_SCHEMA_PATH",
                _default_producer_value_schema_path,
            ),
            failure_producer_topic=_get_required_env(
                "EXPERIMENTOPS_KAFKA_FAILURE_PRODUCER_TOPIC",
                "experiment-run-failure-topic",
            ),
            failure_producer_value_schema_path=_get_required_env_with_default_factory(
                "EXPERIMENTOPS_KAFKA_FAILURE_PRODUCER_VALUE_SCHEMA_PATH",
                _default_failure_producer_value_schema_path,
            ),
            avro_import_paths=_get_csv_env_with_default_factory(
                "EXPERIMENTOPS_AVRO_IMPORT_PATHS",
                _default_avro_import_paths,
            ),
            schema_registry_basic_auth_user_info=_get_optional_env(
                "EXPERIMENTOPS_SCHEMA_REGISTRY_BASIC_AUTH_USER_INFO"
            ),
            kafka_security_protocol=_get_optional_env(
                "EXPERIMENTOPS_KAFKA_SECURITY_PROTOCOL"
            ),
            kafka_sasl_mechanism=_get_optional_env(
                "EXPERIMENTOPS_KAFKA_SASL_MECHANISM"
            ),
            kafka_sasl_username=_get_optional_env(
                "EXPERIMENTOPS_KAFKA_SASL_USERNAME"
            ),
            kafka_sasl_password=_get_optional_env(
                "EXPERIMENTOPS_KAFKA_SASL_PASSWORD"
            ),
        )

    # These are the consumer config settings that Kafka Consumers need
    def consumer_config(self) -> dict[str, object]:
        config: dict[str, object] = {
            "bootstrap.servers": self.bootstrap_servers,
            "group.id": self.group_id,
            "client.id": self.client_id,
            "auto.offset.reset": self.auto_offset_reset,
            "enable.auto.commit": self.enable_auto_commit,
            "max.poll.interval.ms": self.max_poll_interval_ms,
            "session.timeout.ms": self.session_timeout_ms,
            "allow.auto.create.topics": True,
        }

        if self.kafka_security_protocol:
            config["security.protocol"] = self.kafka_security_protocol

        if self.kafka_sasl_mechanism:
            config["sasl.mechanism"] = self.kafka_sasl_mechanism

        if self.kafka_sasl_username:
            config["sasl.username"] = self.kafka_sasl_username

        if self.kafka_sasl_password:
            config["sasl.password"] = self.kafka_sasl_password

        return config

    def producer_config(self) -> dict[str, object]:
        config: dict[str, object] = {
            "bootstrap.servers": self.bootstrap_servers,
            "client.id": f"{self.client_id}-producer",
            "allow.auto.create.topics": True,
        }

        if self.kafka_security_protocol:
            config["security.protocol"] = self.kafka_security_protocol

        if self.kafka_sasl_mechanism:
            config["sasl.mechanism"] = self.kafka_sasl_mechanism

        if self.kafka_sasl_username:
            config["sasl.username"] = self.kafka_sasl_username

        if self.kafka_sasl_password:
            config["sasl.password"] = self.kafka_sasl_password

        return config


    # These are the schema registry config settings that Kafka SchemaRegistryClient need
    def schema_registry_config(self) -> dict[str, str]:
        config: dict[str, str] = {
            "url": self.schema_registry_url,
        }

        if self.schema_registry_basic_auth_user_info:
            config["basic.auth.credentials.source"] = "USER_INFO"
            config["basic.auth.user.info"] = self.schema_registry_basic_auth_user_info

        return config
