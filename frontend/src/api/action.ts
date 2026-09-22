import type { ActionDraft, ActionExecution } from '../types/api'
import { request } from './http'

export async function confirmAction(draft: ActionDraft): Promise<ActionExecution> {
  return request<ActionExecution>(`/actions/${draft.actionId}/confirm`, {
    method: 'POST',
    body: JSON.stringify({ sessionId: draft.sessionId }),
  })
}
