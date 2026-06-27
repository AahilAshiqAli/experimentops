import { useState, type FormEvent } from 'react'

import { useDocumentTitle } from '../../hooks'
import { useLoginContainer } from './useLoginContainer'

export function Login() {
  useDocumentTitle('Login')

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const { handleSubmit, isSubmitting } = useLoginContainer()

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await handleSubmit({ username, password })
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-8 text-secondary">
      <section className="w-full max-w-md rounded-2xl border border-slate-200 bg-surface p-8 shadow-card">
        <p className="text-sm font-semibold uppercase tracking-[0.3em] text-secondary">
          ExperimentOps
        </p>
        <h1 className="mt-4 font-heading text-3xl font-semibold text-secondary">Welcome back</h1>
        <p className="mt-3 text-slate-600">
          Sign in to access the ExperimentOps dashboard.
        </p>
        <form className="mt-8 space-y-5" onSubmit={onSubmit}>
          <div>
            <label
              className="mb-2 block text-sm font-medium text-slate-700"
              htmlFor="username"
            >
              Email or username
            </label>
            <input
              autoComplete="username"
              className="w-full rounded-md border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/30"
              id="username"
              onChange={(event) => setUsername(event.target.value)}
              placeholder="you@example.com"
              required
              type="text"
              value={username}
            />
          </div>

          <div>
            <label
              className="mb-2 block text-sm font-medium text-slate-700"
              htmlFor="password"
            >
              Password
            </label>
            <input
              autoComplete="current-password"
              className="w-full rounded-md border border-slate-300 bg-white px-4 py-3 text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/30"
              id="password"
              minLength={1}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Enter your password"
              required
              type="password"
              value={password}
            />
          </div>

          <button
            className="inline-flex w-full items-center justify-center rounded-md bg-primary px-5 py-3 font-medium text-white transition hover:bg-secondary disabled:cursor-not-allowed disabled:opacity-60"
            disabled={isSubmitting}
            type="submit"
          >
            {isSubmitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
      </section>
    </main>
  )
}
