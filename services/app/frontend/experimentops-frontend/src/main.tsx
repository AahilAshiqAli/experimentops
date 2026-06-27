import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import './index.css'
import App from './App'
import { LoginProvider } from './context-api/logincontext'
import { queryClient } from './queries'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <LoginProvider>
        <App />
      </LoginProvider>
    </QueryClientProvider>
  </StrictMode>,
)
