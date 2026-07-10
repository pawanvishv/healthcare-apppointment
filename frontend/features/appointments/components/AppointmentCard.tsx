"use client";

import Link from "next/link";
import { formatDateTime } from "@/lib/utils/date";
import { StatusBadge } from "@/features/appointments/components/StatusBadge";
import type { Appointment } from "@/types/api";
import { Calendar, ChevronRight } from "lucide-react";

interface AppointmentCardProps {
  appointment: Appointment;
}

export function AppointmentCard({ appointment }: AppointmentCardProps) {
  return (
    <Link
      href={`/appointments/${appointment.id}`}
      className="block rounded-xl border border-slate-200 bg-white p-4 shadow-sm transition-shadow hover:shadow-md"
    >
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-2">
          <div className="flex items-center gap-2 text-sm text-slate-500">
            <Calendar className="h-4 w-4" />
            <span>{formatDateTime(appointment.startTime)}</span>
          </div>
          <p className="font-medium text-slate-900">
            Doctor: {appointment.doctorId}
          </p>
          <StatusBadge
            status={appointment.status}
            processingStatus={appointment.processingStatus}
          />
        </div>
        <ChevronRight className="h-5 w-5 text-slate-400" />
      </div>
    </Link>
  );
}
