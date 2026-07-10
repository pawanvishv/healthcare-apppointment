"use client";

import { AppointmentList } from "@/features/appointments/components/AppointmentList";
import { Select } from "@/components/ui/Select";
import { useAuthStore } from "@/store/auth-store";
import { useState } from "react";
import type { AppointmentStatus } from "@/types/api";

export default function AppointmentsPage() {
  const [statusFilter, setStatusFilter] = useState<AppointmentStatus | "">("");
  const role = useAuthStore((s) => s.role);
  const isDoctor = role === "DOCTOR";

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">
            {isDoctor ? "Patient Appointments" : "My Appointments"}
          </h1>
          <p className="text-sm text-slate-500">
            {isDoctor
              ? "View appointments scheduled with you"
              : "View and manage your appointments"}
          </p>
        </div>
        <div className="w-full sm:w-48">
          <Select
            label="Filter by status"
            options={[
              { value: "", label: "All" },
              { value: "CONFIRMED", label: "Confirmed" },
              { value: "CANCELLED", label: "Cancelled" },
              { value: "COMPLETED", label: "Completed" },
            ]}
            value={statusFilter}
            onChange={(e) =>
              setStatusFilter(e.target.value as AppointmentStatus | "")
            }
          />
        </div>
      </div>
      <AppointmentList statusFilter={statusFilter || undefined} />
    </div>
  );
}
