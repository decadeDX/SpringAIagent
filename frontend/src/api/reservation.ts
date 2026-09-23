import type { ActionDraft, Lab, LabAvailability, Reservation, ReservationDraftInput } from '../types/api'
import { request } from './http'

export async function getMyReservations(): Promise<Reservation[]> {
  const page = await request<{ items: Reservation[] }>('/reservations/me')
  return page.items
}

export async function prepareCancellation(reservation: Reservation): Promise<ActionDraft> {
  return request<ActionDraft>(`/reservations/${reservation.id}/cancellation-draft`, { method: 'POST' })
}

export async function getLabs(): Promise<Lab[]> {
  const page = await request<{ items: Lab[] }>('/labs?size=50')
  return page.items
}

export async function getLabAvailability(labId: string, date: string): Promise<LabAvailability> {
  return request<LabAvailability>(`/labs/${encodeURIComponent(labId)}/availability?date=${encodeURIComponent(date)}`)
}

export async function prepareReservation(input: ReservationDraftInput): Promise<ActionDraft> {
  return request<ActionDraft>('/reservation-drafts', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}
