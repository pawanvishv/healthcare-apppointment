"use client";

import { useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { bookAppointment } from "@/features/appointments/api";
import { SlotGrid } from "@/features/slots/components/SlotGrid";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { useToast } from "@/components/ui/Toast";
import { useAuthStore } from "@/store/auth-store";
import { useDebounce } from "@/hooks/useDebounce";
import { formatTime } from "@/lib/utils/date";
import { HttpError } from "@/lib/http/client";
import type { Slot } from "@/types/api";

const DEFAULT_DOCTOR_ID = "d-101";

function NewAppointmentContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const role = useAuthStore((s) => s.role);

  const [doctorId, setDoctorId] = useState(
    () => searchParams.get("doctorId") ?? DEFAULT_DOCTOR_ID
  );
  const [date, setDate] = useState(
    () => searchParams.get("date") ?? "2026-07-10"
  );
  const [selectedSlot, setSelectedSlot] = useState<Slot | null>(null);
  const [idempotencyKey] = useState(() => crypto.randomUUID());

  const preselectedSlotId = searchParams.get("slotId") ?? undefined;
  const debouncedDoctorId = useDebounce(doctorId);
  const debouncedDate = useDebounce(date);

  const bookMutation = useMutation({
    mutationFn: () =>
      bookAppointment(
        { doctorId: debouncedDoctorId, slotId: selectedSlot!.id },
        idempotencyKey
      ),
    onSuccess: (appointment) => {
      queryClient.invalidateQueries({ queryKey: ["slots"] });
      queryClient.invalidateQueries({ queryKey: ["appointments"] });
      showToast("Appointment booked successfully!", "success");
      router.push(`/appointments/${appointment.id}`);
    },
    onError: (err) => {
      if (err instanceof HttpError && err.status === 409) {
        showToast("Slot no longer available — please pick another.", "error");
        queryClient.invalidateQueries({
          queryKey: ["slots", debouncedDoctorId, debouncedDate],
        });
        setSelectedSlot(null);
      } else {
        showToast(
          err instanceof HttpError ? err.message : "Booking failed.",
          "error"
        );
      }
    },
  });

  if (role && role !== "PATIENT") {
    return (
      <div className="rounded-lg bg-amber-50 p-6 text-amber-800">
        Only patients can book appointments. Please login as a patient.
      </div>
    );
  }

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-xl font-bold text-slate-900">Book Appointment</h1>
        <p className="text-sm text-slate-500">
          Select a doctor, date, and available time slot
        </p>
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <Input
          label="Doctor ID"
          value={doctorId}
          onChange={(e) => {
            setDoctorId(e.target.value);
            setSelectedSlot(null);
          }}
        />
        <Input
          label="Date"
          type="date"
          value={date}
          onChange={(e) => {
            setDate(e.target.value);
            setSelectedSlot(null);
          }}
        />
      </div>

      <SlotGrid
        doctorId={debouncedDoctorId}
        date={debouncedDate}
        selectedSlotId={selectedSlot?.id}
        preselectedSlotId={preselectedSlotId}
        onSelectSlot={setSelectedSlot}
      />

      <Button
        className="w-full sm:w-auto"
        disabled={!selectedSlot}
        loading={bookMutation.isPending}
        onClick={() => bookMutation.mutate()}
      >
        {selectedSlot
          ? `Book ${formatTime(selectedSlot.startTime)} Slot`
          : "Book Selected Slot"}
      </Button>
    </div>
  );
}

export default function NewAppointmentPage() {
  return (
    <Suspense
      fallback={
        <div className="flex justify-center py-16">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-200 border-t-primary-600" />
        </div>
      }
    >
      <NewAppointmentContent />
    </Suspense>
  );
}
