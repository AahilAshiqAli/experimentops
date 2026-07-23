import { mkdir, readFile, writeFile } from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'


// I have used Ollama for generating the translations using qwen3.5:latest model. You can also use MyMemory translation API by setting the TRANSLATION_PROVIDER environment variable to "mymemory" and optionally providing an email address via MYMEMORY_EMAIL for higher request limits.

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url))
const projectDirectory = path.resolve(scriptDirectory, '..')
const sourceFile = path.join(projectDirectory, 'src/i18n/locales/en.json')
const targetFile = path.join(projectDirectory, 'src/i18n/locales/de.json')
const cacheDirectory = path.join(scriptDirectory, 'translation-cache')
const cacheFile = path.join(cacheDirectory, 'de.json')
const API_URL = 'https://api.mymemory.translated.net/get'
const LANGUAGE_PAIR = 'en|de'
const MAX_REQUEST_BYTES = 500
const CONCURRENCY = 4
const CACHE_VERSION = 2
const OLLAMA_BATCH_SIZE = 30
const OLLAMA_URL = process.env.OLLAMA_URL ?? 'http://127.0.0.1:11434'
const OLLAMA_MODEL = process.env.OLLAMA_TRANSLATION_MODEL ?? 'qwen3.5:latest'
const TRANSLATION_PROVIDER = process.env.TRANSLATION_PROVIDER ?? 'ollama'

function logDiagnostic(label, value) {
  process.stdout.write(`[i18n] ${label}\n${JSON.stringify(value, null, 2)}\n`)
}

async function readJson(file, fallback = {}) {
  try {
    return JSON.parse(await readFile(file, 'utf8'))
  } catch (error) {
    if (error?.code === 'ENOENT') return fallback
    throw error
  }
}

function flatten(value, prefix = '', output = {}) {
  for (const [key, child] of Object.entries(value)) {
    const nextKey = prefix ? `${prefix}.${key}` : key
    if (typeof child === 'string') {
      output[nextKey] = child
    } else if (child && typeof child === 'object' && !Array.isArray(child)) {
      flatten(child, nextKey, output)
    } else {
      throw new Error(
        `Translation key "${nextKey}" must contain a string or object.`,
      )
    }
  }
  return output
}

function unflatten(value) {
  const output = {}
  for (const [key, translation] of Object.entries(value)) {
    const parts = key.split('.')
    let cursor = output
    for (const part of parts.slice(0, -1)) {
      cursor[part] ??= {}
      cursor = cursor[part]
    }
    cursor[parts.at(-1)] = translation
  }
  return output
}

function protectPlaceholders(text) {
  const placeholders = []
  const protectedText = text.replace(/{{\s*[\w.]+\s*}}/g, (placeholder) => {
    const token = `ZXQVAR${placeholders.length}QXZ`
    placeholders.push({ placeholder, token })
    return token
  })
  return { placeholders, protectedText }
}

function restorePlaceholders(text, placeholders) {
  return placeholders.reduce(
    (translation, { placeholder, token }) =>
      translation.replaceAll(token, placeholder),
    text,
  )
}

function decodeHtmlEntities(value) {
  return value
    .replaceAll('&quot;', '"')
    .replaceAll('&#39;', "'")
    .replaceAll('&amp;', '&')
    .replaceAll('&lt;', '<')
    .replaceAll('&gt;', '>')
}

async function translateTextWithMyMemory(sourceText) {
  const { placeholders, protectedText } = protectPlaceholders(sourceText)
  if (Buffer.byteLength(protectedText, 'utf8') > MAX_REQUEST_BYTES) {
    throw new Error(
      `Source message exceeds MyMemory's ${MAX_REQUEST_BYTES}-byte limit: ${sourceText}`,
    )
  }

  const url = new URL(API_URL)
  url.searchParams.set('q', protectedText)
  url.searchParams.set('langpair', LANGUAGE_PAIR)
  url.searchParams.set('mt', '1')
  if (process.env.MYMEMORY_EMAIL) {
    url.searchParams.set('de', process.env.MYMEMORY_EMAIL)
  }

  let lastError
  for (let attempt = 1; attempt <= 3; attempt += 1) {
    try {
      const loggedUrl = new URL(url)
      if (loggedUrl.searchParams.has('de')) {
        loggedUrl.searchParams.set('de', '[redacted]')
      }
      logDiagnostic(`MyMemory request (attempt ${attempt}/3)`, {
        method: 'GET',
        url: loggedUrl.toString(),
      })
      const response = await fetch(url, {
        headers: { 'User-Agent': 'ExperimentOps build-time localization' },
      })
      const payload = await response.json()
      logDiagnostic(`MyMemory response (attempt ${attempt}/3)`, {
        body: payload,
        status: response.status,
        statusText: response.statusText,
      })
      if (!response.ok || Number(payload.responseStatus) !== 200) {
        throw new Error(
          payload.responseDetails ||
            `Translation API returned HTTP ${response.status}.`,
        )
      }

      const translatedText = payload.responseData?.translatedText
      if (typeof translatedText !== 'string' || !translatedText.trim()) {
        throw new Error('Translation API returned an empty translation.')
      }

      const restored = restorePlaceholders(
        decodeHtmlEntities(translatedText.trim()),
        placeholders,
      )
      for (const { placeholder } of placeholders) {
        if (!restored.includes(placeholder)) {
          throw new Error(`Translation API changed placeholder ${placeholder}.`)
        }
      }
      return restored
    } catch (error) {
      lastError = error
      if (attempt < 3) {
        await new Promise((resolve) => setTimeout(resolve, attempt * 500))
      }
    }
  }
  throw lastError
}

