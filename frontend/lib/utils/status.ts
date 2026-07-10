import type { AppointmentStatus, ProcessingStatus } from "@/types/api";

export function appointmentStatusColor(status: AppointmentStatus): string {
  switch (status) {
    case "CONFIRMED":
      return "bg-green-100 text-green-800";
    case "CANCELLED":
      return "bg-red-100 text-red-800";
    case "COMPLETED":
      return "bg-blue-100 text-blue-800";
    default:
      return "bg-yellow-100 text-yellow-800";
  }
}

export function processingStatusColor(status: ProcessingStatus): string {
  switch (status) {
    case "NOTIFIED":
      return "bg-green-100 text-green-800";
    case "FAILED":
      return "bg-red-100 text-red-800";
    default:
      return "bg-amber-100 text-amber-800";
  }
}

export function processingStatusLabel(status: ProcessingStatus): string {
  switch (status) {
    case "NOTIFIED":
      return "Notified";
    case "FAILED":
      return "Notification failed";
    default:
      return "Processing notification...";
  }
}
