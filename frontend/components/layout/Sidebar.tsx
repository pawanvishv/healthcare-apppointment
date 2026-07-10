"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuthStore } from "@/store/auth-store";
import { logout } from "@/features/auth/api";
import { useToast } from "@/components/ui/Toast";
import { cn } from "@/lib/utils/cn";
import {
  Calendar,
  CalendarPlus,
  Clock,
  LogOut,
  Menu,
  Stethoscope,
  X,
} from "lucide-react";
import { useState } from "react";
import type { Role } from "@/types/api";

const patientNav = [
  { href: "/appointments", label: "My Appointments", icon: Calendar },
  { href: "/slots", label: "Browse Slots", icon: Clock },
  { href: "/appointments/new", label: "Book Appointment", icon: CalendarPlus },
];

const doctorNav = [
  { href: "/appointments", label: "Patient Appointments", icon: Calendar },
  { href: "/slots", label: "My Slots", icon: Clock },
];

function getNavItems(role: Role | null) {
  return role === "DOCTOR" ? doctorNav : patientNav;
}

export function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const { email, role, refreshToken, clearSession } = useAuthStore();
  const { showToast } = useToast();
  const [mobileOpen, setMobileOpen] = useState(false);
  const navItems = getNavItems(role);

  const handleLogout = async () => {
    try {
      if (refreshToken) await logout(refreshToken);
    } catch {
      // ignore logout errors
    }
    clearSession();
    showToast("Logged out", "info");
    router.push("/login");
  };

  const NavContent = () => (
    <>
      <div className="flex items-center gap-2 px-4 py-6">
        <Stethoscope className="h-8 w-8 text-primary-600" />
        <div>
          <p className="font-bold text-slate-900">HealthApp</p>
          <p className="text-xs text-slate-500">{role ?? "User"}</p>
        </div>
      </div>

      <nav className="flex-1 space-y-1 px-3">
        {navItems.map(({ href, label, icon: Icon }) => (
          <Link
            key={href}
            href={href}
            onClick={() => setMobileOpen(false)}
            className={cn(
              "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
              pathname === href || (href !== "/slots" && pathname.startsWith(href + "/"))
                ? "bg-primary-50 text-primary-700"
                : "text-slate-600 hover:bg-slate-100"
            )}
          >
            <Icon className="h-5 w-5" />
            {label}
          </Link>
        ))}
      </nav>

      <div className="border-t border-slate-200 p-4">
        <p className="mb-2 truncate text-xs text-slate-500">{email}</p>
        <button
          onClick={handleLogout}
          className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-slate-600 hover:bg-slate-100"
        >
          <LogOut className="h-4 w-4" />
          Logout
        </button>
      </div>
    </>
  );

  return (
    <>
      <button
        className="fixed left-4 top-4 z-40 rounded-lg bg-white p-2 shadow-md md:hidden"
        onClick={() => setMobileOpen(!mobileOpen)}
      >
        {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
      </button>

      {mobileOpen && (
        <div
          className="fixed inset-0 z-30 bg-black/40 md:hidden"
          onClick={() => setMobileOpen(false)}
        />
      )}

      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-30 flex w-64 flex-col bg-white border-r border-slate-200",
          "transition-transform md:translate-x-0",
          mobileOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0"
        )}
      >
        <NavContent />
      </aside>
    </>
  );
}
