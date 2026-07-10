"use client";

import { useEffect } from "react";
import { Button } from "@/components/ui/Button";

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 p-4">
      <h2 className="text-xl font-semibold text-slate-900">Something went wrong</h2>
      <p className="text-sm text-slate-500">An unexpected error occurred.</p>
      <Button onClick={reset}>Try again</Button>
    </div>
  );
}
