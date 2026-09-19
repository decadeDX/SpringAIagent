import { createDraft, mockRepairs } from '../mocks/data'
import type { ActionDraft, RepairTicket } from '../types/api'
import { request, useMockApi } from './http'

export interface RepairDraftInput {
  labId: string
  equipmentInfo: string
  description: string
}

export async function getMyRepairTickets(): Promise<RepairTicket[]> {
  return useMockApi ? mockRepairs : request<RepairTicket[]>('/repair-tickets/me')
}

export async function prepareRepair(input: RepairDraftInput): Promise<ActionDraft> {
  if (useMockApi) return createDraft('CREATE_REPAIR_TICKET', { ...input })
  return request<ActionDraft>('/repair-drafts', { method: 'POST', body: JSON.stringify(input) })
}
