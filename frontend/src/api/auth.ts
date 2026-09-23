import { request } from './http'

export interface LoginResult {
  accessToken: string
  tokenType: 'Bearer'
  expiresAt: string
  user: {
    id: number
    username: string
    role: 'STUDENT' | 'ADMIN'
    trainingStatus: 'PENDING' | 'PASSED' | 'FAILED'
  }
}

export async function login(username: string, password: string): Promise<LoginResult> {
  return request<LoginResult>('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) })
}
