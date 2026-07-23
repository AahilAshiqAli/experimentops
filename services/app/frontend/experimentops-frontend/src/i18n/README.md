# ExperimentOps localization

English is the source locale. German is generated at build time and must not be
edited by hand.

- Edit `locales/en.json` when adding or changing product copy.
- Use `t('path.to.key')` from `react-i18next` in React code.
- Run `pnpm run i18n:generate` to refresh `locales/de.json`.
- Run `pnpm run i18n:validate` to check key and interpolation parity.
- `pnpm run build` runs generation and validation before TypeScript and Vite.

Generation uses the local Ollama service by default, so source copy stays on the
developer machine. It defaults to `qwen3.5:latest`; override it with
`OLLAMA_TRANSLATION_MODEL` or the service URL with `OLLAMA_URL`.
`scripts/translation-cache/de.json` makes subsequent builds deterministic and
means CI does not need Ollama when the checked-in cache is current.

MyMemory is available as an explicit, opt-in free API fallback:
`TRANSLATION_PROVIDER=mymemory pnpm run i18n:generate`. This sends only new or
changed English messages to MyMemory. `MYMEMORY_EMAIL` can identify
higher-volume requests as recommended by that service.

The browser never calls a translation service. Both locale catalogs are bundled
into the compiled application.
