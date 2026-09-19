import { mockReservations, createDraft } from '../mocks/data'
import type { ActionDraft, Reservation } from '../types/api'
import { request, useMockApi } from './http'

export async function getMyReservations(): Promise<Reservation[]> {
  return useMockApi ? mockReservations : request<Reservation[]>('/reservations/me')
}

export async function prepareCancellation(reservation: Reservation): Promise<ActionDraft> {
  if (useMockApi) return createDraft('CANCEL_RESERVATION', { reservationId: reservation.id, reservationNo: reservation.reservationNo, labName: reservation.labName, startTime: reservation.startTime })
  return request<ActionDraft>(`/reservations/${reservation.id}/cancellation-draft`, { method: 'POST' })
}

export async function confirmCancellation(reservationId: number): Promise<void> {
  if (useMockApi) {
    const reservation = mockReservations.find((item) => item.id === reservationId)
    if (reservation) reservation.status = 'CANCELLED'
    return
  }
}
