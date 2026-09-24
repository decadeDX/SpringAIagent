import { ref } from 'vue'
import type { ActionDraft, Citation } from './types/api'

export interface DisplayMessage {
  role: 'user' | 'assistant'
  content: string
  citations?: Citation[]
}

function createWelcomeMessage(): DisplayMessage[] {
  return [{ role: 'assistant', content: '你好，我可以帮你查询实验室规定、可用时段、预约和报修。涉及预约、取消或报修时，我会先生成草案，等待你在页面上明确确认。' }]
}

export const messages = ref<DisplayMessage[]>(createWelcomeMessage())
export const draft = ref<ActionDraft>()
export const confirmed = ref(false)
export const chatSessionId = ref('')

export function clearAssistantState() {
  messages.value = createWelcomeMessage()
  draft.value = undefined
  confirmed.value = false
  chatSessionId.value = ''
}
