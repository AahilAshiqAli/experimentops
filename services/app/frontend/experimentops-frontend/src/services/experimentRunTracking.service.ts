const STORAGE_KEY = 'experimentops.tracked-experiment-runs'

type TrackingListener = () => void

const listeners = new Set<TrackingListener>()

function readTrackedExperimentRuns(): string[] {
  if (typeof window === 'undefined') return []

  try {
    const value = JSON.parse(window.sessionStorage.getItem(STORAGE_KEY) ?? '[]')
    return Array.isArray(value)
      ? value.filter(
          (runUuid): runUuid is string => typeof runUuid === 'string',
        )
      : []
  } catch {
    return []
  }
}

function writeTrackedExperimentRuns(runUuids: string[]) {
  if (typeof window === 'undefined') return

  window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(runUuids))
  listeners.forEach((listener) => listener())
}

export function getTrackedExperimentRuns() {
  return readTrackedExperimentRuns()
}

export function trackExperimentRun(experimentRunUuid: string) {
  const runUuids = readTrackedExperimentRuns()
  if (!runUuids.includes(experimentRunUuid)) {
    writeTrackedExperimentRuns([...runUuids, experimentRunUuid])
  }
}

export function untrackExperimentRun(experimentRunUuid: string) {
  writeTrackedExperimentRuns(
    readTrackedExperimentRuns().filter(
      (runUuid) => runUuid !== experimentRunUuid,
    ),
  )
}

export function subscribeToTrackedExperimentRuns(listener: TrackingListener) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}
