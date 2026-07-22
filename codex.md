# Codex Instructions

- For schema migrations, always first run an existence query. Only run the schema change when the existence query returns `0`.
- For migration files, follow the guarded `information_schema` pattern used in `infra/database/migration/V1_1_9__add_step_and_port_to_run_artifact.sql`; do not add columns with an unguarded multi-column `ALTER TABLE`.
- Use jspecify annotations such as `@NonNull` and `@Nullable` in function definitions to make nullability explicit.
- Use `StringUtils.isBlank` from Apache Commons Lang when checking whether a string is null, empty, or whitespace.
- When a downstream function or repository query intentionally accepts `null`, normalize blank external input at the boundary, e.g. `StringUtils.isBlank(name) ? null : name`; do not re-check with `StringUtils.isBlank` inside that downstream function unless it has distinct behavior for blank strings.
- Use `ExperimentOpsUtils.isEmpty(...)` when checking whether a collection or map is null or empty.
- Use `JSONUtil` for JSON serialization and deserialization instead of a generic `ObjectMapper`.
- Always use transformer components for transformation code. Services should delegate object/entity/model/request/response conversion to the appropriate transformer.
- Always use Lombok `@RequiredArgsConstructor` for required-constructor generation instead of writing constructors yourself, unless the runtime/framework requires a custom constructor signature.
