export type ActionType = 'CREATE_RESERVATION' | 'CANCEL_RESERVATION' | 'CREATE_REPAIR_TICKET'

export interface ApiResponse<T> {
  code: number
  message: string
  requestId: string
  data: T
}

export interface ActionExecution {
  actionId: string
  actionType: ActionType
  executionStatus: 'SUCCEEDED'
  idempotentReplay: boolean
  result: Record<string, unknown>
}

export interface ChatSession {
  sessionId: string
  name?: string
  createdAt: string
}

export interface ActionDraft {
  actionId: string
  actionType: ActionType
  sessionId: string
  expiresAt: string
  payload: Record<string, unknown>
  notices: string[]
}

export interface Reservation {
  id: number
  reservationNo: string
  labId: string
  labName: string
  startTime: string
  endTime: string
  participantCount: number
  status: 'CONFIRMED' | 'CANCELLED'
}

export interface RepairTicket {
  id: number
  ticketNo: string
  labId: string
  equipmentInfo: string
  description: string
  safetyRisk: boolean
  status: 'SUBMITTED' | 'PROCESSING' | 'RESOLVED'
  resolutionNote?: string
  createdAt: string
}

export interface Citation {
  documentTitle: string
  version: string
  chunkId: string
  excerpt: string
}

export interface ChatMessage {
  sessionId: string
  messageId: string
  answer: string
  citations: Citation[]
  toolCallCount: number
  draft?: ActionDraft
}

export interface KnowledgeDocument {
  id: number
  logicalDocumentCode: string
  title: string
  version: string
  indexStatus: 'PENDING' | 'INDEXING' | 'SUCCEEDED' | 'FAILED'
  publishStatus: 'DRAFT' | 'PUBLISHED' | 'DISABLED'
  chunkCount: number
}