function parseJsonResponse(content) {
  const trimmed = content.trim()
  const withoutFence = trimmed
    .replace(/^```(?:json)?\s*/i, '')
    .replace(/\s*```$/, '')
  return JSON.parse(withoutFence)
}

async function translateBatchWithOllama(entries) {
  const protectedEntries = entries.map(([key, sourceText]) => {
    const placeholders = [...sourceText.matchAll(/{{\s*[\w.]+\s*}}/g)].map(
      (match) => match[0],
    )
    return { key, placeholders, protectedText: sourceText }
  })
  const source = Object.fromEntries(
    protectedEntries.map(({ key, protectedText }) => [key, protectedText]),
  )
  const requestUrl = `${OLLAMA_URL}/api/chat`
  const requestBody = {
    format: 'json',
    messages: [
      {
        role: 'system',
        content:
          'Translate JSON object values from English to concise, professional German UI copy using formal "Sie". Keep every key exactly unchanged. Keep interpolation placeholders such as {{name}} exactly unchanged, including braces and the placeholder name. Return only one JSON object with the same keys and string values. Use this terminology consistently: experiment = Experiment, experiment run = Experimentlauf, run (execution) = Lauf, dataset = Datensatz, dataset version = Datensatzversion, config = Konfiguration, board = Board, pipeline = Pipeline, log = Protokoll, workspace = Arbeitsbereich, artifact = Artefakt. Do not translate product names, file formats, identifiers, or code. Do not add information or paraphrase technical meaning.',
      },
      { role: 'user', content: JSON.stringify(source) },
    ],
    model: OLLAMA_MODEL,
    options: { temperature: 0 },
    stream: false,
    think: false,
  }
  logDiagnostic('Ollama request', {
    body: requestBody,
    method: 'POST',
    url: requestUrl,
  })
  const response = await fetch(requestUrl, {
    body: JSON.stringify(requestBody),
    headers: { 'Content-Type': 'application/json' },
    method: 'POST',
  })

  const responseText = await response.text()
  let payload
  try {
    payload = JSON.parse(responseText)
  } catch {
    logDiagnostic('Ollama response', {
      body: responseText,
      status: response.status,
      statusText: response.statusText,
    })
    if (!response.ok) {
      throw new Error(`Ollama returned HTTP ${response.status}.`)
    }
    throw new Error('Ollama returned a non-JSON translation response.')
  }
  logDiagnostic('Ollama response', {
    body: payload,
    status: response.status,
    statusText: response.statusText,
  })
  if (!response.ok) {
    throw new Error(`Ollama returned HTTP ${response.status}.`)
  }
  const content = payload.message?.content
  if (typeof content !== 'string') {
    throw new Error('Ollama returned an invalid translation response.')
  }
  const translated = parseJsonResponse(content)
  const expectedKeys = protectedEntries.map(({ key }) => key).sort()
  const actualKeys = Object.keys(translated).sort()
  if (JSON.stringify(expectedKeys) !== JSON.stringify(actualKeys)) {
    throw new Error('Ollama changed translation keys in its response.')
  }

  return protectedEntries.map(({ key, placeholders }) => {
    const translatedText = translated[key]
    if (typeof translatedText !== 'string' || !translatedText.trim()) {
      throw new Error(`Ollama returned an empty translation for ${key}.`)
    }
    const restored = translatedText.trim()
    for (const placeholder of placeholders) {
      if (!restored.includes(placeholder)) {
        throw new Error(`Ollama changed placeholder ${placeholder} for ${key}.`)
      }
    }
    return [key, restored]
  })
}

