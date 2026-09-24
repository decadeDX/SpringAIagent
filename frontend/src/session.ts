import { ref } from 'vue'
import type { LoginResult } from './api/auth'
import { clearAssistantState } from './assistantState'

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
  const stored = sessionStorage.getItem(SESSION_STORAGE_KEY)
  if (!stored) return null
  try {
    return JSON.parse(stored) as StoredSession
  } catch {
    sessionStorage.removeItem(SESSION_STORAGE_KEY)
    return null
  }
}

function applySession(session: StoredSession | null) {
  accessToken.value = session?.accessToken ?? null
  username.value = session?.username ?? '当前用户'
  role.value = session?.role ?? 'STUDENT'
  trainingStatus.value = session?.trainingStatus ?? 'PENDING'
}

export function updateSession(result: LoginResult) {
  const session: StoredSession = {
    accessToken: result.accessToken,
    username: result.user.username,
    role: result.user.role,
    trainingStatus: result.user.trainingStatus,
  }
  sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session))
  sessionExpired.value = false
  applySession(session)
}

export function clearSession() {
  sessionStorage.removeItem(SESSION_STORAGE_KEY)
  clearAssistantState()
  sessionExpired.value = false
  applySession(null)
}

export function invalidateSession() {
  sessionStorage.removeItem(SESSION_STORAGE_KEY)
  clearAssistantState()
  sessionExpired.value = true
  applySession(null)
}

applySession(readStoredSession())
