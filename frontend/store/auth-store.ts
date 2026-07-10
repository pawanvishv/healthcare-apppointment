import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { Role } from "@/types/api";

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  userId: string | null;
  email: string | null;
  role: Role | null;
  _hasHydrated: boolean;
  setHasHydrated: (state: boolean) => void;
  setSession: (data: {
    accessToken: string;
    refreshToken: string;
    userId?: string;
    email?: string;
    role?: Role;
  }) => void;
  setAccessToken: (token: string) => void;
  clearSession: () => void;
  isAuthenticated: () => boolean;
}

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const base64 = token.split(".")[1];
    const json = atob(base64.replace(/-/g, "+").replace(/_/g, "/"));
    return JSON.parse(json);
  } catch {
    return null;
  }
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      userId: null,
      email: null,
      role: null,
      _hasHydrated: false,

      setHasHydrated: (state) => set({ _hasHydrated: state }),

      setSession: ({ accessToken, refreshToken, userId, email, role }) => {
        const payload = decodeJwtPayload(accessToken);
        const roles = (payload?.roles as string[]) ?? [];
        const resolvedRole = (role ?? roles[0]?.replace("ROLE_", "")) as Role | undefined;

        set({
          accessToken,
          refreshToken,
          userId: userId ?? (payload?.sub as string) ?? null,
          email: email ?? (payload?.email as string) ?? null,
          role: resolvedRole ?? null,
        });
      },

      setAccessToken: (token) => {
        const payload = decodeJwtPayload(token);
        const roles = (payload?.roles as string[]) ?? [];
        set({
          accessToken: token,
          userId: (payload?.sub as string) ?? null,
          email: (payload?.email as string) ?? null,
          role: (roles[0]?.replace("ROLE_", "") as Role) ?? null,
        });
      },

      clearSession: () =>
        set({
          accessToken: null,
          refreshToken: null,
          userId: null,
          email: null,
          role: null,
        }),

      isAuthenticated: () => !!get().accessToken,
    }),
    {
      name: "healthcare-auth",
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        userId: state.userId,
        email: state.email,
        role: state.role,
      }),
      onRehydrateStorage: () => (state) => {
        state?.setHasHydrated(true);
      },
    }
  )
);
