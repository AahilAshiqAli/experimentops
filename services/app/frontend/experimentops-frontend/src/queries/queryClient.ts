import { QueryCache, QueryClient } from '@tanstack/react-query'

import { Toaster } from '../services/toaster.service'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 30,
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
  queryCache: new QueryCache({
    onError: (error) => {
      Toaster.error(
        error instanceof Error
          ? error.message
          : 'Unable to load the requested data. Please try again.',
      )
    },
  }),
})
