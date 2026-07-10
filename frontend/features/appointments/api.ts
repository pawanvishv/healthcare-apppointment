import { apiClient } from "@/lib/http/client";
import type {
  Appointment,
  AppointmentLog,
  AppointmentStatus,
  BookAppointmentRequest,
  PageResponse,
  RescheduleRequest,
} from "@/types/api";

export async function bookAppointment(
  data: BookAppointmentRequest,
  idempotencyKey?: string
): Promise<Appointment> {
  const headers: Record<string, string> = {};
  if (idempotencyKey) {
    headers["Idempotency-Key"] = idempotencyKey;
  }

  return apiClient<Appointment>("/appointments", {
    method: "POST",
    body: JSON.stringify(data),
    headers,
  });
}

export async function getMyAppointments(
  page = 0,
  size = 10,
  status?: AppointmentStatus
): Promise<PageResponse<Appointment>> {
  let url = `/appointments/me?page=${page}&size=${size}`;
  if (status) url += `&status=${status}`;
  return apiClient<PageResponse<Appointment>>(url);
}

export async function getAppointment(id: string): Promise<Appointment> {
  return apiClient<Appointment>(`/appointments/${id}`);
}

export async function cancelAppointment(id: string): Promise<Appointment> {
  return apiClient<Appointment>(`/appointments/${id}/cancel`, {
    method: "PATCH",
  });
}

export async function rescheduleAppointment(
  id: string,
  data: RescheduleRequest
): Promise<Appointment> {
  return apiClient<Appointment>(`/appointments/${id}/reschedule`, {
    method: "PATCH",
    body: JSON.stringify(data),
  });
}

export async function getAppointmentHistory(
  id: string
): Promise<AppointmentLog[]> {
  return apiClient<AppointmentLog[]>(`/appointments/${id}/history`);
}
