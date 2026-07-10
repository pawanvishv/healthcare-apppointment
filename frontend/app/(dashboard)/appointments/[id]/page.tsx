"use client";

import { useParams } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getAppointment,
  getAppointmentHistory,
  cancelAppointment,
  rescheduleAppointment,
} from "@/features/appointments/api";
import { getAvailableSlots } from "@/features/slots/api";
import { StatusBadge } from "@/features/appointments/components/StatusBadge";
import { formatDateTime, formatTime } from "@/lib/utils/date";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { useToast } from "@/components/ui/Toast";
import { HttpError } from "@/lib/http/client";
import { useState } from "react";
import { ArrowLeft, History } from "lucide-react";
import Link from "next/link";
import type { ProcessingStatus } from "@/types/api";

export default function AppointmentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const [showReschedule, setShowReschedule] = useState(false);
  const [rescheduleDate, setRescheduleDate] = useState("2026-07-10");
  const [newSlotId, setNewSlotId] = useState("");

  const { data: appointment, isLoading } = useQuery({
    queryKey: ["appointments", id],
    queryFn: () => getAppointment(id),
    refetchInterval: (query) => {
      const status = query.state.data?.processingStatus as ProcessingStatus | undefined;
      if (status === "PENDING") return 3000;
      return false;
    },
  });

  const { data: history } = useQuery({
    queryKey: ["appointments", id, "history"],
    queryFn: () => getAppointmentHistory(id),
    enabled: !!id,
  });

  const { data: slots } = useQuery({
    queryKey: ["slots", appointment?.doctorId, rescheduleDate],
    queryFn: () => getAvailableSlots(appointment!.doctorId, rescheduleDate),
    enabled: showReschedule && !!appointment?.doctorId,
  });

  const cancelMutation = useMutation({
    mutationFn: () => cancelAppointment(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["appointments"] });
      queryClient.invalidateQueries({ queryKey: ["slots"] });
      showToast("Appointment cancelled", "success");
    },
    onError: (err) =>
      showToast(err instanceof HttpError ? err.message : "Cancel failed", "error"),
  });

  const rescheduleMutation = useMutation({
    mutationFn: () => rescheduleAppointment(id, { newSlotId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["appointments"] });
      queryClient.invalidateQueries({ queryKey: ["slots"] });
      setShowReschedule(false);
      showToast("Appointment rescheduled!", "success");
    },
    onError: (err) => {
      if (err instanceof HttpError && err.status === 409) {
        showToast("New slot taken — pick another.", "error");
        queryClient.invalidateQueries({
          queryKey: ["slots", appointment?.doctorId, rescheduleDate],
        });
      } else {
        showToast(err instanceof HttpError ? err.message : "Reschedule failed", "error");
      }
    },
  });

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner />
      </div>
    );
  }

  if (!appointment) {
    return <p className="text-red-600">Appointment not found.</p>;
  }

  const canCancel = appointment.status === "CONFIRMED";
  const canReschedule = appointment.status === "CONFIRMED";

  return (
    <div className="space-y-6">
      <Link
        href="/appointments"
        className="inline-flex items-center gap-1 text-sm text-primary-600 hover:underline"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to appointments
      </Link>

      <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <h1 className="text-2xl font-bold text-slate-900">Appointment Details</h1>
        <p className="mt-1 text-sm text-slate-500">ID: {appointment.id}</p>

        <div className="mt-4 space-y-3">
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <p className="text-xs text-slate-500">Date & Time</p>
              <p className="font-medium">{formatDateTime(appointment.startTime)}</p>
            </div>
            <div>
              <p className="text-xs text-slate-500">Doctor</p>
              <p className="font-medium">{appointment.doctorId}</p>
            </div>
            <div>
              <p className="text-xs text-slate-500">Slot</p>
              <p className="font-medium">{appointment.slotId}</p>
            </div>
            <div>
              <p className="text-xs text-slate-500">Booked on</p>
              <p className="font-medium">{formatDateTime(appointment.createdAt)}</p>
            </div>
          </div>

          <StatusBadge
            status={appointment.status}
            processingStatus={appointment.processingStatus}
          />
        </div>

        {canCancel && (
          <div className="mt-6 flex flex-wrap gap-3">
            <Button
              variant="danger"
              loading={cancelMutation.isPending}
              onClick={() => {
                if (confirm("Are you sure you want to cancel this appointment?")) {
                  cancelMutation.mutate();
                }
              }}
            >
              Cancel Appointment
            </Button>
            <Button
              variant="secondary"
              onClick={() => setShowReschedule(!showReschedule)}
            >
              Reschedule
            </Button>
          </div>
        )}

        {showReschedule && canReschedule && (
          <div className="mt-4 rounded-lg border border-slate-200 p-4 space-y-3">
            <h3 className="font-medium">Reschedule to new slot</h3>
            <Input
              label="Date"
              type="date"
              value={rescheduleDate}
              onChange={(e) => setRescheduleDate(e.target.value)}
            />
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
              {slots?.map((slot) => (
                <button
                  key={slot.id}
                  type="button"
                  onClick={() => setNewSlotId(slot.id)}
                  className={`rounded-lg border-2 p-3 text-sm ${
                    newSlotId === slot.id
                      ? "border-primary-600 bg-primary-50"
                      : "border-slate-200"
                  }`}
                >
                  {formatTime(slot.startTime)}
                </button>
              ))}
            </div>
            <Button
              loading={rescheduleMutation.isPending}
              disabled={!newSlotId}
              onClick={() => rescheduleMutation.mutate()}
            >
              Confirm Reschedule
            </Button>
          </div>
        )}
      </div>

      {history && history.length > 0 && (
        <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-center gap-2 mb-4">
            <History className="h-5 w-5 text-slate-500" />
            <h2 className="font-semibold text-slate-900">Audit History</h2>
          </div>
          <div className="space-y-3">
            {history.map((log) => (
              <div
                key={log.id}
                className="flex items-start gap-3 border-l-2 border-primary-200 pl-4"
              >
                <div>
                  <p className="text-sm font-medium">{log.eventType}</p>
                  <p className="text-xs text-slate-500">{log.message}</p>
                  <p className="text-xs text-slate-400">
                    {formatDateTime(log.createdAt)}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
