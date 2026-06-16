from __future__ import annotations

from typing import Any, Protocol

from experiment_runtime.storage.object_storage import ObjectStorage


class S3Client(Protocol):
    """Small protocol explaining that whatever S3 Client you pass must have utility for these three functions which we are going to utilize in this class."""

    def put_object(self, **kwargs: Any) -> dict[str, Any]:
        ...

    def get_object(self, **kwargs: Any) -> dict[str, Any]:
        ...

    def delete_object(self, **kwargs: Any) -> dict[str, Any]:
        ...


class S3Storage(ObjectStorage):
    def __init__(
        self,
        bucket_name: str,
        client: S3Client,
        *,
        key_prefix: str = "",
    ) -> None:
        if not bucket_name.strip():
            raise ValueError("bucket_name is required")

        self.bucket_name = bucket_name
        self.client = client
        self.key_prefix = key_prefix.strip("/")

    # Method belongs to the class and can be overridden by subclass. cls is class reference of self
    @classmethod
    def from_bucket(
        cls,
        bucket_name: str,
        *,
        key_prefix: str = "",
        **client_kwargs: Any,
    ) -> "S3Storage":
        """Convenience factory for app wiring; tests should inject a client using the constructor."""
        import boto3

        return cls(
            bucket_name=bucket_name,
            client=boto3.client("s3", **client_kwargs),
            key_prefix=key_prefix,
        )

    def upload_file(self, key: str, content: bytes) -> str:
        object_key = self._object_key(key)

        self.client.put_object(
            Bucket=self.bucket_name,
            Key=object_key,
            Body=content,
        )

        return object_key

    def download_file(self, key: str) -> bytes:
        response = self.client.get_object(
            Bucket=self.bucket_name,
            Key=self._object_key(key),
        )

        return response["Body"].read()

    def delete_file(self, key: str) -> None:
        self.client.delete_object(
            Bucket=self.bucket_name,
            Key=self._object_key(key),
        )

    def _object_key(self, key: str) -> str:
        cleaned_key = key.strip("/")

        if not cleaned_key:
            raise ValueError("key is required")

        if not self.key_prefix:
            return cleaned_key

        return f"{self.key_prefix}/{cleaned_key}"
