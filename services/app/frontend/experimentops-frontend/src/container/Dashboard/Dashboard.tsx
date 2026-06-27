import { useDocumentTitle } from '../../hooks'

export function Dashboard() {
  useDocumentTitle('Dashboard')

  return (
    <section className="rounded-2xl border border-slate-200 bg-surface p-8 shadow-card">
      <h1 className="font-heading text-3xl font-semibold text-secondary">Dashboard</h1>
      <p className="mt-3 max-w-2xl text-slate-600">
        This protected screen is ready for authenticated ExperimentOps features.
      </p>
    </section>
  )
}
