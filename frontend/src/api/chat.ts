import type { ChatMessage } from '../types/api'
import type { ChatSession } from '../types/api'
import { request } from './http'

export async function createChatSession(): Promise<ChatSession> {
  return request<ChatSession>('/chat/sessions', { method: 'POST', body: JSON.stringify({}) })
}

export async function sendChatMessage(sessionId: string, content: string): Promise<ChatMessage> {
  return request<ChatMessage>(`/chat/sessions/${sessionId}/messages`, { method: 'POST', body: JSON.stringify({ content }) })
}
