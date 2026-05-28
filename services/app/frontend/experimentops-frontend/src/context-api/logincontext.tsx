/* eslint-disable react-refresh/only-export-components */

import {
  createContext,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'

export type LoginUser = {
  email: string
  name: string
}

type LoginContextValue = {
  user: LoginUser | null
  isAuthenticated: boolean
  login: (user: LoginUser) => void
  logout: () => void
}

const LoginContext = createContext<LoginContextValue | undefined>(undefined)

type LoginProviderProps = {
  children: ReactNode
}

export function LoginProvider({ children }: LoginProviderProps) {
  const [user, setUser] = useState<LoginUser | null>(null)

  const value = useMemo<LoginContextValue>(
    () => ({
      user,
      isAuthenticated: user !== null,
      login: (nextUser) => setUser(nextUser),
      logout: () => setUser(null),
    }),
    [user],
  )

  return <LoginContext.Provider value={value}>{children}</LoginContext.Provider>
}

export function useLogin() {
  const context = useContext(LoginContext)

  if (context === undefined) {
    throw new Error('useLogin must be used inside a LoginProvider')
  }

  return context
}
