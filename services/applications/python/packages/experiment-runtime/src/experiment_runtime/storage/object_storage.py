from abc import ABC, abstractmethod


class ObjectStorage(ABC):
    """Interface for experiment artifact/object storage implementations."""

    @abstractmethod
    def upload_file(self, key: str, content: bytes) -> str:
        raise NotImplementedError

    @abstractmethod
    def download_file(self, key: str) -> bytes:
        raise NotImplementedError

    @abstractmethod
    def delete_file(self, key: str) -> None:
        raise NotImplementedError
