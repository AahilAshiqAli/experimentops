# Codex project rules

These rules guide future Codex work in this frontend project.

## Container architecture

- Keep data-fetching and mutation orchestration in container hooks.
- For a feature folder, create/use a hook named `use{FolderName}Container.ts`.
  - Example: `src/container/Projects/useProjectsContainer.ts`
  - Example: `src/container/ExperimentResources/useCreateExperimentRunContainer.ts`
- UI components should receive prepared state, handlers, and view models from the container hook when practical.
- Avoid placing `useQuery`, `useMutation`, or service orchestration directly inside deeply nested presentational components.
- Container hooks should communicate with query hooks/services and expose a clear contract to the component layer.

## Query and service boundaries

- Query hooks should stay close to service contracts and avoid inventing extra response shapes.
- Services should model backend API contracts precisely.
- Do not create random or guessed request/response contracts.
- If an API contract is unclear, incomplete, or inconsistent, stop and ask for clarification before implementing behavior that depends on it.
- Prefer explicit types for request payloads, query params, and responses.

## Contract precision

- Be precise about required fields, optional fields, enum-like values, and payload shape.
- Do not silently assume backend behavior for important fields.
- When adding query params or payload fields, confirm they match the backend contract or clearly mark the assumption.
- If a feature depends on ordering, status filtering, IDs, permissions, or lifecycle state, verify the contract before wiring UI behavior.

## Component structure

- Keep large feature components split into focused files.
- Always place custom React hooks in a `hooks` folder; do not define them alongside components or container files.
- Prefer separate files for:
  - page components
  - dialogs
  - board/canvas widgets
  - table/list panels
  - container hooks
  - shared feature types
- Avoid letting a route file become a catch-all for all UI and logic.

## Implementation style

- Preserve existing user changes and avoid broad rewrites unless requested.
- Keep UI behavior explicit and easy to trace.
- Use the shared `Toaster` for user-facing errors. Do not render error messages or error panels inline on a screen unless the user explicitly requests an inline error state.
- Run relevant validation after changes, typically:
  - `pnpm run build`
  - `pnpm run lint`
  - `git diff --check`
