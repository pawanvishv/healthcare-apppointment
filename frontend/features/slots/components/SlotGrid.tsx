"use client";

import { useEffect, useRef } from "react";
import { useQuery } from "@tanstack/react-query";
import { getAvailableSlots } from "@/features/slots/api";
import { SlotCard } from "@/features/slots/components/SlotCard";
import { Skeleton } from "@/components/ui/Spinner";
import { useAuthStore } from "@/store/auth-store";
import { HttpError } from "@/lib/http/client";
import type { Slot } from "@/types/api";
import { Calendar } from "lucide-react";

interface SlotGridProps {
  doctorId: string;
  date: string;
  selectedSlotId?: string;
  preselectedSlotId?: string;
  onSelectSlot: (slot: Slot) => void;
}

export function SlotGrid({
  doctorId,
  date,
  selectedSlotId,
  preselectedSlotId,
  onSelectSlot,
}: SlotGridProps) {
  const accessToken = useAuthStore((s) => s.accessToken);
  const hasHydrated = useAuthStore((s) => s._hasHydrated);

  const { data: slots, isLoading, isFetching, error } = useQuery({
    queryKey: ["slots", doctorId, date],
    queryFn: () => getAvailableSlots(doctorId, date),
    enabled: hasHydrated && !!accessToken && !!doctorId && !!date,
    staleTime: 60_000,
    placeholderData: (prev) => prev,
  });

  const appliedPreselect = useRef(false);

  useEffect(() => {
    if (!preselectedSlotId || !slots?.length || appliedPreselect.current) return;
    const match = slots.find((s) => s.id === preselectedSlotId);
    if (match) {
      onSelectSlot(match);
      appliedPreselect.current = true;
    }
  }, [preselectedSlotId, slots, onSelectSlot]);

  if (isLoading && !slots) {
    return (
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <Skeleton key={i} className="h-14" />
        ))}
      </div>
    );
  }

  if (error) {
    const message =
      error instanceof HttpError
        ? error.message
        : "Failed to load slots. Please try again.";
    return (
      <div className="rounded-lg bg-red-50 p-4 text-sm text-red-700">
        {message}
      </div>
    );
  }

  if (!slots?.length) {
    return (
      <div className="flex flex-col items-center gap-2 rounded-lg border border-dashed border-slate-300 py-10 text-slate-500">
        <Calendar className="h-10 w-10" />
        <p className="text-sm">No slots available — try another date.</p>
      </div>
    );
  }

  return (
    <div>
      {isFetching && (
        <p className="mb-2 text-xs text-slate-400">Updating slots...</p>
      )}
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
        {slots.map((slot) => (
          <SlotCard
            key={slot.id}
            slot={slot}
            selected={selectedSlotId === slot.id}
            onSelect={onSelectSlot}
          />
        ))}
      </div>
    </div>
  );
}
