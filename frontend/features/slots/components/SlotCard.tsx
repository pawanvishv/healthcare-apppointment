"use client";

import { memo } from "react";
import { formatTime } from "@/lib/utils/date";
import { cn } from "@/lib/utils/cn";
import type { Slot } from "@/types/api";

interface SlotCardProps {
  slot: Slot;
  selected: boolean;
  onSelect: (slot: Slot) => void;
}

export const SlotCard = memo(function SlotCard({
  slot,
  selected,
  onSelect,
}: SlotCardProps) {
  return (
    <button
      type="button"
      onClick={() => onSelect(slot)}
      className={cn(
        "rounded-lg border-2 p-3 text-left min-h-[56px]",
        selected
          ? "border-primary-600 bg-primary-50"
          : "border-slate-200 bg-white hover:border-primary-400"
      )}
    >
      <p className="text-sm font-semibold text-slate-900">
        {formatTime(slot.startTime)}
      </p>
      <p className="text-xs text-slate-500">to {formatTime(slot.endTime)}</p>
    </button>
  );
});
