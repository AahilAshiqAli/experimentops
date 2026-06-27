export const BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/v1').replace(
  /\/$/,
  '',
)