async function translateOllamaEntries(entries) {
  try {
    return await translateBatchWithOllama(entries)
  } catch (error) {
    if (entries.length === 1) throw error
    const middle = Math.ceil(entries.length / 2)
    const left = await translateOllamaEntries(entries.slice(0, middle))
    const right = await translateOllamaEntries(entries.slice(middle))
    return [...left, ...right]
  }
}

async function mapWithConcurrency(items, worker) {
  const results = new Array(items.length)
  let nextIndex = 0

  async function run() {
    while (nextIndex < items.length) {
      const index = nextIndex
      nextIndex += 1
      results[index] = await worker(items[index], index)
    }
  }

  await Promise.all(
    Array.from({ length: Math.min(CONCURRENCY, items.length) }, () => run()),
  )
  return results
}

const source = flatten(await readJson(sourceFile))
const cache = await readJson(cacheFile)
const translationsBySource = new Map(
  Object.values(cache)
    .filter(
      (entry) =>
        entry &&
        entry.generatorVersion === CACHE_VERSION &&
        typeof entry.source === 'string' &&
        typeof entry.translation === 'string',
    )
    .map((entry) => [entry.source, entry.translation]),
)

if (!['mymemory', 'ollama'].includes(TRANSLATION_PROVIDER)) {
  throw new Error(
    `Unsupported TRANSLATION_PROVIDER "${TRANSLATION_PROVIDER}". Use "ollama" or "mymemory".`,
  )
}

logDiagnostic('Translation provider', {
  ...(TRANSLATION_PROVIDER === 'ollama'
    ? { model: OLLAMA_MODEL, url: OLLAMA_URL }
    : { url: API_URL }),
  provider: TRANSLATION_PROVIDER,
})

const entries = Object.entries(source)
const translatedByKey = new Map()
const pendingEntries = []
for (const [key, sourceText] of entries) {
  const cached = cache[key]
  if (
    cached?.generatorVersion === CACHE_VERSION &&
    cached.source === sourceText &&
    typeof cached.translation === 'string'
  ) {
    translatedByKey.set(key, cached.translation)
    continue
  }
  const reusedTranslation = translationsBySource.get(sourceText)
  if (reusedTranslation) {
    cache[key] = {
      generatorVersion: CACHE_VERSION,
      source: sourceText,
      translation: reusedTranslation,
    }
    translatedByKey.set(key, reusedTranslation)
    continue
  }
  pendingEntries.push([key, sourceText])
}

process.stdout.write(
  `[i18n] ${pendingEntries.length} of ${entries.length} translation(s) require a provider request.\n`,
)

let generatedCount = 0
async function storeTranslations(translatedEntries) {
  for (const [key, translation] of translatedEntries) {
    const sourceText = source[key]
    cache[key] = {
      generatorVersion: CACHE_VERSION,
      source: sourceText,
      translation,
    }
    translationsBySource.set(sourceText, translation)
    translatedByKey.set(key, translation)
    generatedCount += 1
    process.stdout.write(`Translated ${key}\n`)
  }
  await mkdir(cacheDirectory, { recursive: true })
  await writeFile(cacheFile, `${JSON.stringify(cache, null, 2)}\n`)
}

if (TRANSLATION_PROVIDER === 'ollama') {
  for (
    let index = 0;
    index < pendingEntries.length;
    index += OLLAMA_BATCH_SIZE
  ) {
    const batch = pendingEntries.slice(index, index + OLLAMA_BATCH_SIZE)
    await storeTranslations(await translateOllamaEntries(batch))
  }
} else {
  const translatedEntries = await mapWithConcurrency(
    pendingEntries,
    async ([key, sourceText]) => [
      key,
      await translateTextWithMyMemory(sourceText),
    ],
  )
  await storeTranslations(translatedEntries)
}

const translatedEntries = entries.map(([key]) => [
  key,
  translatedByKey.get(key),
])

const activeKeys = new Set(entries.map(([key]) => key))
for (const key of Object.keys(cache)) {
  if (!activeKeys.has(key)) delete cache[key]
}

await mkdir(cacheDirectory, { recursive: true })
await writeFile(
  targetFile,
  `${JSON.stringify(unflatten(Object.fromEntries(translatedEntries)), null, 2)}\n`,
)
await writeFile(cacheFile, `${JSON.stringify(cache, null, 2)}\n`)

process.stdout.write(
  generatedCount
    ? `Generated ${generatedCount} German translation(s).\n`
    : 'German translations are already current.\n',
)
