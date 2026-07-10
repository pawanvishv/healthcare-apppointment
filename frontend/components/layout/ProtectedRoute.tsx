"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/auth-store";
import { Spinner } from "@/components/ui/Spinner";

export function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated());
  const hasHydrated = useAuthStore((s) => s._hasHydrated);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const unsub = useAuthStore.persist.onFinishHydration(() => setReady(true));
    if (useAuthStore.persist.hasHydrated()) {
      setReady(true);
    }
    return unsub;
  }, []);

  useEffect(() => {
    if (ready && hasHydrated && !isAuthenticated) {
      router.replace("/login");
    }
  }, [ready, hasHydrated, isAuthenticated, router]);

  if (!ready || !hasHydrated || !isAuthenticated) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Spinner />
      </div>
    );
  }

  return <>{children}</>;
}
