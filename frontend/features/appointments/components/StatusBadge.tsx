"use client";

import {
  appointmentStatusColor,
  processingStatusColor,
  processingStatusLabel,
} from "@/lib/utils/status";
import type { AppointmentStatus, ProcessingStatus } from "@/types/api";
import { Badge } from "@/components/ui/Badge";
import { CheckCircle, Loader2, BellOff } from "lucide-react";

interface StatusBadgeProps {
  status: AppointmentStatus;
  processingStatus?: ProcessingStatus;
}

export function StatusBadge({ status, processingStatus }: StatusBadgeProps) {
  return (
    <div className="flex flex-wrap gap-2">
      <Badge className={appointmentStatusColor(status)}>{status}</Badge>
      {processingStatus && (
        <Badge className={processingStatusColor(processingStatus)}>
          <span className="flex items-center gap-1">
            {processingStatus === "PENDING" && <Loader2 className="h-3 w-3 animate-spin" />}
            {processingStatus === "NOTIFIED" && <CheckCircle className="h-3 w-3" />}
            {processingStatus === "FAILED" && <BellOff className="h-3 w-3" />}
            {processingStatusLabel(processingStatus)}
          </span>
        </Badge>
      )}
    </div>
  );
}

export function ProgressStepper({ step }: { step: string }) {
  return (
    <div className="rounded-lg bg-primary-50 px-3 py-2 text-sm text-primary-700">
      {step}
    </div>
  );
}
