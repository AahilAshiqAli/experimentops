from __future__ import annotations

import os

from experiment_runtime.base_model import ExperimentOpsModel
from experiment_runtime.storage import ObjectStorage, S3Storage


class ObjectStorageConfig(ExperimentOpsModel):
    bucket_name: str | None = None
    region_name: str | None = None
    key_prefix: str = ""

    @classmethod
    def from_env(cls) -> "ObjectStorageConfig":
        return cls(
            bucket_name=_get_optional_env("EXPERIMENTOPS_STORAGE_S3_BUCKET"),
            region_name=_get_optional_env("EXPERIMENTOPS_STORAGE_S3_REGION"),
            key_prefix=_get_optional_env("EXPERIMENTOPS_STORAGE_S3_KEY_PREFIX", "") or "",
        )

    def create_object_storage(self) -> ObjectStorage | None:
        if self.bucket_name is None:
            return None

        client_kwargs: dict[str, str] = {}

        if self.region_name is not None:
            client_kwargs["region_name"] = self.region_name

        return S3Storage.from_bucket(
            bucket_name=self.bucket_name,
            key_prefix=self.key_prefix,
            **client_kwargs,
        )


def _get_optional_env(name: str, default: str | None = None) -> str | None:
    value = os.getenv(name, default)

    if value is None or value.strip() == "":
        return default

    return value
