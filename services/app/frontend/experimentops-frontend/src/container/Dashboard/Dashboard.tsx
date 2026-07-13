import { useDocumentTitle } from '../../hooks'

export function Dashboard() {
  useDocumentTitle('Dashboard')

  return (
    <section>
      <h1 className="font-heading text-2xl font-semibold text-secondary">
        Dashboard
      </h1>
      <p className="mt-2 max-w-2xl text-sm text-slate-600">
        This protected screen is ready for authenticated ExperimentOps features.
      </p>
    </section>
  )
}
