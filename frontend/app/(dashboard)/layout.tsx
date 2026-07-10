"use client";

import { ProtectedRoute } from "@/components/layout/ProtectedRoute";
import { Sidebar } from "@/components/layout/Sidebar";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-slate-50">
        <Sidebar />
        <main className="md:pl-64">
          <div className="mx-auto max-w-5xl px-4 py-8 pt-16 md:pt-8">
            {children}
          </div>
        </main>
      </div>
    </ProtectedRoute>
  );
}
