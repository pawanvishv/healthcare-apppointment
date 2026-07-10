"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { SlotGrid } from "@/features/slots/components/SlotGrid";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { useDebounce } from "@/hooks/useDebounce";
import { formatTime } from "@/lib/utils/date";
import type { Slot } from "@/types/api";

const DEFAULT_DOCTOR_ID = "d-101";

export default function SlotsPage() {
  const router = useRouter();
  const [doctorId, setDoctorId] = useState(DEFAULT_DOCTOR_ID);
  const [date, setDate] = useState("2026-07-10");
  const [selectedSlot, setSelectedSlot] = useState<Slot | null>(null);

  const debouncedDoctorId = useDebounce(doctorId);
  const debouncedDate = useDebounce(date);

  const handleBook = () => {
    if (selectedSlot) {
      router.push(
        `/appointments/new?doctorId=${debouncedDoctorId}&slotId=${selectedSlot.id}&date=${debouncedDate}`
      );
    }
  };

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-xl font-bold text-slate-900">Browse Available Slots</h1>
        <p className="text-sm text-slate-500">
          Find and select an available appointment slot
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
        onSelectSlot={setSelectedSlot}
      />

      {selectedSlot && (
        <Button onClick={handleBook}>
          Book {formatTime(selectedSlot.startTime)} Slot
        </Button>
      )}
    </div>
  );
}
