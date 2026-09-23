import type { ApiResponse } from '../types/api'
import { accessToken, invalidateSession } from '../session'

const baseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api'

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = accessToken.value
  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  })
  const body = (await response.json()) as ApiResponse<T>
  if (response.status === 401) invalidateSession()
  if (!response.ok) throw new Error(body.message)
  return body.data
}

export const useMockApi = import.meta.env.VITE_USE_MOCK_API !== 'false'
