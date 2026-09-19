import type { ChatMessage } from '../types/api'
import { createDraft } from '../mocks/data'
import { request, useMockApi } from './http'

export async function sendChatMessage(content: string): Promise<ChatMessage> {
  if (useMockApi) {
    return {
      sessionId: 'session-demo-001',
      messageId: crypto.randomUUID(),
      answer: '人工智能实验室预约人需要先通过安全培训。你已满足资格；我已为 3 人生成 B402 的预约草案，请核对后确认。',
      toolCallCount: 3,
      citations: [{ documentTitle: '人工智能实验室使用指南', version: 'v1.0', chunkId: 'AI-GUIDE-003', excerpt: '预约人工智能实验室的人员应先完成安全培训。' }],
      draft: createDraft('CREATE_RESERVATION', { labId: 'LAB-B402', labName: '人工智能实验室', startTime: '2026-09-22T14:00:00+08:00', endTime: '2026-09-22T16:00:00+08:00', participantCount: 3 }),
    }
  }
  return request<ChatMessage>('/chat/sessions/session-demo-001/messages', { method: 'POST', body: JSON.stringify({ content }) })
}
