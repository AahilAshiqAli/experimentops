import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import { LanguageSelector } from '../../components/LanguageSelector'
import { useLogin } from '../../context-api/logincontext'
import { useDocumentTitle } from '../../hooks'
import { useMutationResetPassword } from '../../queries'
import { Toaster } from '../../services/toaster.service'
import {
  authenticatedRoutesConstant,
  unAuthenticatedRoutesConstant,
} from '../../routes'

function EyeIcon({ visible }: { visible: boolean }) {
  return visible ? (
    <svg aria-hidden="true" className="h-5 w-5" fill="none" viewBox="0 0 24 24">
      <path
        d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Zm10 3a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  ) : (
    <svg aria-hidden="true" className="h-5 w-5" fill="none" viewBox="0 0 24 24">
      <path
        d="m3 3 18 18M10.6 10.6A2 2 0 0 0 13.4 13.4M9.9 5.1A10.8 10.8 0 0 1 12 5c6.5 0 10 7 10 7a18.4 18.4 0 0 1-3.1 3.8M6.6 6.6C3.6 8.6 2 12 2 12s3.5 7 10 7c1 0 2-.2 2.9-.6"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  )
}

export function ResetPassword() {
  const { t } = useTranslation()
  useDocumentTitle(t('auth.resetPassword'))

  const { logout } = useLogin()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')?.trim() ?? ''
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [isPasswordVisible, setIsPasswordVisible] = useState(false)
  const [isConfirmationVisible, setIsConfirmationVisible] = useState(false)
  const resetPasswordMutation = useMutationResetPassword()

  const requirements = [
    { label: t('auth.reset.requirementLength'), met: password.length >= 8 },
    {
      label: t('auth.reset.requirementUppercase'),
      met: /[A-Z]/.test(password),
    },
    {
      label: t('auth.reset.requirementSpecial'),
      met: /[^A-Za-z0-9]/.test(password),
    },
  ]
  const isPasswordValid = requirements.every((requirement) => requirement.met)
  const passwordsMatch = password.length > 0 && password === confirmation

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!token) {
      Toaster.error(t('auth.reset.missingToken'))
      return
    }
    if (!isPasswordValid) {
      Toaster.error(t('auth.reset.invalidRequirements'))
      return
    }
    if (!passwordsMatch) {
      Toaster.error(t('auth.reset.confirmationMismatch'))
      return
    }

    resetPasswordMutation.mutate(
      { newPassword: password, token },
      {
        onSuccess: () => {
          logout()
          navigate(unAuthenticatedRoutesConstant.LOGIN, { replace: true })
        },
        onError: () => {
          Toaster.error(t('auth.reset.invalidPassword'))
        },
      },
    )
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-8 text-secondary">
      <section className="w-full max-w-lg rounded-2xl border border-slate-200 bg-surface p-8 shadow-card sm:p-10">
        <div className="mb-5 flex justify-end">
          <LanguageSelector />
        </div>
        <p className="text-sm font-semibold uppercase tracking-[0.3em] text-secondary">
          {t('app.name')}
        </p>
        <h1 className="mt-4 font-heading text-3xl font-semibold text-secondary">
          {t('auth.reset.setNewPassword')}
        </h1>
        <p className="mt-3 text-slate-600">{t('auth.chooseStrongPassword')}</p>

        <form className="mt-8 space-y-5" onSubmit={handleSubmit}>
          <PasswordField
            label={t('auth.newPassword')}
            onToggle={() => setIsPasswordVisible((visible) => !visible)}
            onChange={setPassword}
            visible={isPasswordVisible}
            value={password}
          />
          <PasswordField
            label={t('auth.confirmPassword')}
            onToggle={() => setIsConfirmationVisible((visible) => !visible)}
            onChange={setConfirmation}
            visible={isConfirmationVisible}
            value={confirmation}
          />

          <button
            className="inline-flex w-full items-center justify-center rounded-md bg-primary px-5 py-3 font-medium text-white transition hover:bg-secondary disabled:cursor-not-allowed disabled:opacity-60"
            disabled={
              !isPasswordValid ||
              !passwordsMatch ||
              resetPasswordMutation.isPending
            }
            type="submit"
          >
            {resetPasswordMutation.isPending
              ? t('auth.reset.updating')
              : t('auth.reset.update')}
          </button>

          <PasswordRequirements
            requirements={requirements}
            passwordsMatch={passwordsMatch}
          />
        </form>

        <Link
          className="mt-7 inline-block text-sm font-medium text-primary underline underline-offset-2 hover:text-secondary"
          to={authenticatedRoutesConstant.HOME}
        >
          {t('auth.backToSignIn')}
        </Link>
      </section>
    </main>
  )
}

function PasswordField({
  label,
  onChange,
  onToggle,
  value,
  visible,
}: {
  label: string
  onChange: (value: string) => void
  onToggle: () => void
  value: string
  visible: boolean
}) {
  const { t } = useTranslation()
  return (
    <label className="block text-sm font-medium text-slate-700">
      {label}
      <span className="relative mt-2 block">
        <input
          autoComplete={
            label === 'New password' ? 'new-password' : 'new-password'
          }
          className="w-full rounded-md border border-slate-300 bg-white px-4 py-3 pr-12 text-slate-900 outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/30"
          onChange={(event) => onChange(event.target.value)}
          required
          type={visible ? 'text' : 'password'}
          value={value}
        />
        <button
          aria-label={
            visible
              ? t('auth.reset.hideField', { field: label.toLowerCase() })
              : t('auth.reset.showField', { field: label.toLowerCase() })
          }
          className="absolute inset-y-0 right-0 px-4 text-slate-500 hover:text-primary"
          onClick={onToggle}
          type="button"
        >
          <EyeIcon visible={visible} />
        </button>
      </span>
    </label>
  )
}

function PasswordRequirements({
  passwordsMatch,
  requirements,
}: {
  passwordsMatch: boolean
  requirements: { label: string; met: boolean }[]
}) {
  const { t } = useTranslation()
  return (
    <div className="rounded-lg bg-slate-50 p-4">
      <p className="text-sm font-semibold text-secondary">
        {t('auth.reset.requirements')}
      </p>
      <ul className="mt-3 space-y-2 text-sm text-slate-600">
        {requirements.map((requirement) => (
          <li className="flex items-center gap-2" key={requirement.label}>
            <span
              aria-hidden="true"
              className={`flex h-5 w-5 items-center justify-center rounded-full border text-xs font-bold ${requirement.met ? 'border-primary bg-primary text-white' : 'border-slate-400 text-slate-400'}`}
            >
              {requirement.met ? '✓' : '•'}
            </span>
            {requirement.label}
          </li>
        ))}
        <li className="flex items-center gap-2">
          <span
            aria-hidden="true"
            className={`flex h-5 w-5 items-center justify-center rounded-full border text-xs font-bold ${passwordsMatch ? 'border-primary bg-primary text-white' : 'border-slate-400 text-slate-400'}`}
          >
            {passwordsMatch ? '✓' : '•'}
          </span>
          {t('auth.reset.passwordsMatch')}
        </li>
      </ul>
    </div>
  )
}
