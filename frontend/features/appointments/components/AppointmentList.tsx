"use client";

import { useQuery } from "@tanstack/react-query";
import { getMyAppointments } from "@/features/appointments/api";
import { AppointmentCard } from "@/features/appointments/components/AppointmentCard";
import { Spinner } from "@/components/ui/Spinner";
import type { AppointmentStatus } from "@/types/api";
import { HttpError } from "@/lib/http/client";
import { CalendarX } from "lucide-react";

interface AppointmentListProps {
  statusFilter?: AppointmentStatus;
}

export function AppointmentList({ statusFilter }: AppointmentListProps) {
  const { data, isLoading, error } = useQuery({
    queryKey: ["appointments", "me", statusFilter],
    queryFn: () => getMyAppointments(0, 20, statusFilter),
  });

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner />
      </div>
    );
  }

  if (error) {
    const message =
      error instanceof HttpError ? error.message : "Failed to load appointments.";
    return (
      <div className="rounded-lg bg-red-50 p-4 text-sm text-red-700">
        {message}
      </div>
    );
  }

  if (!data?.content.length) {
    return (
      <div className="flex flex-col items-center gap-3 py-12 text-slate-500">
        <CalendarX className="h-12 w-12" />
        <p>No appointments found.</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {data.content.map((appointment) => (
        <AppointmentCard key={appointment.id} appointment={appointment} />
      ))}
    </div>
  );
}
