import axios, {
  type AxiosRequestConfig,
  type Method,
} from 'axios'

import { BASE_URL } from './environment'

type ApiErrorResponse = {
  error?: string
  errorMessage?: string
  message?: string
}

export class ApiServiceError extends Error {
  public readonly data: unknown
  public readonly status: number

  constructor(message: string, status: number, data?: unknown) {
    super(message)
    this.name = 'ApiServiceError'
    this.data = data
    this.status = status
  }
}

const apiService = axios.create({
  baseURL: BASE_URL,
})

function getErrorMessage(payload: unknown, fallbackMessage: string) {
  if (typeof payload !== 'object' || payload === null) {
    return fallbackMessage
  }

  const { error, errorMessage, message } = payload as ApiErrorResponse
  return message ?? errorMessage ?? error ?? fallbackMessage
}

export async function handleApiRequest<TResponse, TRequest = unknown>(
  method: Method,
  url: string,
  data?: TRequest,
  requestConfig: AxiosRequestConfig = {},
): Promise<TResponse> {
  try {
    const response = await apiService.request<TResponse>({
      ...requestConfig,
      data,
      method,
      url,
    })

    return response.data
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const responseData: unknown = error.response?.data
      const fallbackMessage = error.response
        ? 'The request could not be completed.'
        : 'Unable to connect to the service. Please try again.'

      throw new ApiServiceError(
        getErrorMessage(responseData, fallbackMessage),
        error.response?.status ?? 0,
        responseData,
      )
    }

    throw error
  }
}

const get = <TResponse>(url: string, requestConfig?: AxiosRequestConfig) =>
  handleApiRequest<TResponse>('get', url, undefined, requestConfig)

const post = <TResponse, TRequest = unknown>(
  url: string,
  data?: TRequest,
  requestConfig?: AxiosRequestConfig,
) => handleApiRequest<TResponse, TRequest>('post', url, data, requestConfig)

const put = <TResponse, TRequest = unknown>(
  url: string,
  data?: TRequest,
  requestConfig?: AxiosRequestConfig,
) => handleApiRequest<TResponse, TRequest>('put', url, data, requestConfig)

const patch = <TResponse, TRequest = unknown>(
  url: string,
  data?: TRequest,
  requestConfig?: AxiosRequestConfig,
) => handleApiRequest<TResponse, TRequest>('patch', url, data, requestConfig)

const ApiService = {
  get,
  patch,
  post,
  put,
}

export default ApiService
