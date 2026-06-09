import { create } from "zustand";
import { persist } from "zustand/middleware";
import { clearStoredAccessToken, setStoredAccessToken } from "@/lib/auth-storage";

export type AuthRole = "ADMIN" | "USER_FREE_TRIAL" | "USER_BASIC" | "USER_PREMIUM" | string;

type AuthState = {
  accessToken: string | null;
  role: AuthRole | null;
  username: string | null;
  isAuthenticated: boolean;
  setSession: (session: { accessToken: string; role: AuthRole; username: string }) => void;
  logout: () => void;
};

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      role: null,
      username: null,
      isAuthenticated: false,
      setSession: ({ accessToken, role, username }) => {
        setStoredAccessToken(accessToken);
        set({ accessToken, role, username, isAuthenticated: true });
      },
      logout: () => {
        clearStoredAccessToken();
        set({ accessToken: null, role: null, username: null, isAuthenticated: false });
      },
    }),
    {
      name: "sllp.auth",
      partialize: (state) => ({
        accessToken: state.accessToken,
        role: state.role,
        username: state.username,
        isAuthenticated: state.isAuthenticated,
      }),
      onRehydrateStorage: () => (state) => {
        if (state?.accessToken) {
          setStoredAccessToken(state.accessToken);
        }
      },
    },
  ),
);
