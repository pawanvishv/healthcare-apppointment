export type Role = "PATIENT" | "DOCTOR" | "ADMIN";

export type AppointmentStatus = "PENDING" | "CONFIRMED" | "CANCELLED" | "COMPLETED";

export type ProcessingStatus = "PENDING" | "NOTIFIED" | "FAILED";

export interface ApiError {
  timestamp: string;
  path: string;
  status: number;
  errorCode: string;
  message: string;
  traceId?: string;
  details?: string[];
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error: ApiError | null;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface AuthTokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface Slot {
  id: string;
  doctorId: string;
  startTime: string | number;
  endTime: string | number;
  available: boolean;
}

export interface Appointment {
  id: string;
  status: AppointmentStatus;
  processingStatus: ProcessingStatus;
  doctorId: string;
  slotId: string;
  patientId: string;
  startTime: string | number;
  endTime: string | number;
  createdAt: string | number;
}

export interface AppointmentLog {
  id: string;
  eventType: string;
  oldStatus: AppointmentStatus | null;
  newStatus: AppointmentStatus | null;
  actor: string;
  message: string;
  createdAt: string | number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  role: Role;
  specialization?: string;
  phone?: string;
}

export interface BookAppointmentRequest {
  doctorId: string;
  slotId: string;
}

export interface RescheduleRequest {
  newSlotId: string;
}
