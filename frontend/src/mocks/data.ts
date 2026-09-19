import type { ActionDraft, KnowledgeDocument, RepairTicket, Reservation } from '../types/api'

export const mockReservations: Reservation[] = [
  {
    id: 1,
    reservationNo: 'RSV-20260922001',
    labId: 'LAB-B402',
    labName: '人工智能实验室',
    startTime: '2026-09-22T14:00:00+08:00',
    endTime: '2026-09-22T16:00:00+08:00',
    participantCount: 3,
    status: 'CONFIRMED',
  },
  {
    id: 2,
    reservationNo: 'RSV-20260923002',
    labId: 'LAB-A301',
    labName: '软件工程实验室',
    startTime: '2026-09-23T09:00:00+08:00',
    endTime: '2026-09-23T11:00:00+08:00',
    participantCount: 8,
    status: 'CONFIRMED',
  },
]

export const mockRepairs: RepairTicket[] = [
  {
    id: 1,
    ticketNo: 'TKT-20260918001',
    labId: 'LAB-A301',
    equipmentInfo: '工作站 A301-12',
    description: '显示器间歇黑屏，重启后恢复。',
    safetyRisk: false,
    status: 'PROCESSING',
    createdAt: '2026-09-18T10:20:00+08:00',
  },
]

export const mockDocuments: KnowledgeDocument[] = [
  { id: 1, logicalDocumentCode: 'LAB-BOOKING', title: '实验室预约管理办法', version: 'v1.0', indexStatus: 'SUCCEEDED', publishStatus: 'PUBLISHED', chunkCount: 12 },
  { id: 2, logicalDocumentCode: 'AI-GUIDE', title: '人工智能实验室使用指南', version: 'v1.0', indexStatus: 'SUCCEEDED', publishStatus: 'PUBLISHED', chunkCount: 9 },
  { id: 3, logicalDocumentCode: 'REPAIR-GUIDE', title: '设备报修操作指南', version: 'v1.1', indexStatus: 'INDEXING', publishStatus: 'DRAFT', chunkCount: 0 },
]

export function createDraft(actionType: ActionDraft['actionType'], payload: Record<string, unknown>): ActionDraft {
  return {
    actionId: crypto.randomUUID(),
    actionType,
    sessionId: 'session-demo-001',
    expiresAt: new Date(Date.now() + 5 * 60_000).toISOString(),
    payload,
    notices: ['草案有效期为 5 分钟，确认时将重新校验当前规则与可用性。'],
  }
}
