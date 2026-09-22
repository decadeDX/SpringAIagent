import type { ActionDraft, RepairTicket } from '../types/api'
import { request } from './http'

export interface RepairDraftInput {
  labId: string
  equipmentInfo: string
  description: string
}

export async function getMyRepairTickets(): Promise<RepairTicket[]> {
  const page = await request<{ items: RepairTicket[] }>('/repair-tickets/me')
  return page.items
}

export async function prepareRepair(input: RepairDraftInput): Promise<ActionDraft> {
  return request<ActionDraft>('/repair-drafts', { method: 'POST', body: JSON.stringify(input) })
}
