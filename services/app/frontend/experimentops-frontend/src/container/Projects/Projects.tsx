import { useDocumentTitle } from '../../hooks'
import { useProjectsContainer } from './useProjectsContainer'

export function Projects() {
  useDocumentTitle('Projects')

  const {
    errorMessage,
    firstProjectNumber,
    isLoading,
    lastProjectNumber,
    page,
    paginatedProjects,
    setPage,
    totalPages,
    totalProjects,
  } = useProjectsContainer()

  return (
    <section className="rounded-2xl border border-slate-200 bg-surface shadow-card">
      <div className="flex flex-col gap-2 border-b border-slate-200 px-6 py-5 sm:px-8">
        <h1 className="font-heading text-3xl font-semibold text-secondary">Projects</h1>
        <p className="text-slate-600">Projects available in this workspace.</p>
      </div>

      <div className="p-6 sm:p-8">
        {isLoading && (
          <div className="space-y-3" role="status">
            <div className="h-14 animate-pulse rounded-md bg-slate-100" />
            <div className="h-14 animate-pulse rounded-md bg-slate-100" />
            <div className="h-14 animate-pulse rounded-md bg-slate-100" />
            <span className="sr-only">Loading projects</span>
          </div>
        )}

        {!isLoading && errorMessage && (
          <div className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700" role="alert">
            {errorMessage}
          </div>
        )}

        {!isLoading && !errorMessage && totalProjects === 0 && (
          <div className="rounded-md border border-dashed border-slate-300 px-6 py-12 text-center">
            <h2 className="font-heading text-xl font-semibold text-secondary">No projects yet</h2>
            <p className="mt-2 text-sm text-slate-600">Projects created for this workspace will appear here.</p>
          </div>
        )}

        {!isLoading && !errorMessage && totalProjects > 0 && (
          <>
            <div className="overflow-x-auto rounded-lg border border-slate-200">
              <table className="min-w-full divide-y divide-slate-200 text-left">
                <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="px-5 py-3 font-semibold" scope="col">Project</th>
                    <th className="px-5 py-3 font-semibold" scope="col">Description</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 bg-white">
                  {paginatedProjects.map((project) => (
                    <tr className="transition hover:bg-slate-50" key={project.uuid}>
                      <td className="whitespace-nowrap px-5 py-4 text-sm font-medium text-secondary">
                        {project.name}
                      </td>
                      <td className="min-w-72 px-5 py-4 text-sm text-slate-600">
                        {project.description || '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="mt-5 flex flex-col gap-3 text-sm text-slate-600 sm:flex-row sm:items-center sm:justify-between">
              <p>{`Showing ${firstProjectNumber}–${lastProjectNumber} of ${totalProjects} projects`}</p>
              <div className="flex items-center gap-2">
                <button
                  className="rounded-md border border-slate-300 bg-white px-3 py-2 font-medium text-secondary transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-50"
                  disabled={page === 1}
                  onClick={() => setPage((currentPage) => currentPage - 1)}
                  type="button"
                >
                  Previous
                </button>
                <span className="px-2 text-slate-500">{`Page ${page} of ${totalPages}`}</span>
                <button
                  className="rounded-md border border-slate-300 bg-white px-3 py-2 font-medium text-secondary transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-50"
                  disabled={page === totalPages}
                  onClick={() => setPage((currentPage) => currentPage + 1)}
                  type="button"
                >
                  Next
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </section>
  )
}
