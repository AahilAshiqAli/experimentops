import { readFile, readdir } from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url))
const projectDirectory = path.resolve(scriptDirectory, '..')
const sourceFile = path.join(projectDirectory, 'src/i18n/locales/en.json')
const targetFile = path.join(projectDirectory, 'src/i18n/locales/de.json')
const sourceDirectory = path.join(projectDirectory, 'src')

function flatten(value, prefix = '', output = {}) {
  for (const [key, child] of Object.entries(value)) {
    const nextKey = prefix ? `${prefix}.${key}` : key
    if (typeof child === 'string') output[nextKey] = child
    else if (child && typeof child === 'object' && !Array.isArray(child)) {
      flatten(child, nextKey, output)
    } else throw new Error(`Invalid translation value at ${nextKey}.`)
  }
  return output
}

function placeholders(value) {
  return [...value.matchAll(/{{\s*([\w.]+)\s*}}/g)]
    .map((match) => match[1])
    .sort()
}

async function listSourceFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true })
  const files = await Promise.all(
    entries.map(async (entry) => {
      const entryPath = path.join(directory, entry.name)
      if (entry.isDirectory()) return listSourceFiles(entryPath)
      return /\.(ts|tsx)$/.test(entry.name) ? [entryPath] : []
    }),
  )
  return files.flat()
}

const english = flatten(JSON.parse(await readFile(sourceFile, 'utf8')))
const german = flatten(JSON.parse(await readFile(targetFile, 'utf8')))
const errors = []

for (const [key, source] of Object.entries(english)) {
  const target = german[key]
  if (typeof target !== 'string' || !target.trim()) {
    errors.push(`German translation is missing for ${key}.`)
    continue
  }
  if (
    JSON.stringify(placeholders(source)) !==
    JSON.stringify(placeholders(target))
  ) {
    errors.push(`Interpolation placeholders do not match for ${key}.`)
  }
}

for (const key of Object.keys(german)) {
  if (!(key in english))
    errors.push(`German translation has unknown key ${key}.`)
}

const sourceFiles = await listSourceFiles(sourceDirectory)
for (const file of sourceFiles) {
  const source = await readFile(file, 'utf8')
  const translationCalls = source.matchAll(
    /\b(?:t|i18n\.t)\(\s*['"]([\w.-]+)['"]/g,
  )
  for (const match of translationCalls) {
    const key = match[1]
    const isPluralKey = `${key}_one` in english && `${key}_other` in english
    if (!(key in english) && !isPluralKey) {
      errors.push(
        `${path.relative(projectDirectory, file)} uses unknown translation key ${key}.`,
      )
    }
  }
}

if (errors.length) {
  process.stderr.write(`${errors.join('\n')}\n`)
  process.exitCode = 1
} else {
  process.stdout.write(
    `Validated ${Object.keys(english).length} English and German translations.\n`,
  )
}
