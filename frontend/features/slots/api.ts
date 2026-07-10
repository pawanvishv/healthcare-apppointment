import { apiClient } from "@/lib/http/client";
import type { Slot } from "@/types/api";

export async function getAvailableSlots(
  doctorId: string,
  date: string
): Promise<Slot[]> {
  return apiClient<Slot[]>(`/slots?doctorId=${doctorId}&date=${date}`);
}
