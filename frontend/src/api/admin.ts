import type { KnowledgeDocument, Lab, LabStatus, RepairTicket } from '../types/api'
import { request } from './http'

interface Page<T> {
  items: T[]
  page: number
  size: number
  total: number
}

export async function getLabs(): Promise<Lab[]> {
  return (await request<Page<Lab>>('/labs?page=1&size=50')).items
}

export async function updateLabStatus(labId: string, status: LabStatus): Promise<Lab> {
  return request<Lab>(`/admin/labs/${labId}`, { method: 'PATCH', body: JSON.stringify({ status }) })
}

export async function getSubmittedRepairTickets(): Promise<RepairTicket[]> {
  return (await request<Page<RepairTicket>>('/admin/repair-tickets?status=SUBMITTED&page=1&size=20')).items
}

export async function processRepairTicket(ticketId: number, resolutionNote: string): Promise<RepairTicket> {
  return request<RepairTicket>(`/admin/repair-tickets/${ticketId}`, {
    method: 'PATCH',
    body: JSON.stringify({ status: 'PROCESSING', resolutionNote }),
  })
}

export async function getKnowledgeDocuments(): Promise<KnowledgeDocument[]> {
  return (await request<Page<KnowledgeDocument>>('/admin/knowledge/documents?page=1&size=20')).items
}

export async function uploadKnowledgeDocument(input: {
  file: File
  logicalDocumentCode: string
  title: string
  version: string
  effectiveAt: string
}): Promise<KnowledgeDocument> {
  const formData = new FormData()
  formData.append('file', input.file)
  formData.append('logicalDocumentCode', input.logicalDocumentCode)
  formData.append('title', input.title)
  formData.append('version', input.version)
  formData.append('effectiveAt', `${input.effectiveAt}:00+08:00`)
  return request<KnowledgeDocument>('/admin/knowledge/documents', { method: 'POST', body: formData })
}

export async function publishKnowledgeDocument(documentId: number): Promise<KnowledgeDocument> {
  return request<KnowledgeDocument>(`/admin/knowledge/documents/${documentId}/publish`, { method: 'POST' })
}

export async function disableKnowledgeDocument(documentId: number): Promise<KnowledgeDocument> {
  return request<KnowledgeDocument>(`/admin/knowledge/documents/${documentId}/disable`, { method: 'POST' })
}
