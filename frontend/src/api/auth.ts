import { request, useMockApi } from './http'

export interface LoginResult {
  accessToken: string
  username: string
  role: 'STUDENT' | 'ADMIN'
}

export async function login(username: string, password: string): Promise<LoginResult> {
  if (useMockApi) {
    if (!username || !password) throw new Error('请输入账号和密码')
    return { accessToken: 'mock-access-token', username, role: username === 'admin01' ? 'ADMIN' : 'STUDENT' }
  }
  return request<LoginResult>('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) })
}
