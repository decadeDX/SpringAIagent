import { ref } from 'vue'
import type { LoginResult } from './api/auth'

export const SESSION_STORAGE_KEY = 'authSession'

interface StoredSession {
  accessToken: string
  username: string
  role: 'STUDENT' | 'ADMIN'
  trainingStatus: 'PENDING' | 'PASSED' | 'FAILED'
}

export const accessToken = ref<string | null>(null)
export const username = ref('当前用户')
export const role = ref<StoredSession['role']>('STUDENT')
export const trainingStatus = ref<StoredSession['trainingStatus']>('PENDING')
export const sessionExpired = ref(false)

function readStoredSession(): StoredSession | null {
  const stored = localStorage.getItem(SESSION_STORAGE_KEY)
  if (!stored) return null
  try {
    return JSON.parse(stored) as StoredSession
  } catch {
    localStorage.removeItem(SESSION_STORAGE_KEY)
    return null
  }
}

function applySession(session: StoredSession | null) {
  accessToken.value = session?.accessToken ?? null
  username.value = session?.username ?? '当前用户'
  role.value = session?.role ?? 'STUDENT'
  trainingStatus.value = session?.trainingStatus ?? 'PENDING'
}

export function synchronizeSession() {
  sessionExpired.value = false
  applySession(readStoredSession())
}

export function updateSession(result: LoginResult) {
  const session: StoredSession = {
    accessToken: result.accessToken,
    username: result.user.username,
    role: result.user.role,
    trainingStatus: result.user.trainingStatus,
  }
  localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session))
  sessionExpired.value = false
  applySession(session)
}

export function clearSession() {
  localStorage.removeItem(SESSION_STORAGE_KEY)
  sessionExpired.value = false
  applySession(null)
}

export function invalidateSession() {
  localStorage.removeItem(SESSION_STORAGE_KEY)
  sessionExpired.value = true
  applySession(null)
}

synchronizeSession()
