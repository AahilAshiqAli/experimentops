from __future__ import annotations

import copy
import json
from pathlib import Path
from typing import Any

""" Loader reads a schema from json file on disk and returns a single self contained string """

def load_avro_schema(
    schema_path: str,
    import_paths: tuple[str, ...] = (),
) -> str:
    """ This function is the public entry point for loading a schema from avro file as python dict. Brings schema paths and import paths from config """
    # schema path is experiment_run_completed and import path is event-metadata(Any imports in the json)
    schema = _read_schema_file(schema_path)
    imported_schemas = {
        # Get name ExperimentOpsMetadataEvent from event-metadata
        _fully_qualified_avro_name(imported_schema): imported_schema
        # Saves all imported json with their name in imported_schemas
        for imported_schema in (_read_schema_file(path) for path in import_paths)
    }

    # Adds all imported schemas in the right place in main schema json
    return json.dumps(_expand_named_type_references(schema, imported_schemas))

""" Reads the file and returns the json schema """
def _read_schema_file(schema_path: str) -> dict[str, Any]:
    path = Path(schema_path)

    if not path.exists():
        raise FileNotFoundError(f"Avro schema file does not exist: {schema_path}")

    with path.open(encoding="utf-8") as schema_file:
        schema = json.load(schema_file)

    if not isinstance(schema, dict):
        raise TypeError(f"Avro schema file must contain a JSON object: {schema_path}")

    return schema


def _fully_qualified_avro_name(schema: dict[str, Any]) -> str:
    """ Joins name and namespaces to get key com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent"""

    name = schema.get("name")
    namespace = schema.get("namespace")

    if not isinstance(name, str) or not name:
        raise ValueError("Imported Avro schema must define a name")

    if isinstance(namespace, str) and namespace:
        return f"{namespace}.{name}"

    return name

# value is a python dict json, named schemas has event-metadata json in python dict
def _expand_named_type_references(
    value: Any,
    named_schemas: dict[str, dict[str, Any]],
) -> Any:
    """ This is a recursive function"""

    # Base case: reach value as string (no more dict) , If value is a string then try to find that value as string in
    # type = "com.experimentops.common.kafka.model.event.ExperimentOpsMetadataEvent" Exactly what is saved as key in named-schemas
    if isinstance(value, str):
        imported_schema = named_schemas.get(value)

        if imported_schema is None:
            return value

        return copy.deepcopy(imported_schema)

    if isinstance(value, list):
        return [_expand_named_type_references(item, named_schemas) for item in value]

    # If value is a dictionary , then call inside dictionary in this function 
    if isinstance(value, dict):
        return {
            key: _expand_named_type_references(item, named_schemas)
            for key, item in value.items()
        }

    return value
