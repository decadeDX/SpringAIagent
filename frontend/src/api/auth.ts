import { request, useMockApi } from './http'

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
  if (useMockApi) {
    if (!username || !password) throw new Error('请输入账号和密码')
    const isAdmin = username === 'admin01'
    return {
      accessToken: 'mock-access-token',
      tokenType: 'Bearer',
      expiresAt: '2026-12-31T23:59:59+08:00',
      user: {
        id: isAdmin ? 3 : 1,
        username,
        role: isAdmin ? 'ADMIN' : 'STUDENT',
        trainingStatus: isAdmin ? 'PENDING' : 'PASSED',
      },
    }
  }
  return request<LoginResult>('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) })
}
