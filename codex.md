# Codex Instructions

- For schema migrations, always first run an existence query. Only run the schema change when the existence query returns `0`.
- Use `StringUtils.isBlank` from Apache Commons Lang when checking whether a string is null, empty, or whitespace.
- Use `JSONUtil` for JSON serialization and deserialization instead of a generic `ObjectMapper`.
- Keep transformer code in transformer components. Services should delegate object/entity/model conversion to the appropriate transformer.
