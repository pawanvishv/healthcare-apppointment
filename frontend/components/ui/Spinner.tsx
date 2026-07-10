import { cn } from "@/lib/utils/cn";

export function Spinner({ className }: { className?: string }) {
  return (
    <div
      className={cn(
        "h-8 w-8 animate-spin rounded-full border-4 border-primary-200 border-t-primary-600",
        className
      )}
    />
  );
}

export function Skeleton({ className }: { className?: string }) {
  return (
    <div className={cn("animate-pulse rounded-lg bg-slate-200", className)} />
  );
}
