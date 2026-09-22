import type { ActionDraft, Reservation } from '../types/api'
import { request } from './http'

export async function getMyReservations(): Promise<Reservation[]> {
  const page = await request<{ items: Reservation[] }>('/reservations/me')
  return page.items
}

export async function prepareCancellation(reservation: Reservation): Promise<ActionDraft> {
  return request<ActionDraft>(`/reservations/${reservation.id}/cancellation-draft`, { method: 'POST' })
}
